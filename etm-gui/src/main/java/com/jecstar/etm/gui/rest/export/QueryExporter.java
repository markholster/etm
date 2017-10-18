package com.jecstar.etm.gui.rest.export;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.search.SearchHit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryExporter {

    private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();

    public File exportToFile(ScrollableSearch scrollableSearch, FileType fileType, int maxRows, EtmPrincipal etmPrincipal, FieldLayout... fields) {
        try {
            File outputFile = File.createTempFile("etm-", "-download");
            outputFile.deleteOnExit();
            if (FileType.CSV.equals(fileType)) {
                createCsv(scrollableSearch, maxRows, outputFile, etmPrincipal, fields);
            } else if (FileType.XLSX.equals(fileType)) {
                createXlsx(scrollableSearch, maxRows, outputFile, etmPrincipal, fields);
            }
            return outputFile;
        } catch (IOException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    private void createCsv(ScrollableSearch scrollableSearch, int maxResults, File outputFile, EtmPrincipal etmPrincipal, FieldLayout... fields) throws IOException {
        final String csvSeparator = ",";

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
            boolean first = true;
            for (FieldLayout field : fields) {
                if (!first) {
                    writer.append(csvSeparator);
                }
                writer.append(escapeToQuotedCsvField(field.getName()));
                first = false;
            }

            for (int i=0; i < maxResults && scrollableSearch.hasNext(); i++) {
                SearchHit searchHit = scrollableSearch.next();
                Map<String, Object> sourceValues = searchHit.getSource();
                writer.newLine();
                first = true;
                for (FieldLayout field : fields) {
                    if (!first) {
                        writer.append(csvSeparator);
                    }
                    first = false;
                    String fieldName = field.getName();
                    if (Keyword.TYPE.getName().equals(fieldName)) {
                        writer.append(escapeToQuotedCsvField(searchHit.getType()));
                        continue;
                    }
                    List<Object> values = collectValuesFromPath(fieldName, sourceValues);
                    if (values.isEmpty()) {
                        writer.append(escapeToQuotedCsvField(null));
                        continue;
                    }
                    Object value = field.getMultiSelect().select(values);
                    String formattedValue = field.getType().formatValue(value, etmPrincipal.getTimeZone().toZoneId());
                    writer.append(escapeToQuotedCsvField(formattedValue));
                }
            }
        }
    }

    private void createXlsx(ScrollableSearch scrollableSearch, int maxResults, File outputFile, EtmPrincipal etmPrincipal, FieldLayout... fields) throws IOException {
        final int charsPerCell = 30000;
        // First make sure the payload field is at the end of the field list because it can be splitted into several cells.
        for (int i = fields.length - 2; i >= 0; i-- ) {
            // Start at the one but last value and move it to the end of the array
            // if the field is a payload field. We can skip the last field,
            // because if it is a payload field its already at the end of the
            // list.
            if (this.eventTags.getPayloadTag().equals(fields[i].getName())) {
                FieldLayout[] temp = new FieldLayout[fields.length];

                // Copy the start of the array until the payload field (at ix == i) to the temp array
                System.arraycopy(fields, 0, temp, 0, i);
                // Copy the end of the array (after the payload field) to the temp array.
                System.arraycopy(fields, i + 1, temp, i, fields.length - i);
                // Move the payload field (at ix == i) to the end of the array.
                temp[fields.length - 1] = fields[i];
                // Reassign temp array to fields
                fields = temp;
            }
        }
        int rowIx = 0;
        int cellIx = 0;
        try (XSSFWorkbook wb = new XSSFWorkbook(); FileOutputStream os = new FileOutputStream(outputFile)){
            XSSFSheet sheet = wb.createSheet("etm-export");
            XSSFRow row = sheet.createRow(rowIx++);
            for (FieldLayout field : fields) {
                XSSFCell cell = row.createCell(cellIx++);
                cell.setCellValue(field.getName());
            }
            for (int i=0; i < maxResults && scrollableSearch.hasNext(); i++) {
                SearchHit searchHit = scrollableSearch.next();
                Map<String, Object> sourceValues = searchHit.getSource();
                row = sheet.createRow(rowIx++);
                cellIx = 0;
                for (FieldLayout field: fields) {
                    String fieldName = field.getName();
                    if (Keyword.TYPE.getName().equals(field.getName())) {
                        XSSFCell cell = row.createCell(cellIx++);
                        cell.setCellValue(searchHit.getType());
                        continue;
                    }
                    List<Object> values = collectValuesFromPath(fieldName, sourceValues);
                    if (values.isEmpty()) {
                        cellIx++;
                        continue;
                    }
                    Object value = field.getMultiSelect().select(values);
                    String formattedValue = field.getType().formatValue(value, etmPrincipal.getTimeZone().toZoneId());

                    if (this.eventTags.getPayloadTag().equals(fieldName)) {
                        String payload = value.toString();
                        for (int k=0; k < payload.length(); k += charsPerCell) {
                            XSSFCell cell = row.createCell(cellIx++);
                            if (k + charsPerCell > payload.length()) {
                                cell.setCellValue(payload.substring(k));
                            } else {
                                cell.setCellValue(payload.substring(k, k + charsPerCell));
                            }
                        }
                    } else {
                        XSSFCell cell = row.createCell(cellIx++);
                         if (value instanceof Number){
                            cell.setCellValue(((Number)value).doubleValue());
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
}
