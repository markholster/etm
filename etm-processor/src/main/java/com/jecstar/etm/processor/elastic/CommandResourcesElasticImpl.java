package com.jecstar.etm.processor.elastic;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandlingTimeComparator;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.processor.core.CommandResources;
import com.jecstar.etm.server.core.domain.EndpointConfiguration;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangeListener;
import com.jecstar.etm.server.core.domain.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.EndpointConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.server.core.persisting.elastic.*;
import com.jecstar.etm.server.core.util.LruCache;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class CommandResourcesElasticImpl implements CommandResources, ConfigurationChangeListener {

    private static final long ENDPOINT_CACHE_VALIDITY = 60_000;

    private final DataRepository dataRepository;
    private final EtmConfiguration etmConfiguration;
    private final BulkProcessorListener bulkProcessorListener;

    private final EndpointHandlingTimeComparator endpointComparater = new EndpointHandlingTimeComparator();
    private final EndpointConfigurationConverterJsonImpl endpointConfigurationConverter = new EndpointConfigurationConverterJsonImpl();


    @SuppressWarnings("rawtypes")
    private final Map<CommandType, TelemetryEventPersister> persisters = new HashMap<>();

    private BulkProcessor bulkProcessor;

    private final LruCache<String, EndpointConfiguration> endpointCache;

    CommandResourcesElasticImpl(final DataRepository dataRepository, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
        this.dataRepository = dataRepository;
        this.etmConfiguration = etmConfiguration;
        this.endpointCache = new LruCache<>(etmConfiguration.getEndpointConfigurationCacheSize(), ENDPOINT_CACHE_VALIDITY);
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

    @Override
    public void loadEndpointConfig(List<Endpoint> endpoints, EndpointConfiguration endpointConfiguration) {
        endpointConfiguration.initialize();
        endpoints.sort(this.endpointComparater);
        for (Endpoint endpoint : endpoints) {
            if (endpoint.name != null) {
                mergeEndpointConfigs(endpointConfiguration, retrieveEndpoint(endpoint.name));
            }
        }
        mergeEndpointConfigs(endpointConfiguration, retrieveEndpoint(ElasticsearchLayout.CONFIGURATION_OBJECT_ID_ENDPOINT_DEFAULT));
    }

    private EndpointConfiguration retrieveEndpoint(String endpointName) {
        EndpointConfiguration cachedItem = endpointCache.get(endpointName);
        if (cachedItem != null) {
            return cachedItem;
        }
        GetResponse getResponse = this.dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_OBJECT_ID_ENDPOINT_DEFAULT.equals(endpointName) ? ElasticsearchLayout.CONFIGURATION_OBJECT_ID_ENDPOINT_DEFAULT : ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_ENDPOINT_ID_PREFIX + endpointName)
                .setFetchSource(true));
        if (getResponse.isExists() && !getResponse.isSourceEmpty()) {
            EndpointConfiguration loadedConfig = this.endpointConfigurationConverter.read(getResponse.getSourceAsMap());
            this.endpointCache.put(endpointName, loadedConfig);
            return loadedConfig;
        } else {
            NonExsistentEndpointConfiguration endpointConfig = new NonExsistentEndpointConfiguration();
            this.endpointCache.put(endpointName, endpointConfig);
            return endpointConfig;
        }
    }

    private void mergeEndpointConfigs(EndpointConfiguration endpointConfiguration, EndpointConfiguration endpointToMerge) {
        if (endpointToMerge instanceof NonExsistentEndpointConfiguration) {
            return;
        }
        if (endpointConfiguration.eventEnhancer == null) {
            endpointConfiguration.eventEnhancer = endpointToMerge.eventEnhancer;
            return;
        }
        if (endpointConfiguration.eventEnhancer instanceof DefaultTelemetryEventEnhancer &&
                endpointToMerge.eventEnhancer instanceof DefaultTelemetryEventEnhancer) {
            ((DefaultTelemetryEventEnhancer) endpointConfiguration.eventEnhancer).mergeExpressionParsers((DefaultTelemetryEventEnhancer) endpointToMerge.eventEnhancer);
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
        } else if (event.isChanged(EtmConfiguration.CONFIG_KEY_ENDPOINT_CONFIGURATION_CACHE_SIZED)) {
            this.endpointCache.setMaxSize(this.etmConfiguration.getEndpointConfigurationCacheSize());
        }
    }

    /**
     * Class to store in de endpoint cache to make sure the
     *
     * @author Mark Holster
     */
    private class NonExsistentEndpointConfiguration extends EndpointConfiguration {
    }


}
