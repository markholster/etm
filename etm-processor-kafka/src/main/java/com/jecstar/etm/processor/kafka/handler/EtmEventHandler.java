package com.jecstar.etm.processor.kafka.handler;

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.HandlerResults;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class EtmEventHandler extends AbstractKafkaHandler {

    private final TelemetryCommandProcessor telemetryCommandProcessor;

    public EtmEventHandler(TelemetryCommandProcessor telemetryCommandProcessor, String defaultImportProfile) {
        super(defaultImportProfile);
        this.telemetryCommandProcessor = telemetryCommandProcessor;
    }

    @Override
    protected TelemetryCommandProcessor getProcessor() {
        return this.telemetryCommandProcessor;
    }

    public HandlerResults handleMessage(ConsumerRecord<String, String> record) {
        return handleData(record.value());
    }
}
