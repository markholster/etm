package com.jecstar.etm.server.core.enhancers;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.builder.HttpTelemetryEventBuilder;
import com.jecstar.etm.server.core.domain.parser.CopyValueExpressionParser;
import org.junit.Test;

import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DefaultTelemetryEventEnhancerTest {


    @Test
    public void testParserCopiesMetadataToId() {
        final String id = "12345";
        DefaultTelemetryEventEnhancer enhancer = new DefaultTelemetryEventEnhancer();
        DefaultField field = new DefaultField("id");
        field.setParsersSource("metadata.http_X-Request-ID");
        field.getParsers().add(new CopyValueExpressionParser("Copy the value"));
        field.setWritePolicy(DefaultField.WritePolicy.WHEN_EMPTY);
        enhancer.addField(field);

        HttpTelemetryEvent event = new HttpTelemetryEventBuilder().setHttpEventType(HttpTelemetryEvent.HttpEventType.GET).addMetadata("http_X-Request-ID", id).build();
        assertNull(event.id);
        enhancer.enhance(event, ZonedDateTime.now());
        assertEquals(id, event.id);
    }

}
