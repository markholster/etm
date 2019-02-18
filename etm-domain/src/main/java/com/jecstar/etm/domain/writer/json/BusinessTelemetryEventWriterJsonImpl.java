package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.writer.BusinessTelemetryEventWriter;
import com.jecstar.etm.domain.writer.TelemetryEventTags;

public class BusinessTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<BusinessTelemetryEvent> implements BusinessTelemetryEventWriter<String> {

    @Override
    String getType() {
        return TelemetryEventTags.EVENT_OBJECT_TYPE_BUSINESS;
    }

    @Override
    protected boolean doWrite(BusinessTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
        return firstElement;
    }

}
