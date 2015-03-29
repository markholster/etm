package com.jecstar.etm.gui.rest.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.ExpressionParserFactory;

public class EndpointRepository {

	private final Session session;
	
	private final PreparedStatement selectEndpointNamesStatement;
	private final PreparedStatement selectEndpointStatement;
	private final PreparedStatement deleteEndpointStatement;

	public EndpointRepository(Session session) {
	    this.session = session;
	    this.selectEndpointNamesStatement = this.session.prepare("select endpoint from endpoint_config allow filtering;");
	    this.selectEndpointStatement = this.session.prepare("select direction, applicationParsers, eventNameParsers, correlationParsers, transactionNameParsers, slaRules from endpoint_config where endpoint = ?;");
	    this.deleteEndpointStatement = this.session.prepare("delete from endpoint_config where endpoint = ?;");
    }
	
	public List<String> getEndpointNames() {
		List<String> endpointNames = new ArrayList<String>();
		ResultSet resultSet = this.session.execute(this.selectEndpointNamesStatement.bind());
		Iterator<Row> rowIterator = resultSet.iterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			endpointNames.add(row.getString(0));
		}
		int ix = endpointNames.indexOf("*");
		if (ix != -1) {
			endpointNames.remove(ix);
		}
		Collections.sort(endpointNames);
		return endpointNames;
	}

	public EndpointConfiguration getEndpointConfiguration(String endpointName) {
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration();
		endpointConfiguration.name = endpointName;
	    Row row = this.session.execute(this.selectEndpointStatement.bind(endpointName)).one();
	    if (row != null) {
	    	String direction = row.getString(0);
	    	if (direction != null) {
	    		endpointConfiguration.direction = TelemetryEventDirection.valueOf(direction);
	    	}
	    	addExpressionParsers(endpointConfiguration.applicationParsers, row.getList(1, String.class));
	    	addExpressionParsers(endpointConfiguration.eventNameParsers, row.getList(2, String.class));
	    	addExpressionParsers(endpointConfiguration.correlationParsers, row.getMap(3, String.class, String.class));
	    	addExpressionParsers(endpointConfiguration.transactionNameParsers, row.getList(4, String.class));
	    	// TODO add sla's
	    }
	    return endpointConfiguration;
    }
	
	public void deleteEndpointConfiguration(String endpointName) {
	    this.session.execute(this.deleteEndpointStatement.bind(endpointName));
    }
	
	private void addExpressionParsers(List<ExpressionParser> expressionParsers, List<String> dbValues) {
		if (dbValues == null || dbValues.size() == 0) {
			return;
		}
		for (String value : dbValues) {
			expressionParsers.add(ExpressionParserFactory.createExpressionParserFromConfiguration(value));
		}
	}
	
	private void addExpressionParsers(Map<String, ExpressionParser> expressionParsers, Map<String, String> dbValues) {
		if (dbValues == null || dbValues.size() == 0) {
			return;
		}
		for (String value : dbValues.keySet()) {
			expressionParsers.put(value, ExpressionParserFactory.createExpressionParserFromConfiguration(dbValues.get(value)));
		}
	}
}
