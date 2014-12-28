package com.holster.etm.gui.rest.repository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class StatisticsRepository {

	private final long normalizeMinuteFactor = 1000 * 60;
	private final long normalizeHourFactor = 1000 * 60 * 60;
	final String keyspace = "etm";
	
	private final Session session; 
	
	public StatisticsRepository(Session session) {
		this.session = session;
	}

	public Map<String, Map<Long, Long>> getTransactionStatistics(Long startTime, Long endTime, int topTransactions) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		BuiltStatement builtStatement = QueryBuilder.select("name").from(this.keyspace , "event_occurrences").where(QueryBuilder.in("timeunit", determineHours(startTime, endTime))).and(QueryBuilder.eq("type", "TransactionName"));
		List<Object> transactionNames = new ArrayList<Object>();
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String name = row.getString(0);
			if (!transactionNames.contains(name)) {
				transactionNames.add(name);
			}
		}
		if (transactionNames.size() == 0) {
			return Collections.emptyMap();
		}
		builtStatement = QueryBuilder.select("transactionName", "timeunit", "transactionStart")
		        .from(this.keyspace, "transactionname_counter")
		        .where(QueryBuilder.in("transactionName", transactionNames))
		        .and(QueryBuilder.gte("timeunit", new Date(startTime))).and(QueryBuilder.lte("timeunit", new Date(endTime)));
		resultSet = this.session.execute(builtStatement);
		final Map<String, Long> totals = new HashMap<String, Long>();
		final Map<String, Map<Long, Long>> data = new HashMap<String, Map<Long, Long>>();
		iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String name = row.getString(0);
			long timeUnit = normalizeTime(row.getDate(1).getTime(), this.normalizeMinuteFactor);
			long count = row.getLong(2);
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
	
	private List<Object> determineHours(long startTime, long endTime) {
	    Calendar startCalendar = Calendar.getInstance();
	    Calendar endCalendar = Calendar.getInstance();
	    startCalendar.setTimeInMillis(startTime);
	    endCalendar.setTimeInMillis(endTime);
	    // For unknown reasons cassandra gives an error when the arraylist is created with the Date generic type.
	    List<Object> result = new ArrayList<Object>();
	    do {
	    	result.add(new Date(normalizeTime(startCalendar.getTime().getTime(), this.normalizeHourFactor)));
	    	startCalendar.add(Calendar.HOUR, 1);
	    } while (startCalendar.before(endCalendar) || (!startCalendar.before(endCalendar) && startCalendar.get(Calendar.HOUR_OF_DAY) == endCalendar.get(Calendar.HOUR_OF_DAY)));
	    return result;
    }

	private long normalizeTime(long timeInMillis, long factor) {
		return (timeInMillis / factor) * factor;
    }
	
}
