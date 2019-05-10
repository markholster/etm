package com.jecstar.etm.processor.kafka.handler;

import com.jecstar.etm.processor.handler.AbstractJsonHandler;

public abstract class AbstractKafkaHandler extends AbstractJsonHandler {

    protected AbstractKafkaHandler(String defaultImportProfile) {
        super(defaultImportProfile);
    }
}
