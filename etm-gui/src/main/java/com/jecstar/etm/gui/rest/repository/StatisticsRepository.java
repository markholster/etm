package com.jecstar.etm.gui.rest.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface StatisticsRepository {

	
	public Map<String, Map<Long, Average>> getTransactionPerformanceStatistics(Long startTime, Long endTime, int maxTransactions, TimeUnit timeUnit);

	public Map<String, Map<Long, Average>> getMessagesPerformanceStatistics(Long startTime, Long endTime, int maxMessages, TimeUnit timeUnit);
	
	public Map<String, Map<Long, Long>> getApplicationMessagesCountStatistics(String application, Long startTime, Long endTime, TimeUnit timeUnit);
	
	public Map<String, Map<Long, Average>> getApplicationMessagesPerformanceStatistics(String application, Long startTime, Long endTime, TimeUnit timeUnit);
	
	public Map<String, Map<Long, Long>> getApplicationMessageNamesStatistics(String application, Long startTime, Long endTime, TimeUnit timeUnit);
	
	public List<ExpiredMessage> getApplicationMessagesExpirationStatistics(String application, Long startTime, Long endTime, int maxExpirations);
	
	public List<ExpiredMessage> getMessagesExpirationStatistics(Long startTime, Long endTime, int maxExpirations);
	
	public Map<String, Map<String, Long>> getApplicationCountStatistics(Long startTime, Long endTime, int maxApplications);
	
}
