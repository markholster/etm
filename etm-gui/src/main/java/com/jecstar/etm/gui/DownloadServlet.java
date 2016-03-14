package com.jecstar.etm.gui;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.sort.SortOrder;

import com.jecstar.etm.core.converter.TelemetryEventConverterTags;
import com.jecstar.etm.core.converter.json.TelemetryEventConverterTagsJsonImpl;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@WebServlet("/search/download")
public class DownloadServlet extends HttpServlet {

	@GuiConfiguration
	@Inject
	private Client elasticClient;
	
	private static final long serialVersionUID = -5943929079966118935L;
	
	private static final DateFormat csvDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private final TelemetryEventConverterTags tags = new TelemetryEventConverterTagsJsonImpl();
	
	private final String csvSeparator = ";";
	private final int charsPerCell = 32767; 
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String queryString = req.getParameter("queryString");
		String start = req.getParameter("start");
		String rows = req.getParameter("rows");
		String sortField = req.getParameter("sortField");
		String sortOrder = req.getParameter("sortOrder");
		
		int startIx = 0;
		int nrOfRows = 500;
		if (start != null && start.trim().length() > 0) {
			startIx = Integer.valueOf(start);
		}
		if (rows != null && rows.trim().length() > 0) {
			nrOfRows = Integer.valueOf(rows);
			if (nrOfRows > 500) {
				nrOfRows = 500;
			}
		}
		SearchRequestBuilder searchRequest = this.elasticClient.prepareSearch("etm_event_all")
				.setFrom(startIx)
				.setSize(nrOfRows)
				.setQuery(QueryBuilders.queryStringQuery(queryString)
						.defaultField("content")
						.lowercaseExpandedTerms(false)
						.analyzeWildcard(true)
// TODO Hard coded for Achmea, we need this to be an input field from the browser/user.						
						.timeZone("Europe/Amsterdam"));
		if (sortField != null && sortField.trim().length() > 0) {
			if ("asc".equalsIgnoreCase(sortOrder)) {
				searchRequest.addSort(sortField, SortOrder.ASC);
			} else {
				searchRequest.addSort(sortField, SortOrder.DESC);
			}
		}
		SearchResponse searchResponse = searchRequest.get();
		resp.setContentType("text/csv");
		resp.setHeader("Content-Disposition", "inline; filename=\"result.csv\""); 
		resp.setCharacterEncoding(System.getProperty("file.encoding"));
		resp.getWriter().write("\"id\""
				+ this.csvSeparator + "\"correlation id\""
				+ this.csvSeparator + "\"creation time\""
				+ this.csvSeparator + "\"expiry time\""
				+ this.csvSeparator + "\"name\""
				+ this.csvSeparator + "\"application\""
				+ this.csvSeparator + "\"transaction name\""
				+ this.csvSeparator + "\"endpoint\""
				+ this.csvSeparator + "\"response time\""
				+ this.csvSeparator + "\"content\"\r\n");
		for (SearchHit hit : searchResponse.getHits()) {
			Map<String, Object> valueMap = hit.getSource();
			resp.getWriter().write("\"" + hit.getId()+ "\""
					+ this.csvSeparator + "\"" + getStringValue(this.tags.getCorrelationIdTag(), valueMap) + "\""
					+ this.csvSeparator + "\"" + getDateValue(this.tags.getCreationTimeTag(), valueMap) + "\""
					+ this.csvSeparator + "\"" + getDateValue(this.tags.getExpiryTimeTag(), valueMap) + "\""
					+ this.csvSeparator + "\"" + getStringValue(this.tags.getNameTag(), valueMap) + "\""
					+ this.csvSeparator + "\"" + getStringValue(this.tags.getApplicationTag(), valueMap) + "\""
					+ this.csvSeparator + "\"" + getStringValue(this.tags.getTransactionNameTag(), valueMap) + "\""
					+ this.csvSeparator + "\"" + getStringValue(this.tags.getEndpointTag(), valueMap) + "\""
					+ this.csvSeparator + "\"" + getStringValue(this.tags.getResponsetimeTag(), valueMap) + "\"");
			String content = getStringValue(this.tags.getContentTag(), valueMap);
			for (int i=0; i < content.length(); i += this.charsPerCell) {
				if (i + this.charsPerCell > content.length()) {
					resp.getWriter().write(this.csvSeparator + "\"" + content.substring(i) + "\"");
				} else {
					resp.getWriter().write(this.csvSeparator + "\"" + content.substring(i, i + this.charsPerCell) + "\"");
				}
			}
			resp.getWriter().write("\r\n");
		}
		resp.flushBuffer();
	}
	
	private String getDateValue(String key, Map<String, ?> valueMap) {
		if (valueMap.containsKey(key)) {
			Object object = valueMap.get(key);
			if (object instanceof SearchHitField) {
				long time = ((SearchHitField)object).getValue(); 
				return csvDateFormat.format(new Date(time)).replaceAll("\"", "\"\"");
			}
			return csvDateFormat.format(new Date(((Number)valueMap.get(key)).longValue())).replaceAll("\"", "\"\"");
		}
		return "";
	}
	
	private String getStringValue(String key, Map<String, ?> valueMap) {
		if (valueMap.containsKey(key)) {
			Object object = valueMap.get(key);
			if (object instanceof SearchHitField) {
				return ((SearchHitField)object).getValue().toString().replaceAll("\"", "\"\"");
			}
			return valueMap.get(key).toString().replaceAll("\"", "\"\"");
		}
		return "";
	}

}
