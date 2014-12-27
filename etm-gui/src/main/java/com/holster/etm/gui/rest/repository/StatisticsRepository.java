package com.holster.etm.gui.rest.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class StatisticsRepository {

	private final PreparedStatement selectTransactionStatisticsStatement;
	private final Session session; 
	
	public StatisticsRepository(Session session) {
		this.session = session;
		final String keyspace = "etm";
		this.selectTransactionStatisticsStatement = session.prepare("select transactionName, timeunit, transactionStart from " + keyspace + ".transactionname_counter where token(timeunit) >= token(?) and token(timeunit) <= token(?)");
	}

	public Map<String, Map<Long, Long>> getTransactionStatistics(Long startTime, Long endTime, int topTransactions) {
		final ResultSet resultSet = this.session.execute(this.selectTransactionStatisticsStatement.bind(new Date(startTime), new Date(endTime)));
		final Map<String, Long> totals = new HashMap<String, Long>();
		final Map<String, Map<Long, Long>> data = new HashMap<String, Map<Long, Long>>();
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String name = row.getString(1);
			long timeUnit = normalizeTimeToMinute(row.getDate(2).getTime());
			long count = row.getLong(3);
			if (!totals.containsKey(name)) {
				totals.put(name, count);
			} else {
				Long currentValue = totals.get(name);
				totals.put(name, currentValue + count);
			}
			if (!data.containsKey(name)) {
				Map<Long, Long> values = new HashMap<Long, Long>();
				values.put(timeUnit, count);
				data.put(name, values);
			} else {
				Map<Long, Long> values = data.get(name);
				if (!values.containsKey(timeUnit)) {
					values.put(timeUnit, count);
				} else {
					Long currentValue = values.get(timeUnit);
					values.put(timeUnit, currentValue + count);
				}
			}
		}
		List<Long> values = new ArrayList<>(totals.values().size());
		Collections.sort(values);
		Collections.reverse(values);
		if (values.size() > topTransactions) {
			for (int i = topTransactions; i <= values.size(); i++) {
				Long valueToRemove = values.get(i);
				for (String name : totals.keySet()) {
					if (totals.get(name) == valueToRemove) {
						data.remove(name);
						break;
					}
				}
			}
		}
		return data;
    }
	
	private long normalizeTimeToMinute(long timeMillis) {
		return (timeMillis / 60000) * 60000;
    }
}
