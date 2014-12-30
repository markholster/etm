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
	
	public Map<String, Map<Long, Long>> getTransactionCountStatistics(String transactionName, Long startTime, Long endTime) {
		List<Object> transactionNames = getTransactionNames(startTime, endTime);
		if (transactionNames.size() == 0 || !transactionNames.contains(transactionName)) {
			return Collections.emptyMap();
		}
		transactionNames.clear();
		transactionNames.add(transactionName);
		return getTransactionCountStatistics(transactionNames, startTime, endTime, 1);
    }

	public Map<String, Map<Long, Long>> getTransactionCountStatistics(Long startTime, Long endTime, int maxTransactions) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<Object> transactionNames = getTransactionNames(startTime, endTime);
		if (transactionNames.size() == 0) {
			return Collections.emptyMap();
		}
		return getTransactionCountStatistics(transactionNames, startTime, endTime, maxTransactions);
    }
	
	private Map<String, Map<Long, Long>> getTransactionCountStatistics(List<Object> transactionNames, Long startTime, Long endTime, int maxTransactions) {
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
			if (count == 0) {
				continue;
			}
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
		filterCountsToMaxResults(maxTransactions, totals, data);
		return data;		
	}
	
	public Map<String, Map<Long, Long>> getMessagesCountStatistics(Long startTime, Long endTime, int maxMessages) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<Object> eventNames = getMessageNames(startTime, endTime);
		if (eventNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> totals = new HashMap<String, Long>();
		final Map<String, Map<Long, Long>> data = new HashMap<String, Map<Long, Long>>();
		BuiltStatement builtStatement = QueryBuilder.select("eventName", "timeunit", "messageRequestCount", "messageDatagramCount")
		        .from(this.keyspace, "eventname_counter")
		        .where(QueryBuilder.in("eventName", eventNames))
		        .and(QueryBuilder.gte("timeunit", new Date(startTime))).and(QueryBuilder.lte("timeunit", new Date(endTime)));
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String name = row.getString(0);
			long timeUnit = normalizeTime(row.getDate(1).getTime(), this.normalizeMinuteFactor);
			long count = row.getLong(2) + row.getLong(3);
			if (count == 0) {
				continue;
			}
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
		filterCountsToMaxResults(maxMessages, totals, data);
		return data;
    }

	public Map<String, Map<Long, Average>> getTransactionPerformanceStatistics(Long startTime, Long endTime, int maxTransactions) {
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
				.from(this.keyspace, "transaction_performance")
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
			long responseTime = determineResponseTime(transactionStartTime, transactionFinishTime, transactionExpiryTime);
			if (responseTime == -1) {
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
		filterAveragesToMaxResults(maxTransactions, highest, data);
		return data;
    }

	public Map<String, Map<Long, Average>> getMessagesPerformanceStatistics(Long startTime, Long endTime, int maxMessages) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<Object> messagesNames = getMessageNames(startTime, endTime);
		if (messagesNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> highest = new HashMap<String, Long>();
		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
		BuiltStatement builtStatement = QueryBuilder.select("name", "startTime", "finishTime", "expiryTime")
				.from(this.keyspace, "message_performance")
				.where(QueryBuilder.in("name", messagesNames))
				.and(QueryBuilder.gte("startTime", new Date(startTime))).and(QueryBuilder.lte("startTime", new Date(endTime)));
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String messageName = row.getString(0);
			Date messageStartTime = row.getDate(1);
			Date messageFinishTime = row.getDate(2);
			Date messageExpiryTime = row.getDate(3);
			long timeUnit = normalizeTime(messageStartTime.getTime(), this.normalizeMinuteFactor);
			long responseTime = determineResponseTime(messageStartTime, messageFinishTime, messageExpiryTime);
			if (responseTime == -1) {
					continue;
			}
			if (!highest.containsKey(messageName)) {
				highest.put(messageName, responseTime);
			} else {
				Long currentValue = highest.get(messageName);
				if (responseTime > currentValue) {
					highest.put(messageName, responseTime);
				}
			}
			if (!data.containsKey(messageName)) {
				Map<Long, Average> values = new HashMap<Long, Average>();
				values.put(timeUnit, new Average(responseTime));
				data.put(messageName, values);
			} else {
				Map<Long, Average> values = data.get(messageName);
				if (!values.containsKey(timeUnit)) {
					values.put(timeUnit, new Average(responseTime));
				} else {
					Average currentValue = values.get(timeUnit);
					currentValue.add(responseTime);
				}
			}
		}
		filterAveragesToMaxResults(maxMessages, highest, data);
		return data;
    }
	
	public void filterCountsToMaxResults(int maxResults, Map<String, Long> highest, Map<String, Map<Long, Long>> data) {
		List<Long> values = new ArrayList<>(highest.values().size());
		values.addAll(highest.values());
		Collections.sort(values);
		Collections.reverse(values);
		if (values.size() > maxResults) {
			for (int i = maxResults; i < values.size(); i++) {
				Long valueToRemove = values.get(i);
				for (String name : highest.keySet()) {
					if (highest.get(name).equals(valueToRemove)) {
						data.remove(name);
						highest.remove(name);
						break;
					}
				}
			}
		}
	}
	
	public void filterAveragesToMaxResults(int maxResults, Map<String, Long> highest, Map<String, Map<Long, Average>> data) {
		List<Long> values = new ArrayList<>(highest.values().size());
		values.addAll(highest.values());
		Collections.sort(values);
		Collections.reverse(values);
		if (values.size() > maxResults) {
			for (int i = maxResults; i < values.size(); i++) {
				Long valueToRemove = values.get(i);
				for (String name : highest.keySet()) {
					if (highest.get(name).equals(valueToRemove)) {
						data.remove(name);
						highest.remove(name);
						break;
					}
				}
			}
		}
	}
	
	private long determineResponseTime(Date startTime, Date finishTime, Date expiryTime) {
		if (finishTime != null && finishTime.getTime() != 0) {
			return finishTime.getTime() - startTime.getTime(); 
		} else if (expiryTime != null && expiryTime.getTime() != 0) {
			if (expiryTime.getTime() < System.currentTimeMillis()) {
				// transaction expired, set reponse time to expiry time.
				return expiryTime.getTime() - startTime.getTime();
			} else {
				// transaction not yet finished, and transaction not expired. Ignore this one.
				return -1;
			}
		} 
		// transaction not yet finished, and no expiry time available. Ignore this one.
		return -1;
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
	
	private List<Object> getMessageNames(long startTime, long endTime) {
		BuiltStatement builtStatement = QueryBuilder.select("name").from(this.keyspace , "event_occurrences").where(QueryBuilder.in("timeunit", determineHours(startTime, endTime))).and(QueryBuilder.eq("type", "MessageName"));
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
