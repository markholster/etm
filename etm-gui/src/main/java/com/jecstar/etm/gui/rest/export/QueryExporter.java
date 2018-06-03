package com.jecstar.etm.gui.rest.export;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.search.TransactionEvent;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.search.SearchHit;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;

public class QueryExporter {

    private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();

    public File exportToFile(ScrollableSearch scrollableSearch, FileType fileType, int maxRows, EtmPrincipal etmPrincipal, FieldLayout... fields) {
        try {
            File outputFile = File.createTempFile("etm-", "-download");
            outputFile.deleteOnExit();
            if (FileType.CSV.equals(fileType)) {
                createCsv(() -> new ScrollableSearchSupplier(scrollableSearch), maxRows, outputFile, etmPrincipal, fields);
            } else if (FileType.XLSX.equals(fileType)) {
                createXlsx(() -> new ScrollableSearchSupplier(scrollableSearch), maxRows, outputFile, etmPrincipal, fields);
            }
            return outputFile;
        } catch (IOException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public File exportToFile(List<TransactionEvent> events, FileType fileType, EtmPrincipal etmPrincipal) {
        FieldLayout[] fields = new FieldLayout[]{
                new FieldLayout("Id", "id", FieldType.PLAIN, MultiSelect.FIRST),
                new FieldLayout("Name", eventTags.getNameTag(), FieldType.PLAIN, MultiSelect.FIRST),
                new FieldLayout("Date", eventTags.getEndpointHandlerHandlingTimeTag(), FieldType.ISO_TIMESTAMP, MultiSelect.FIRST),
                new FieldLayout("Type", "type", FieldType.PLAIN, MultiSelect.FIRST),
                new FieldLayout("Direction", "direction", FieldType.PLAIN, MultiSelect.FIRST),
                new FieldLayout("Subtype", "subtype", FieldType.PLAIN, MultiSelect.FIRST),
                new FieldLayout("Endpoint", "endpoint", FieldType.PLAIN, MultiSelect.FIRST),
                new FieldLayout("Payload", eventTags.getPayloadTag(), FieldType.PLAIN, MultiSelect.FIRST),
        };

        try {
            File outputFile = File.createTempFile("etm-", "-download");
            outputFile.deleteOnExit();
            if (FileType.CSV.equals(fileType)) {
                createCsv(() -> new TransactionEventSupplier(events), Integer.MAX_VALUE, outputFile, etmPrincipal, fields);
            } else if (FileType.XLSX.equals(fileType)) {
                createXlsx(() -> new TransactionEventSupplier(events), Integer.MAX_VALUE, outputFile, etmPrincipal, fields);
            }
            return outputFile;
        } catch (IOException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    private void createCsv(Supplier<Iterator<Map<String, Object>>> sourceSupplier, int maxResults, File outputFile, EtmPrincipal etmPrincipal, FieldLayout... fields) throws IOException {
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
            }
            if (sourceSupplier instanceof Closeable) {
                ((Closeable) sourceSupplier).close();
            }
        }
    }

    private void createXlsx(Supplier<Iterator<Map<String, Object>>> sourceSupplier, int maxResults, File outputFile, EtmPrincipal etmPrincipal, FieldLayout... fields) throws IOException {
        final int charsPerCell = 30000;
        // First make sure the payload field is at the end of the field list because it can be splitted into several cells.
        for (int i = fields.length - 2; i >= 0; i--) {
            // Start at the one but last value and move it to the end of the array
            // if the field is a payload field. We can skip the last field,
            // because if it is a payload field its already at the end of the
            // list.
            if (this.eventTags.getPayloadTag().equals(fields[i].getField())) {
                FieldLayout[] temp = new FieldLayout[fields.length];

                // Copy the start of the array until the payload field (at ix == i) to the temp array
                System.arraycopy(fields, 0, temp, 0, i);
                // Copy the end of the array (after the payload field) to the temp array.
                System.arraycopy(fields, i + 1, temp, i, fields.length - i - 1);
                // Move the payload field (at ix == i) to the end of the array.
                temp[fields.length - 1] = fields[i];
                // Reassign temp array to fields
                fields = temp;
            }
        }
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
                sourceAsMap.put(Keyword.TYPE.getName(), next.getType());
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
            values.put("type", event.objectType != null ? event.objectType : event.type);
            values.put("id", event.id);
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
