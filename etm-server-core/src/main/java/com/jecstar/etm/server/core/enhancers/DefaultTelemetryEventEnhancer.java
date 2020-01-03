package com.jecstar.etm.server.core.enhancers;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.server.core.domain.parser.ExpressionParser;
import com.jecstar.etm.server.core.domain.parser.ExpressionParserField;
import com.jecstar.etm.server.core.enhancers.DefaultField.WritePolicy;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultTelemetryEventEnhancer implements TelemetryEventEnhancer {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(DefaultTelemetryEventEnhancer.class);

    private boolean enhancePayloadFormat = true;
    private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();

    private final List<DefaultTransformation> transformations = new ArrayList<>();
    private final List<DefaultField> fields = new ArrayList<>();

    @Override
    public void enhance(TelemetryEvent<?> event, ZonedDateTime enhanceTime) {
        transform(event);
        enchancePayloadFormat(event);
        enhanceFields(event);
    }

    public void setEnhancePayloadFormat(boolean enhancePayloadFormat) {
        this.enhancePayloadFormat = enhancePayloadFormat;
    }

    public boolean isEnhancePayloadFormat() {
        return this.enhancePayloadFormat;
    }

    public List<DefaultField> getFields() {
        return this.fields;
    }

    public List<DefaultTransformation> getTransformations() {
        return this.transformations;
    }

    public void addField(DefaultField field) {
        Optional<DefaultField> optional = this.fields.stream().filter(f -> f.getName().equals(field.getName())).findFirst();
        if (optional.isPresent()) {
            DefaultField defaultField = optional.get();
            defaultField.addParsers(field.getParsers());
        } else {
            this.fields.add(field);
        }
    }

    public void addTransformation(DefaultTransformation transformation) {
        boolean expressionParserPresent = this.transformations.stream().anyMatch(p -> p.getExpressionParser().getName().equals(transformation.getExpressionParser().getName()));
        if (!expressionParserPresent) {
            this.transformations.add(transformation);
        }
    }

    private void enchancePayloadFormat(final TelemetryEvent<?> event) {
        if (event.payloadFormat == null && this.enhancePayloadFormat) {
            event.payloadFormat = detectPayloadFormat(event.payload);
        }
    }

    /**
     * Super simple payload format detector. This isn't an enhanced detection
     * algorithm because we won't be losing to much performance here. The end
     * user should be able to tell the system which format it is anyway.
     *
     * @param payload The payload.
     * @return The detected <code>PayloadFormat</code>.
     */
    private PayloadFormat detectPayloadFormat(String payload) {
        if (payload == null) {
            return null;
        }
        String trimmed = payload.toLowerCase().trim();
        if (trimmed.contains("http://schemas.xmlsoap.org/soap/envelope/") || trimmed.contains("http://www.w3.org/2003/05/soap-envelope")) {
            return PayloadFormat.SOAP;
        } else if (trimmed.contains("<!doctype html")) {
            return PayloadFormat.HTML;
        } else if (trimmed.startsWith("<?xml ")) {
            return PayloadFormat.XML;
        } else if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            return PayloadFormat.XML;
        } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return PayloadFormat.JSON;
        } else if (trimmed.startsWith("select")
                || trimmed.startsWith("insert")
                || trimmed.startsWith("update")
                || trimmed.startsWith("delete")
                || trimmed.startsWith("drop")
                || trimmed.startsWith("create")) {
            return PayloadFormat.SQL;
        }
        return null;
    }

    private void transform(TelemetryEvent<?> event) {
        if (this.transformations.size() == 0) {
            return;
        }
        for (DefaultTransformation transformation : this.transformations) {
            if (!transformation.getExpressionParser().isCapableOfReplacing()) {
                continue;
            }
            event.payload = transformation.getExpressionParser().replace(event.payload, transformation.getReplacement(), transformation.isReplaceAll());
        }
    }

    private void enhanceFields(TelemetryEvent<?> event) {
        if (this.fields.size() == 0) {
            return;
        }
        for (DefaultField field : this.fields) {
            conditionallySetValue(event, field);
        }

    }

    private void conditionallySetValue(TelemetryEvent<?> event, DefaultField field) {
        if (field == null) {
            return;
        }
        if (ExpressionParserField.ID.getJsonTag().equals(field.getName())) {
            if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE)
                    || (field.getWritePolicy().equals(WritePolicy.WHEN_EMPTY) && event.id == null)) {
                event.id = parseValue(field, event);
            } else if (field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND)) {
                String value = parseValue(field, event);
                if (value != null) {
                    event.id = value;
                }
            }
        } else if (ExpressionParserField.CORRELATION_ID.getJsonTag().equals(field.getName())) {
            if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE)
                    || (field.getWritePolicy().equals(WritePolicy.WHEN_EMPTY) && event.correlationId == null)) {
                event.correlationId = parseValue(field, event);
            } else if (field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND)) {
                String value = parseValue(field, event);
                if (value != null) {
                    event.correlationId = value;
                }
            }
        } else if (ExpressionParserField.NAME.getJsonTag().equals(field.getName())) {
            if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE)
                    || (field.getWritePolicy().equals(WritePolicy.WHEN_EMPTY) && event.name == null)) {
                event.name = parseValue(field, event);
            } else if (field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND)) {
                String value = parseValue(field, event);
                if (value != null) {
                    event.name = value;
                }
            }
        } else if (field.getName().startsWith(ExpressionParserField.CORRELATION_DATA.getJsonTag())) {
            conditionallyPutInMap(event, field, ExpressionParserField.CORRELATION_DATA, event.correlationData);
        } else if (field.getName().startsWith(ExpressionParserField.EXTRACTED_DATA.getJsonTag())) {
            conditionallyPutInMap(event, field, ExpressionParserField.EXTRACTED_DATA, event.extractedData);
        } else if (field.getName().startsWith(ExpressionParserField.METADATA.getJsonTag())) {
            conditionallyPutInMap(event, field, ExpressionParserField.METADATA, event.metadata);
        } else if (ExpressionParserField.WRITER_TRANSACTION_ID.getJsonTag().equals(field.getName())) {
            setWritingTransactionIdOnEndpoints(field, event);
        } else if (ExpressionParserField.READER_TRANSACTION_ID.getJsonTag().equals(field.getName())) {
            setReadingTransactionIdOnEndpoints(field, event);
        } else if (field.getName().startsWith(ExpressionParserField.WRITER_METADATA.getJsonTag())) {
            for (Endpoint endpoint : event.endpoints) {
                EndpointHandler writingEndpointHandler = endpoint.getWritingEndpointHandler();
                if (writingEndpointHandler != null) {
                    conditionallyPutInMap(event, field, ExpressionParserField.WRITER_METADATA, writingEndpointHandler.metadata);
                }
            }
        } else if (field.getName().startsWith(ExpressionParserField.READER_METADATA.getJsonTag())) {
            for (Endpoint endpoint : event.endpoints) {
                for (EndpointHandler handler : endpoint.getReadingEndpointHandlers()) {
                    conditionallyPutInMap(event, field, ExpressionParserField.WRITER_METADATA, handler.metadata);
                }
            }
        }
    }

    private void setWritingTransactionIdOnEndpoints(DefaultField field, TelemetryEvent<?> event) {
        if (event.endpoints == null || event.endpoints.size() < 1) {
            return;
        }
        // Do everything to prevent the slow extraction of the field value from the data. Speed is everything...
        if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE)) {
            String transactionId = parseValue(field, event);
            if (transactionId != null) {
                for (Endpoint endpoint : event.endpoints) {
                    EndpointHandler writingEndpointHandler = endpoint.getWritingEndpointHandler();
                    if (writingEndpointHandler != null) {
                        writingEndpointHandler.transactionId = transactionId;
                    }
                }
            }
        } else {
            List<EndpointHandler> emptyEndpointHandlers = event.endpoints.stream()
                    .filter(p -> p.getWritingEndpointHandler() != null)
                    .map(f -> f.getWritingEndpointHandler())
                    .filter(p -> p.transactionId == null).collect(Collectors.toList());
            if (emptyEndpointHandlers != null && emptyEndpointHandlers.size() > 0) {
                String transactionId = parseValue(field, event);
                for (EndpointHandler endpointHandler : emptyEndpointHandlers) {
                    if ((field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND) && transactionId != null) || endpointHandler.transactionId == null) {
                        endpointHandler.transactionId = transactionId;
                    }
                }
            }
        }
    }

    private void setReadingTransactionIdOnEndpoints(DefaultField field, TelemetryEvent<?> event) {
        if (event.endpoints == null || event.endpoints.size() < 1) {
            return;
        }
        // Do everything to prevent the slow extraction of the field value from the data. Speed is everything...
        if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE)) {
            String transactionId = parseValue(field, event);
            if (transactionId != null) {
                for (Endpoint endpoint : event.endpoints) {
                    for (EndpointHandler handler : endpoint.getReadingEndpointHandlers()) {
                        handler.transactionId = transactionId;
                    }
                }
            }
        } else {
            List<EndpointHandler> emptyEndpointHanlders = event.endpoints.stream()
                    .flatMap(f -> f.getReadingEndpointHandlers().stream())
                    .filter(p -> p.transactionId == null)
                    .collect(Collectors.toList());
            if (emptyEndpointHanlders != null && emptyEndpointHanlders.size() > 0) {
                String transactionId = parseValue(field, event);
                for (EndpointHandler endpointHandler : emptyEndpointHanlders) {
                    if ((field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND) && transactionId != null) || endpointHandler.transactionId == null) {
                        endpointHandler.transactionId = transactionId;
                    }
                }
            }
        }
    }

    private void conditionallyPutInMap(TelemetryEvent<?> event, DefaultField field, ExpressionParserField parserField, Map<String, Object> container) {
        String dataKey = parserField.getCollectionKeyName(field.getName());
        if (field.getWritePolicy().equals(WritePolicy.ALWAYS_OVERWRITE)
                || (field.getWritePolicy().equals(WritePolicy.WHEN_EMPTY) && !container.containsKey(dataKey))) {
            String value = parseValue(field, event);
            if (value == null) {
                container.remove(dataKey);
            } else {
                container.put(dataKey, parseValue(field, event));
            }
        } else if (field.getWritePolicy().equals(WritePolicy.OVERWRITE_WHEN_FOUND)) {
            String value = parseValue(field, event);
            if (value != null) {
                container.put(dataKey, value);
            }
        }
    }

    private String parseValue(DefaultField field, TelemetryEvent<?> event) {
        if (field.getParsers().isEmpty()) {
            return null;
        }
        String valueSource = extractValueFromEvent(field.getParsersSource(), event);
        if (valueSource == null) {
            return null;
        }
        for (ExpressionParser expressionParser : field.getParsers()) {
            String value = parseValue(expressionParser, valueSource);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String extractValueFromEvent(String parsersSource, TelemetryEvent<?> event) {
        if (this.tags.getPayloadTag().equals(parsersSource)) {
            // Fast method for payload extraction.
            return event.payload;
        } else if (parsersSource.startsWith(this.tags.getMetadataTag())) {
            Object value = event.metadata.get(parsersSource.substring(this.tags.getMetadataTag().length() + 1));
            if (value == null) {
                return null;
            }
            return value.toString();
        }
        return event.payload;
    }

    private String parseValue(ExpressionParser expressionParser, String content) {
        if (expressionParser == null || content == null) {
            return null;
        }
        String value = expressionParser.evaluate(content);
        if (value != null && value.trim().length() > 0) {
            return value;
        }
        return null;
    }

    @Override
    public void close() {
        for (var transformation : this.transformations) {
            try {
                transformation.getExpressionParser().close();
            } catch (Exception e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage(e.getMessage(), e);
                }
            }
        }
        for (var field : this.fields) {
            for (var parser : field.getParsers()) {
                try {
                    parser.close();
                } catch (Exception e) {
                    if (log.isDebugLevelEnabled()) {
                        log.logDebugMessage(e.getMessage(), e);
                    }
                }
            }
        }
    }
}
