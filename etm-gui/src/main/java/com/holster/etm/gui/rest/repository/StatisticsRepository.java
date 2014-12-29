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

	public Map<String, Map<Long, Long>> getTransactionCountStatistics(Long startTime, Long endTime, int topTransactions) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<Object> transactionNames = getTransactionNames(startTime, endTime);
		if (transactionNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> totals = new HashMap<String, Long>();
		final Map<String, Map<Long, Long>> data = new HashMap<String, Map<Long, Long>>();
		BuiltStatement builtStatement = QueryBuilder.select("transactionName", "timeunit", "transactionStart")
		        .from(this.keyspace, "transactionname_counter")
		        .where(QueryBuilder.in("transactionName", transactionNames))
		        .and(QueryBuilder.gte("timeunit", new Date(startTime))).and(QueryBuilder.lte("timeunit", new Date(endTime)));
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
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
	
	public Map<String, Map<Long, Long>> getEventsCountStatistics(Long startTime, Long endTime, int topTransactions) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<Object> eventNames = getEventNames(startTime, endTime);
		if (eventNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> totals = new HashMap<String, Long>();
		final Map<String, Map<Long, Long>> data = new HashMap<String, Map<Long, Long>>();
		BuiltStatement builtStatement = QueryBuilder.select("eventName", "timeunit", "count")
		        .from(this.keyspace, "eventname_counter")
		        .where(QueryBuilder.in("eventName", eventNames))
		        .and(QueryBuilder.gte("timeunit", new Date(startTime))).and(QueryBuilder.lte("timeunit", new Date(endTime)));
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
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
	
	
	public Map<String, Map<Long, Average>> getTransactionPerformanceStatistics(Long startTime, Long endTime, int topTransactions) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<Object> transactionNames = getTransactionNames(startTime, endTime);
		if (transactionNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> highest = new HashMap<String, Long>();
		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
		BuiltStatement builtStatement = QueryBuilder.select("transactionName", "startTime", "finishTime", "expiryTime")
				.from(this.keyspace, "transaction_event")
				.where(QueryBuilder.in("transactionName", transactionNames))
				.and(QueryBuilder.gte("startTime", new Date(startTime))).and(QueryBuilder.lte("startTime", new Date(endTime)));
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String transactionName = row.getString(0);
			Date transactionStartTime = row.getDate(1);
			Date transactionFinishTime = row.getDate(2);
			Date transactionExpiryTime = row.getDate(3);
			long timeUnit = normalizeTime(transactionStartTime.getTime(), this.normalizeMinuteFactor);
			long responseTime = 0;
			if (transactionFinishTime != null && transactionFinishTime.getTime() != 0) {
				responseTime = transactionFinishTime.getTime() - transactionStartTime.getTime(); 
			} else if (transactionExpiryTime != null && transactionExpiryTime.getTime() != 0) {
				if (transactionExpiryTime.getTime() < System.currentTimeMillis()) {
					// transaction expired, set reponse time to expiry time.
					responseTime = transactionExpiryTime.getTime() - transactionStartTime.getTime();
				} else {
					// transaction not yet finished, and transaction not expired. Ignore this one.
					continue;
				}
			} else {
				// transaction not yet finished, and no expiry time available. Ignore this one.
				continue;
			}
			if (!highest.containsKey(transactionName)) {
				highest.put(transactionName, responseTime);
			} else {
				Long currentValue = highest.get(transactionName);
				if (responseTime > currentValue) {
					highest.put(transactionName, responseTime);
				}
			}
			if (!data.containsKey(transactionName)) {
				Map<Long, Average> values = new HashMap<Long, Average>();
				values.put(timeUnit, new Average(responseTime));
				data.put(transactionName, values);
			} else {
				Map<Long, Average> values = data.get(transactionName);
				if (!values.containsKey(timeUnit)) {
					values.put(timeUnit, new Average(responseTime));
				} else {
					Average currentValue = values.get(timeUnit);
					currentValue.add(responseTime);
				}
			}
		}
		List<Long> values = new ArrayList<>(highest.values().size());
		Collections.sort(values);
		Collections.reverse(values);
		if (values.size() > topTransactions) {
			for (int i = topTransactions; i <= values.size(); i++) {
				Long valueToRemove = values.get(i);
				for (String name : highest.keySet()) {
					if (highest.get(name) == valueToRemove) {
						data.remove(name);
						break;
					}
				}
			}
		}
		return data;

    }
	
	private List<Object> getTransactionNames(long startTime, long endTime) {
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
		return transactionNames;
	}
	
	private List<Object> getEventNames(long startTime, long endTime) {
		BuiltStatement builtStatement = QueryBuilder.select("name").from(this.keyspace , "event_occurrences").where(QueryBuilder.in("timeunit", determineHours(startTime, endTime))).and(QueryBuilder.eq("type", "EventName"));
		List<Object> eventNames = new ArrayList<Object>();
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String name = row.getString(0);
			if (!eventNames.contains(name)) {
				eventNames.add(name);
			}
		}
		return eventNames;
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
