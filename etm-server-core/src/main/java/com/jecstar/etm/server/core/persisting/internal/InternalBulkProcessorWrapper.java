package com.jecstar.etm.server.core.persisting.internal;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.BusinessTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.LogTelemetryEventConverterJsonImpl;
import com.jecstar.etm.server.core.persisting.elastic.BusinessTelemetryEventPersister;
import com.jecstar.etm.server.core.persisting.elastic.LogTelemetryEventPersister;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Listener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Bulk processor for ETM internal logging. Used for persisting events without
 * placing them on the ringbuffer. This is in particular useful for logging- and
 * business events.
 *
 * @author Mark Holster
 */
public class InternalBulkProcessorWrapper implements AutoCloseable {

    private static final LogTelemetryEventConverterJsonImpl logWriter = new LogTelemetryEventConverterJsonImpl();
    private static final BusinessTelemetryEventConverterJsonImpl businessWriter = new BusinessTelemetryEventConverterJsonImpl();

    private EtmConfiguration configuration;
    private BulkProcessor bulkProcessor;

    private LogTelemetryEventPersister logPersister;
    private BusinessTelemetryEventPersister businessPersister;

    private final List<TelemetryEvent<?>> startupBuffer = new ArrayList<>();

    private boolean open = true;

    public void setConfiguration(EtmConfiguration configuration) {
        this.configuration = configuration;
        if (this.bulkProcessor != null) {
            if (this.logPersister == null) {
                this.logPersister = new LogTelemetryEventPersister(this.bulkProcessor, this.configuration) {
                    @Override
                    public void persist(LogTelemetryEvent event, LogTelemetryEventConverterJsonImpl converter) {
                        var indexRequest = createIndexRequest(event.id).setSource(converter.write(event, false, false), XContentType.JSON);
                        bulkProcessor.add(indexRequest.build());
                        setCorrelationOnParent(event);
                    }
                };
            }
            if (this.businessPersister == null) {
                this.businessPersister = new BusinessTelemetryEventPersister(this.bulkProcessor, this.configuration) {
                    @Override
                    public void persist(BusinessTelemetryEvent event, BusinessTelemetryEventConverterJsonImpl converter) {
                        var indexRequestBuilder = createIndexRequest(event.id).setSource(converter.write(event, false, false), XContentType.JSON);
                        bulkProcessor.add(indexRequestBuilder.build());
                        setCorrelationOnParent(event);
                    }
                };
            }
            flushStartupBuffer();
        }
    }

    public void setClient(RestHighLevelClient client) {
        BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
                (request, bulkListener) ->
                        client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener);


        this.bulkProcessor = BulkProcessor.builder(bulkConsumer, new Listener() {
                    @Override
                    public void beforeBulk(long executionId, BulkRequest request) {
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                    }
                }
        )
                .setBulkActions(100)
                .setBulkSize(new ByteSizeValue(1, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(10))
                .build();
        if (this.configuration != null) {
            if (this.logPersister == null) {
                this.logPersister = new LogTelemetryEventPersister(this.bulkProcessor, this.configuration);
            }
            if (this.businessPersister == null) {
                this.businessPersister = new BusinessTelemetryEventPersister(this.bulkProcessor, this.configuration);
            }
            flushStartupBuffer();
        }
    }

    private synchronized void flushStartupBuffer() {
        for (TelemetryEvent<?> event : this.startupBuffer) {
            if (event instanceof LogTelemetryEvent) {
                persist((LogTelemetryEvent) event);
            }
        }
        this.startupBuffer.clear();
    }

    public void persist(LogTelemetryEvent event) {
        if (this.logPersister != null && isOpen()) {
            this.logPersister.persist(event, logWriter);
        } else {
            this.startupBuffer.add(event);
        }
    }

    public void persist(BusinessTelemetryEvent event) {
        if (this.businessPersister != null && isOpen()) {
            this.businessPersister.persist(event, businessWriter);
        } else {
            this.startupBuffer.add(event);
        }
    }

    private boolean isOpen() {
        return this.open;
    }

    @Override
    public void close() {
        this.open = false;
        if (this.bulkProcessor != null) {
            this.bulkProcessor.close();
        }
    }


}
