package com.holster.etm.gui.rest.repository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class StatisticsRepository {

	private final String keyspace = "etm";
	
	private final Session session; 
	
	public StatisticsRepository(Session session) {
		this.session = session;
	}
	
	public Map<String, Map<Long, Average>> getTransactionPerformanceStatistics(Long startTime, Long endTime, int maxTransactions, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<String> transactionNames = getTransactionNames(startTime, endTime);
		if (transactionNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> highest = new HashMap<String, Long>();
		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
		for (String transactionName : transactionNames) {
			BuiltStatement builtStatement = QueryBuilder.select("transactionName", "startTime", "finishTime", "expiryTime")
					.from(this.keyspace, "transaction_performance")
					.where(QueryBuilder.eq("transactionName_timeunit", transactionName))
					.and(QueryBuilder.gte("startTime", new Date(startTime))).and(QueryBuilder.lte("startTime", new Date(endTime)));
			resultSets.add(this.session.executeAsync(builtStatement));
		}
		for (ResultSetFuture resultSetFuture : resultSets) {
			ResultSet resultSet = resultSetFuture.getUninterruptibly();
			Iterator<Row> iterator = resultSet.iterator();
			while (iterator.hasNext()) {
				Row row = iterator.next();
				String transactionName = row.getString(0);
				if (transactionName == null) {
					continue;
				}
				Date transactionStartTime = row.getDate(1);
				Date transactionFinishTime = row.getDate(2);
				Date transactionExpiryTime = row.getDate(3);
				long timeUnitValue = normalizeTime(transactionStartTime.getTime(), timeUnit.toMillis(1));
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
					values.put(timeUnitValue, new Average(responseTime));
					data.put(transactionName, values);
				} else {
					Map<Long, Average> values = data.get(transactionName);
					if (!values.containsKey(timeUnitValue)) {
						values.put(timeUnitValue, new Average(responseTime));
					} else {
						Average currentValue = values.get(timeUnitValue);
						currentValue.add(responseTime);
					}
				}
			}
		}
		filterAveragesToMaxResults(maxTransactions, highest, data);
		return data;
    }

	public Map<String, Map<Long, Average>> getMessagesPerformanceStatistics(Long startTime, Long endTime, int maxMessages, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<String> messageNames = getMessageNames(startTime, endTime);
		if (messageNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> highest = new HashMap<String, Long>();
		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
		for (String messageName : messageNames) {
			BuiltStatement builtStatement = QueryBuilder.select("name", "startTime", "finishTime", "expiryTime")
					.from(this.keyspace, "message_performance")
					.where(QueryBuilder.eq("name_timeunit", messageName))
					.and(QueryBuilder.gte("startTime", new Date(startTime))).and(QueryBuilder.lte("startTime", new Date(endTime)));
			resultSets.add(this.session.executeAsync(builtStatement));
		}
		for (ResultSetFuture resultSetFuture : resultSets) {
			ResultSet resultSet = resultSetFuture.getUninterruptibly();
			Iterator<Row> iterator = resultSet.iterator();
			while (iterator.hasNext()) {
				Row row = iterator.next();
				String messageName = row.getString(0);
				if (messageName == null) {
					continue;
				}
				Date messageStartTime = row.getDate(1);
				Date messageFinishTime = row.getDate(2);
				Date messageExpiryTime = row.getDate(3);
				long timeUnitValue = normalizeTime(messageStartTime.getTime(), timeUnit.toMillis(1));
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
					values.put(timeUnitValue, new Average(responseTime));
					data.put(messageName, values);
				} else {
					Map<Long, Average> values = data.get(messageName);
					if (!values.containsKey(timeUnitValue)) {
						values.put(timeUnitValue, new Average(responseTime));
					} else {
						Average currentValue = values.get(timeUnitValue);
						currentValue.add(responseTime);
					}
				}
			}		
		}
		filterAveragesToMaxResults(maxMessages, highest, data);
		return data;
    }
	
	public List<ExpiredMessage> getMessagesExpirationStatistics(Long startTime, Long endTime, int maxExpirations) {
		if (startTime > endTime) {
			return Collections.emptyList();
		}
		List<String> messageNames = getMessageNames(startTime, endTime);
		if (messageNames.size() == 0) {
			return Collections.emptyList();
		}
		List<ExpiredMessage> expiredMessages =  new ArrayList<ExpiredMessage>();
		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
		for (String messageName : messageNames) {
			BuiltStatement builtStatement = QueryBuilder.select("name", "startTime", "finishTime", "expiryTime", "application")
					.from(this.keyspace, "message_expiration")
					.where(QueryBuilder.eq("name_timeunit", messageName))
					.and(QueryBuilder.gte("expiryTime", new Date(startTime))).and(QueryBuilder.lte("expiryTime", new Date(endTime)));
			resultSets.add(this.session.executeAsync(builtStatement));
		}
		for (ResultSetFuture resultSetFuture : resultSets) {
			ResultSet resultSet = resultSetFuture.getUninterruptibly();
			Iterator<Row> iterator = resultSet.iterator();
			while (iterator.hasNext()) {
				Row row = iterator.next();
				String messageName = row.getString(0);
				if (messageName == null) {
					continue;
				}
				Date messageStartTime = row.getDate(1);
				Date messageFinishTime = row.getDate(2);
				Date messageExpiryTime = row.getDate(3);
				String application = row.getString(4);
				if (messageExpiryTime != null && messageExpiryTime.getTime() > 0) {
					if ((messageFinishTime == null || messageFinishTime.getTime() == 0) && new Date().after(messageExpiryTime)) {
						synchronized (expiredMessages) {
							expiredMessages.add(new ExpiredMessage(messageName, messageStartTime, messageExpiryTime, application));
                        }
					} else if (messageFinishTime != null && messageFinishTime.getTime() > 0 && messageFinishTime.after(messageExpiryTime)) {
						synchronized (expiredMessages) {
							expiredMessages.add(new ExpiredMessage(messageName, messageStartTime, messageExpiryTime, application));
                        }
					}
				}
			}
		}
		return expiredMessages.stream().sorted((e1, e2) -> e2.getExpirationTime().compareTo(e1.getExpirationTime())).limit(maxExpirations).collect(Collectors.toList());
    }
	
	public Map<String, Map<String, Long>> getApplicationCountStatistics(Long startTime, Long endTime, int maxApplications) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<Object> applicationNames = getApplicationNames(startTime, endTime);
		if (applicationNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> totals = new HashMap<String, Long>();
		final Map<String, Map<String, Long>> data = new HashMap<String, Map<String, Long>>();
		
		BuiltStatement builtStatement = QueryBuilder.select("application", "incomingMessageRequestCount", "incomingMessageDatagramCount", "outgoingMessageRequestCount", "outgoingMessageDatagramCount")
				.from(this.keyspace, "application_counter")
				.where(QueryBuilder.in("application_timeunit", applicationNames))
				.and(QueryBuilder.gte("timeunit", new Date(startTime))).and(QueryBuilder.lte("timeunit", new Date(endTime)));
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String applicationName = row.getString(0);
			if (applicationName == null) {
				continue;
			}
			long incomingMessageRequestCount = row.getLong(1);
			long incomingMessageDatagramCount = row.getLong(2);
			long outgoingMessageRequestCount = row.getLong(3);
			long outgoingMessageDatagramCount = row.getLong(4);
			long total = incomingMessageRequestCount + incomingMessageDatagramCount + outgoingMessageRequestCount + outgoingMessageDatagramCount;
			if (total == 0) {
				continue;
			}
			if (!totals.containsKey(applicationName)) {
				totals.put(applicationName, total);
			} else {
				Long currentValue = totals.get(applicationName);
				totals.put(applicationName, currentValue + total);
			}
			if (!data.containsKey(applicationName)) {
				Map<String, Long> appTotals = new HashMap<String, Long>();
				appTotals.put("incomingMessageRequest", incomingMessageRequestCount);
				appTotals.put("incomingDatagramRequest", incomingMessageDatagramCount);
				appTotals.put("outgoingMessageRequest", outgoingMessageRequestCount);
				appTotals.put("outgoingDatagramRequest", outgoingMessageDatagramCount);
				data.put(applicationName, appTotals);
			} else {
				Map<String, Long> currentValues = data.get(applicationName);
				currentValues.put("incomingMessageRequest", currentValues.get("incomingMessageRequest") + incomingMessageRequestCount);
				currentValues.put("incomingDatagramRequest", currentValues.get("incomingDatagramRequest") + incomingMessageDatagramCount);
				currentValues.put("outgoingMessageRequest", currentValues.get("outgoingMessageRequest") + outgoingMessageRequestCount);
				currentValues.put("outgoingDatagramRequest", currentValues.get("outgoingDatagramRequest") + outgoingMessageDatagramCount);				
			}
		}
		filterCountsToMaxResults(maxApplications, totals, data);
		return data;
    }
	
	private void filterCountsToMaxResults(int maxResults, Map<String, Long> totals, Map<String, Map<String, Long>> data) {
		List<Long> values = new ArrayList<>(totals.values().size());
		values.addAll(totals.values());
		Collections.sort(values);
		Collections.reverse(values);
		if (values.size() > maxResults) {
			for (int i = maxResults; i < values.size(); i++) {
				Long valueToRemove = values.get(i);
				for (String name : totals.keySet()) {
					if (totals.get(name).equals(valueToRemove)) {
						data.remove(name);
						totals.remove(name);
						break;
					}
				}
			}
		}
	}
	
	private void filterAveragesToMaxResults(int maxResults, Map<String, Long> highest, Map<String, Map<Long, Average>> data) {
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
	
	private List<String> getTransactionNames(long startTime, long endTime) {
		BuiltStatement builtStatement = QueryBuilder.select("name").from(this.keyspace , "event_occurrences").where(QueryBuilder.in("timeunit", determineHours(startTime, endTime))).and(QueryBuilder.eq("type", "TransactionName"));
		List<String> transactionNames = new ArrayList<String>();
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
	
	private List<String> getMessageNames(long startTime, long endTime) {
		BuiltStatement builtStatement = QueryBuilder.select("name").from(this.keyspace , "event_occurrences").where(QueryBuilder.in("timeunit", determineHours(startTime, endTime))).and(QueryBuilder.eq("type", "MessageName"));
		List<String> eventNames = new ArrayList<String>();
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
	
	private List<Object> getApplicationNames(long startTime, long endTime) {
		BuiltStatement builtStatement = QueryBuilder.select("name").from(this.keyspace , "event_occurrences").where(QueryBuilder.in("timeunit", determineHours(startTime, endTime))).and(QueryBuilder.eq("type", "Application"));
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
	    	result.add(new Date(normalizeTime(startCalendar.getTime().getTime(), TimeUnit.HOURS.toMillis(1))));
	    	startCalendar.add(Calendar.HOUR, 1);
	    } while (startCalendar.before(endCalendar) || (!startCalendar.before(endCalendar) && startCalendar.get(Calendar.HOUR_OF_DAY) == endCalendar.get(Calendar.HOUR_OF_DAY)));
	    return result;
    }

	private long normalizeTime(long timeInMillis, long factor) {
		return (timeInMillis / factor) * factor;
    }
}
