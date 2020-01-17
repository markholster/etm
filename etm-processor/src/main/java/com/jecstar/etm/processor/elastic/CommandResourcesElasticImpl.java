package com.jecstar.etm.processor.elastic;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.processor.core.CommandResources;
import com.jecstar.etm.server.core.domain.ImportProfile;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangeListener;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.ImportProfileConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.persisting.elastic.*;
import com.jecstar.etm.server.core.util.LruCache;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class CommandResourcesElasticImpl implements CommandResources, ConfigurationChangeListener {

    private static final long ENDPOINT_CACHE_VALIDITY = 60_000;

    private final DataRepository dataRepository;
    private final EtmConfiguration etmConfiguration;
    private final BulkProcessorListener bulkProcessorListener;

    private final ImportProfileConverterJsonImpl importProfileConverter = new ImportProfileConverterJsonImpl();

    @SuppressWarnings("rawtypes")
    private final Map<CommandType, TelemetryEventPersister> persisters = new HashMap<>();

    private BulkProcessor bulkProcessor;

    private final LruCache<String, ImportProfile> importProfileCache;

    CommandResourcesElasticImpl(final DataRepository dataRepository, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
        this.dataRepository = dataRepository;
        this.etmConfiguration = etmConfiguration;
        this.importProfileCache = new LruCache<>(etmConfiguration.getImportProfileCacheSize(), ENDPOINT_CACHE_VALIDITY);
        this.bulkProcessorListener = new BulkProcessorListener(metricRegistry);
        this.bulkProcessor = createBulkProcessor();
        this.etmConfiguration.addConfigurationChangeListener(this);

        this.persisters.put(CommandType.BUSINESS_EVENT, new BusinessTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
        this.persisters.put(CommandType.HTTP_EVENT, new HttpTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
        this.persisters.put(CommandType.LOG_EVENT, new LogTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
        this.persisters.put(CommandType.MESSAGING_EVENT, new MessagingTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
        this.persisters.put(CommandType.SQL_EVENT, new SqlTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getPersister(CommandType commandType) {
        return (T) persisters.get(commandType);
    }


    public ImportProfile loadImportProfile(String importProfileName) {
        if (importProfileName == null) {
            importProfileName = ElasticsearchLayout.CONFIGURATION_OBJECT_ID_IMPORT_PROFILE_DEFAULT;
        }
        var cachedItem = importProfileCache.get(importProfileName);
        if (cachedItem != null) {
            return cachedItem;
        }
        var getResponse = this.dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_OBJECT_ID_IMPORT_PROFILE_DEFAULT.equals(importProfileName) ? ElasticsearchLayout.CONFIGURATION_OBJECT_ID_IMPORT_PROFILE_DEFAULT : ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE_ID_PREFIX + importProfileName)
                .setFetchSource(true));
        if (!getResponse.isExists()) {
            this.dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                    ElasticsearchLayout.CONFIGURATION_OBJECT_ID_IMPORT_PROFILE_DEFAULT)
                    .setFetchSource(true));
        }
        if (getResponse.isExists() && !getResponse.isSourceEmpty()) {
            var loadedProfile = this.importProfileConverter.read(getResponse.getSourceAsMap());
            this.importProfileCache.put(importProfileName, loadedProfile);
            return loadedProfile;
        } else {
            var importProfile = new NonExistentImportProfile();
            this.importProfileCache.put(importProfileName, importProfile);
            return importProfile;
        }
    }

    @Override
    public void close() {
        this.etmConfiguration.removeConfigurationChangeListener(this);
        this.bulkProcessor.close();
    }

    private BulkProcessor createBulkProcessor() {
        BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
                (request, bulkListener) ->
                        this.dataRepository.getClient().bulkAsync(request, RequestOptions.DEFAULT, bulkListener);
        return BulkProcessor.builder(bulkConsumer, this.bulkProcessorListener)
                .setBulkActions(this.etmConfiguration.getPersistingBulkCount() <= 0 ? -1 : this.etmConfiguration.getPersistingBulkCount())
                .setBulkSize(new ByteSizeValue(this.etmConfiguration.getPersistingBulkSize() <= 0 ? -1 : this.etmConfiguration.getPersistingBulkSize(), ByteSizeUnit.BYTES))
                .setFlushInterval(this.etmConfiguration.getPersistingBulkTime() <= 0 ? null : TimeValue.timeValueMillis(this.etmConfiguration.getPersistingBulkTime()))
                .setConcurrentRequests(this.etmConfiguration.getPersistingBulkThreads())
                .build();
    }

    @Override
    public void configurationChanged(ConfigurationChangedEvent event) {
        if (event.isAnyChanged(EtmConfiguration.CONFIG_KEY_PERSISTING_BULK_COUNT,
                EtmConfiguration.CONFIG_KEY_PERSISTING_BULK_SIZE,
                EtmConfiguration.CONFIG_KEY_PERSISTING_BULK_TIME,
                EtmConfiguration.CONFIG_KEY_PERSISTING_BULK_THREADS)) {
            BulkProcessor oldBulkProcessor = this.bulkProcessor;
            this.bulkProcessor = createBulkProcessor();
            this.persisters.values().forEach(c -> ((AbstractElasticTelemetryEventPersister) c).setBulkProcessor(this.bulkProcessor));
            oldBulkProcessor.close();
        } else if (event.isChanged(EtmConfiguration.CONFIG_KEY_IMPORT_PROFILE_CACHE_SIZED)) {
            this.importProfileCache.setMaxSize(this.etmConfiguration.getImportProfileCacheSize());
        }
    }

    /**
     * Class to store in de endpoint cache to make sure the
     *
     * @author Mark Holster
     */
    private static class NonExistentImportProfile extends ImportProfile {
    }


}
