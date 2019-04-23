package com.jecstar.etm.gui.rest.export;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.search.TransactionEvent;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.audit.builder.GetEventAuditLogBuilder;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.search.SearchHit;

import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class QueryExporter {

    private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();

    public File exportToFile(ScrollableSearch scrollableSearch, FileType fileType, int maxRows, EtmPrincipal etmPrincipal, List<FieldLayout> fields, Consumer<GetEventAuditLogBuilder> auditLogPersistingConsumer) {
        GetEventAuditLogBuilder auditLogBuilder = createGetEventAuditLogBuilder(etmPrincipal);

        removePayloadWhenUserIsNotAllowedToSee(fields, etmPrincipal);

        try {
            File outputFile = File.createTempFile("etm-", "-download");
            outputFile.deleteOnExit();
            if (FileType.CSV.equals(fileType)) {
                createCsv(() -> new ScrollableSearchSupplier(scrollableSearch), maxRows, outputFile, etmPrincipal, fields, auditLogBuilder, auditLogPersistingConsumer);
            } else if (FileType.XLSX.equals(fileType)) {
                createXlsx(() -> new ScrollableSearchSupplier(scrollableSearch), maxRows, outputFile, etmPrincipal, fields, auditLogBuilder, auditLogPersistingConsumer);
            }
            return outputFile;
        } catch (IOException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public File exportToFile(List<TransactionEvent> events, FileType fileType, EtmPrincipal etmPrincipal, Consumer<GetEventAuditLogBuilder> auditLogPersistingConsumer) {
        GetEventAuditLogBuilder auditLogBuilder = createGetEventAuditLogBuilder(etmPrincipal);

        List<FieldLayout> fields = new ArrayList<>();
        fields.add(new FieldLayout("Id", Keyword.ID.getName(), FieldType.PLAIN, MultiSelect.FIRST));
        fields.add(new FieldLayout("Name", eventTags.getNameTag(), FieldType.PLAIN, MultiSelect.FIRST));
        fields.add(new FieldLayout("Date", eventTags.getEndpointHandlerHandlingTimeTag(), FieldType.ISO_TIMESTAMP, MultiSelect.FIRST));
        fields.add(new FieldLayout("Direction", "direction", FieldType.PLAIN, MultiSelect.FIRST));
        fields.add(new FieldLayout("Subtype", "subtype", FieldType.PLAIN, MultiSelect.FIRST));
        fields.add(new FieldLayout("Endpoint", "endpoint", FieldType.PLAIN, MultiSelect.FIRST));
        fields.add(new FieldLayout("Payload", eventTags.getPayloadTag(), FieldType.PLAIN, MultiSelect.FIRST));

        removePayloadWhenUserIsNotAllowedToSee(fields, etmPrincipal);

        try {
            File outputFile = File.createTempFile("etm-", "-download");
            outputFile.deleteOnExit();
            if (FileType.CSV.equals(fileType)) {
                createCsv(() -> new TransactionEventSupplier(events), Integer.MAX_VALUE, outputFile, etmPrincipal, fields, auditLogBuilder, auditLogPersistingConsumer);
            } else if (FileType.XLSX.equals(fileType)) {
                createXlsx(() -> new TransactionEventSupplier(events), Integer.MAX_VALUE, outputFile, etmPrincipal, fields, auditLogBuilder, auditLogPersistingConsumer);
            }
            return outputFile;
        } catch (IOException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    private GetEventAuditLogBuilder createGetEventAuditLogBuilder(EtmPrincipal etmPrincipal) {
        Instant now = Instant.now();
        return new GetEventAuditLogBuilder()
                .setTimestamp(now)
                .setHandlingTime(now)
                .setPrincipalId(etmPrincipal.getId())
                .setDownloaded(true)
                .setFound(true);
    }

    private void removePayloadWhenUserIsNotAllowedToSee(List<FieldLayout> fields, EtmPrincipal etmPrincipal) {
        if (etmPrincipal.maySeeEventPayload()) {
            return;
        }
        fields.removeIf(f -> this.eventTags.getPayloadTag().equals(f.getField()));
    }

    private void createCsv(
            Supplier<Iterator<Map<String, Object>>> sourceSupplier,
            int maxResults,
            File outputFile,
            EtmPrincipal etmPrincipal,
            List<FieldLayout> fields,
            GetEventAuditLogBuilder auditLogBuilder,
            Consumer<GetEventAuditLogBuilder> auditLogPersistingConsumer
    ) throws IOException {

        boolean payloadVisible = fields.stream().anyMatch(p -> this.eventTags.getPayloadTag().equals(p.getField()));
        auditLogBuilder.setPayloadVisible(payloadVisible);

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
            boolean first = true;
            String csvSeparator = ",";
            for (FieldLayout field : fields) {
                if (!first) {
                    writer.append(csvSeparator);
                }
                writer.append(escapeToQuotedCsvField(field.getName()));
                first = false;
            }
            Iterator<Map<String, Object>> sourceIterator = sourceSupplier.get();

            for (int i = 0; i < maxResults && sourceIterator.hasNext(); i++) {
                Map<String, Object> sourceValues = sourceIterator.next();
                writer.newLine();
                first = true;
                for (FieldLayout field : fields) {
                    if (!first) {
                        writer.append(csvSeparator);
                    }
                    first = false;
                    String dbFieldName = field.getField();
                    List<Object> values = collectValuesFromPath(dbFieldName, sourceValues);
                    if (values.isEmpty()) {
                        writer.append(escapeToQuotedCsvField(null));
                        continue;
                    }
                    Object value = field.getMultiSelect().select(values);
                    String formattedValue = field.getType().formatValue(value, etmPrincipal.getTimeZone().toZoneId());
                    writer.append(escapeToQuotedCsvField(formattedValue));
                }
                if (auditLogPersistingConsumer != null) {
                    auditLogBuilder.setEventId(sourceValues.get(Keyword.ID.getName()).toString());
                    auditLogPersistingConsumer.accept(auditLogBuilder);
                }
            }
            if (sourceSupplier instanceof Closeable) {
                ((Closeable) sourceSupplier).close();
            }
        }
    }

    private void createXlsx(
            Supplier<Iterator<Map<String, Object>>> sourceSupplier,
            int maxResults,
            File outputFile,
            EtmPrincipal etmPrincipal,
            List<FieldLayout> fields,
            GetEventAuditLogBuilder auditLogBuilder,
            Consumer<GetEventAuditLogBuilder> auditLogPersistingConsumer
    ) throws IOException {

        boolean payloadVisible = fields.stream().anyMatch(p -> this.eventTags.getPayloadTag().equals(p.getField()));
        auditLogBuilder.setPayloadVisible(payloadVisible);

        final int charsPerCell = 30000;
        // First make sure the payload field is at the end of the field list because it can be splitted into several cells.
        fields.sort((f1, f2) -> {
            if (this.eventTags.getPayloadTag().equals(f1.getField())) {
                return 1;
            } else if (this.eventTags.getPayloadTag().equals(f2.getField())) {
                return -1;
            }
            return 0;
        });
        int rowIx = 0;
        int cellIx = 0;
        try (XSSFWorkbook wb = new XSSFWorkbook(); FileOutputStream os = new FileOutputStream(outputFile)) {
            XSSFSheet sheet = wb.createSheet("etm-export");
            XSSFRow row = sheet.createRow(rowIx++);
            for (FieldLayout field : fields) {
                XSSFCell cell = row.createCell(cellIx++);
                cell.setCellValue(field.getName());
            }
            Iterator<Map<String, Object>> sourceIterator = sourceSupplier.get();
            for (int i = 0; i < maxResults && sourceIterator.hasNext(); i++) {
                Map<String, Object> sourceValues = sourceIterator.next();
                row = sheet.createRow(rowIx++);
                cellIx = 0;
                for (FieldLayout field : fields) {
                    String dbFieldName = field.getField();
                    List<Object> values = collectValuesFromPath(dbFieldName, sourceValues);
                    if (values.isEmpty()) {
                        cellIx++;
                        continue;
                    }
                    Object value = field.getMultiSelect().select(values);
                    String formattedValue = field.getType().formatValue(value, etmPrincipal.getTimeZone().toZoneId());

                    if (this.eventTags.getPayloadTag().equals(dbFieldName)) {
                        String payload = value.toString();
                        for (int k = 0; k < payload.length(); k += charsPerCell) {
                            XSSFCell cell = row.createCell(cellIx++);
                            if (k + charsPerCell > payload.length()) {
                                cell.setCellValue(payload.substring(k));
                            } else {
                                cell.setCellValue(payload.substring(k, k + charsPerCell));
                            }
                        }
                    } else {
                        XSSFCell cell = row.createCell(cellIx++);
                        if (value instanceof Number && FieldType.PLAIN.equals(field.getType())) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else {
                            cell.setCellValue(formattedValue);
                        }
                    }
                }
                if (auditLogPersistingConsumer != null) {
                    auditLogBuilder.setEventId(sourceValues.get(Keyword.ID.getName()).toString());
                    auditLogPersistingConsumer.accept(auditLogBuilder);
                }
            }
            wb.write(os);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> collectValuesFromPath(String path, Map<String, Object> valueMap) {
        List<Object> values = new ArrayList<>();
        String[] pathElements = path.split("\\.");
        if (pathElements.length > 1) {
            if (valueMap.containsKey(pathElements[0])) {
                Object object = valueMap.get(pathElements[0]);
                if (object instanceof List) {
                    List<Map<String, Object>> children = (List<Map<String, Object>>) object;
                    for (Map<String, Object> child : children) {
                        values.addAll(collectValuesFromPath(path.substring(pathElements[0].length() + 1), child));
                    }
                } else if (object instanceof Map) {
                    Map<String, Object> child = (Map<String, Object>) object;
                    values.addAll(collectValuesFromPath(path.substring(pathElements[0].length() + 1), child));
                }

            }
        }
        Object object = valueMap.get(path);
        if (object != null) {
            values.add(object);
        }
        return values;
    }

    private String escapeToQuotedCsvField(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replaceAll("\"", "\"\"") + "\"";
    }

    private class ScrollableSearchSupplier implements Iterator<Map<String, Object>>, Closeable {

        private final ScrollableSearch scrollableSearch;

        ScrollableSearchSupplier(ScrollableSearch scrollableSearch) {
            this.scrollableSearch = scrollableSearch;
        }

        @Override
        public boolean hasNext() {
            return this.scrollableSearch.hasNext();
        }

        @Override
        public Map<String, Object> next() {
            SearchHit next = this.scrollableSearch.next();
            if (next != null) {
                Map<String, Object> sourceAsMap = next.getSourceAsMap();
                sourceAsMap.put(Keyword.ID.getName(), next.getId());
                return sourceAsMap;
            }
            return null;
        }

        @Override
        public void close() {
            this.scrollableSearch.clearScrollIds();
        }
    }

    private class TransactionEventSupplier implements Iterator<Map<String, Object>>, Closeable {

        final Iterator<TransactionEvent> iterator;

        TransactionEventSupplier(List<TransactionEvent> events) {
            this.iterator = events.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        @Override
        public Map<String, Object> next() {
            TransactionEvent event = this.iterator.next();
            Map<String, Object> values = new HashMap<>();
            values.put("index", event.index);
            values.put("type", event.objectType);
            values.put(Keyword.ID.getName(), event.id);
            values.put(eventTags.getNameTag(), event.name);
            values.put(eventTags.getEndpointHandlerHandlingTimeTag(), event.handlingTime);
            values.put(eventTags.getEndpointHandlerSequenceNumberTag(), event.sequenceNumber);
            values.put("direction", event.direction);
            values.put(eventTags.getPayloadTag(), event.payload);
            values.put("subtype", event.subtype);
            values.put("endpoint", event.endpoint);
            return values;
        }

        @Override
        public void close() {
        }
    }

}
