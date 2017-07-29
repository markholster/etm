package com.jecstar.etm.gui.rest.services.search;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.search.SearchHit;

import com.jecstar.etm.domain.writers.TelemetryEventTags;
import com.jecstar.etm.domain.writers.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.gui.rest.services.Keyword;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

class QueryExporter {

	private static final SimpleDateFormat ISO_UTC_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	static {
		ISO_UTC_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	private final JsonConverter converter = new JsonConverter();
	private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();
	private final Comparator<Object> objectComparator = (o1, o2) -> o1.toString().compareTo(o2.toString());
	
	public String getContentType(String fileType) {
		if ("csv".equalsIgnoreCase(fileType)) {
			return "text/csv";
		} else if ("xlsx".equalsIgnoreCase(fileType)) {
			return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		}
		return null;
	}
	
	public File exportToFile(ScrollableSearch scrollableSearch, String fileType, int maxRows, SearchRequestParameters parameters, EtmPrincipal etmPrincipal) {
		try {
			File outputFile = File.createTempFile("etm-", "-download");
			outputFile.deleteOnExit();
		    if ("csv".equalsIgnoreCase(fileType)) {
		    	createCsv(scrollableSearch, maxRows, outputFile, parameters, etmPrincipal);
		    } else if ("xlsx".equalsIgnoreCase(fileType)) {
		    	createXlsx(scrollableSearch, maxRows, outputFile, parameters, etmPrincipal);
		    }
			return outputFile;
		} catch (IOException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}	
	}
	
	private void createCsv(ScrollableSearch scrollableSearch, int maxResults, File outputFile, SearchRequestParameters parameters, EtmPrincipal etmPrincipal) throws IOException {
		final String csvSeparator = ",";
		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath());) {
			boolean first = true;
			for (int i = 0; i < parameters.getFields().size(); i++ ) {
				if (!first) {
					writer.append(csvSeparator);
				}
				writer.append(escapeToQuotedCsvField(this.converter.getString("name", parameters.getFieldsLayout().get(i))));
				first = false;
			}
			
			for (int i=0; i < maxResults && scrollableSearch.hasNext(); i++) {
				SearchHit searchHit = scrollableSearch.next();
				Map<String, Object> sourceValues = searchHit.getSource();
				writer.newLine();
				first = true;
				for (int j = 0; j < parameters.getFields().size(); j++ ) {
					if (!first) {
						writer.append(csvSeparator);
					}
					first = false;
					String field = parameters.getFields().get(j);
					if (Keyword.TYPE.getName().equals(field)) {
						writer.append(escapeToQuotedCsvField(searchHit.getType()));
						continue;
					} 
					List<Object> values = collectValuesFromPath(field, sourceValues);
					if (values.isEmpty()) {
						writer.append(escapeToQuotedCsvField(null));
						continue;
					}
					Map<String, Object> fieldLayout = parameters.getFieldsLayout().get(j);
					Object value = selectValue(values, fieldLayout);
					writer.append(escapeToQuotedCsvField(formatValue(value, fieldLayout, etmPrincipal)));
				}
			}
		}
	}
	
	private void createXlsx(ScrollableSearch scrollableSearch, int maxResults, File outputFile, SearchRequestParameters parameters, EtmPrincipal etmPrincipal) throws IOException {
		SimpleDateFormat principalTimeZoneFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		principalTimeZoneFormatter.setTimeZone(etmPrincipal.getTimeZone());
		
		final int charsPerCell = 30000;
		// First make sure the payload field is at the end of the field list because it can be splitted into several cells.
		for (int i = parameters.getFields().size() - 2; i >= 0; i-- ) {
			// Start at the one but last value and move to the end of the list
			// if the field is a payload field. We can skip the last field,
			// because if it is a payload field its already at the end of the
			// list. If it's not a payload field it doesn't matter because the
			// payload field will be added behind it eventually.
			if (this.eventTags.getPayloadTag().equals(parameters.getFields().get(i))) {
				String removedField = parameters.getFields().remove(i);
				parameters.getFields().add(removedField);
				Map<String, Object> removedLayout = parameters.getFieldsLayout().remove(i);
				parameters.getFieldsLayout().add(removedLayout);
			}
		}
		int rowIx = 0;
		int cellIx = 0;
		try (XSSFWorkbook wb = new XSSFWorkbook(); FileOutputStream os = new FileOutputStream(outputFile);){
			XSSFSheet sheet = wb.createSheet("etm-export");
			XSSFRow row = sheet.createRow(rowIx++);
			for (int i = 0; i < parameters.getFields().size(); i++ ) {
				XSSFCell cell = row.createCell(cellIx++);
				cell.setCellValue(this.converter.getString("name", parameters.getFieldsLayout().get(i)));
			}
			for (int i=0; i < maxResults && scrollableSearch.hasNext(); i++) {
				SearchHit searchHit = scrollableSearch.next();
				Map<String, Object> sourceValues = searchHit.getSource();
				row = sheet.createRow(rowIx++);
				cellIx = 0;
				for (int j = 0; j < parameters.getFields().size(); j++ ) {
					String field = parameters.getFields().get(j);
					if (Keyword.TYPE.getName().equals(field)) {
						XSSFCell cell = row.createCell(cellIx++);
						cell.setCellValue(searchHit.getType());
						continue;
					}
					List<Object> values = collectValuesFromPath(field, sourceValues);
					if (values.isEmpty()) {
						cellIx++;
						continue;
					}
					Map<String, Object> fieldLayout = parameters.getFieldsLayout().get(j);
					Object value = selectValue(values, fieldLayout);
					if (value == null) {
						cellIx++;
						continue;
					}
					String format = this.converter.getString("format", fieldLayout, "plain");
					// When changed also change the searchresults-layout.js with the same functionality.
					if (this.eventTags.getPayloadTag().equals(field)) {
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
						if ("isoutctimestamp".equals(format) && value instanceof Number) {
							cell.setCellValue(ISO_UTC_FORMATTER.format(new Date(((Number)value).longValue())));
						} else if ("isotimestamp".equals(format) && value instanceof Number) {
							cell.setCellValue(principalTimeZoneFormatter.format(new Date(((Number)value).longValue())));
						} else if (value instanceof Number){
							cell.setCellValue(((Number)value).doubleValue());
						} else {
							cell.setCellValue(value.toString());
						}
					}
				}
			}
            wb.write(os);
		}
	}

	private Object selectValue(List<Object> values, Map<String, Object> fieldLayout) {
		String array = this.converter.getString("array", fieldLayout, "first");
		Object value;
		// When changed also change the searchresults-layout.js with the same functionality.
		if ("last".equals(array)) {
			value = values.get(values.size() - 1);
		} else if ("lowest".equals(array)) {
			values.sort(this.objectComparator);
			value = values.get(0);
		} else if ("highest".equals(array))  {
			values.sort(this.objectComparator);
			value = values.get(values.size() - 1);
		} else {
			value = values.get(0);
		}
		return value;
	}
	
	private String formatValue(Object value, Map<String, Object> fieldLayout, EtmPrincipal etmPrincipal) {
		// When changed also change the searchresults-layout.js with the same functionality.
		if (value == null) {
			return null;
		}
		String format = this.converter.getString("format", fieldLayout, "plain");
		if ("isoutctimestamp".equals(format)) {
			if (value instanceof Number) {
				return ISO_UTC_FORMATTER.format(new Date(((Number)value).longValue()));
			}
			return value.toString();
		} else if ("isotimestamp".equals(format)) {
			if (value instanceof Number) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
				formatter.setTimeZone(etmPrincipal.getTimeZone());
				return formatter.format(new Date(((Number)value).longValue()));
			}
			return value.toString();
		}
 		return value.toString();
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
