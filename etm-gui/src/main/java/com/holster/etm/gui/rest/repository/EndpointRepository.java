package com.holster.etm.gui.rest.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class EndpointRepository {

	private final Session session;
	
	private final PreparedStatement selectEndpointNamesStatement;

	public EndpointRepository(Session session) {
	    this.session = session;
	    this.selectEndpointNamesStatement = this.session.prepare("select endpoint from endpoint_config allow filtering;");
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

}
