package com.jecstar.etm.gui.rest.repository.elastic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.avg.AvgBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.gui.rest.TelemetryEventConverterTagsJsonImpl;
import com.jecstar.etm.gui.rest.repository.Average;
import com.jecstar.etm.gui.rest.repository.ExpiredMessage;
import com.jecstar.etm.gui.rest.repository.StatisticsRepository;

public class StatisticsRepositoryElasticImpl implements StatisticsRepository {

	private final String eventIndex = "etm_event_all";
	private final TelemetryEventConverterTagsJsonImpl tags = new TelemetryEventConverterTagsJsonImpl();
	private final Client elasticClient;
	
	public StatisticsRepositoryElasticImpl(Client elasticClient) {
		this.elasticClient = elasticClient;
	}
	
	public Map<String, Map<Long, Average>> getTransactionPerformanceStatistics(Long startTime, Long endTime, int maxTransactions, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		final String distinctTransactionsAggregation = "distinct_transactions";
		final String dateIntervalAggregation = "date_interval";
		final String avgResponsetimeAggregation = "avg_responsetime";
		
		AvgBuilder avgBuilder = AggregationBuilders.avg(avgResponsetimeAggregation).field(this.tags.getResponsetimeTag());
		DateHistogramBuilder dateHistogramBuilder = AggregationBuilders.dateHistogram(dateIntervalAggregation).field(this.tags.getCreationTimeTag()).interval(timeUnit.toMillis(1)).subAggregation(avgBuilder);
		TermsBuilder termsBuilder = AggregationBuilders.terms(distinctTransactionsAggregation).field(this.tags.getTransactionNameTag()).subAggregation(dateHistogramBuilder);
		
		FilterBuilder filterBuilder = FilterBuilders.andFilter(FilterBuilders.rangeFilter(this.tags.getCreationTimeTag()).from(startTime).to(endTime), 
				FilterBuilders.existsFilter(this.tags.getResponsetimeTag()));
		
		SearchResponse searchResponse = this.elasticClient.prepareSearch(this.eventIndex)
			.setSearchType(SearchType.COUNT)
			.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder))
			.addAggregation(termsBuilder)
			.get();
		
		final Map<String, Float> highest = new HashMap<String, Float>();
		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
		Terms terms = searchResponse.getAggregations().get(distinctTransactionsAggregation);
		List<Terms.Bucket> transactionBuckets = terms.getBuckets();
		for (Terms.Bucket transactionBucket : transactionBuckets) {
			String transactionName = transactionBucket.getKey();
			DateHistogram dateHistogram = transactionBucket.getAggregations().get(dateIntervalAggregation);
			List<? extends DateHistogram.Bucket> dateHistogramBuckets = dateHistogram.getBuckets();
			Map<Long, Average> averages = new HashMap<Long, Average>();
			for (DateHistogram.Bucket dateHistogramBucket : dateHistogramBuckets) {
				long time = dateHistogramBucket.getKeyAsNumber().longValue();
				long count = dateHistogramBucket.getDocCount();
				Avg avg = dateHistogramBucket.getAggregations().get(avgResponsetimeAggregation);
				float average = new Double(avg.getValue()).floatValue();
				averages.put(time, new Average(average, count));
				storeWhenHighest(highest, transactionName, average);
			}
			data.put(transactionName, averages);
		}
		filterAveragesToMaxResults(maxTransactions, highest, data);
		return data;
    }
	
	public Map<String, Map<Long, Average>> getMessagesPerformanceStatistics(Long startTime, Long endTime, int maxMessages, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		final String distinctMessagesAggregation = "distinct_messages";
		final String dateIntervalAggregation = "date_interval";
		final String avgResponsetimeAggregation = "avg_responsetime";
		
		AvgBuilder avgBuilder = AggregationBuilders.avg(avgResponsetimeAggregation).field(this.tags.getResponsetimeTag());
		DateHistogramBuilder dateHistogramBuilder = AggregationBuilders.dateHistogram(dateIntervalAggregation).field(this.tags.getCreationTimeTag()).interval(timeUnit.toMillis(1)).subAggregation(avgBuilder);
		TermsBuilder termsBuilder = AggregationBuilders.terms(distinctMessagesAggregation).field(this.tags.getNameTag()).subAggregation(dateHistogramBuilder);
		
		FilterBuilder filterBuilder = FilterBuilders.andFilter(FilterBuilders.rangeFilter(this.tags.getCreationTimeTag()).from(startTime).to(endTime), 
				FilterBuilders.existsFilter(this.tags.getResponsetimeTag()));
		
		SearchResponse searchResponse = this.elasticClient.prepareSearch(this.eventIndex)
			.setSearchType(SearchType.COUNT)
			.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder))
			.addAggregation(termsBuilder)
			.get();
		
		final Map<String, Float> highest = new HashMap<String, Float>();
		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
		Terms terms = searchResponse.getAggregations().get(distinctMessagesAggregation);
		List<Terms.Bucket> transactionBuckets = terms.getBuckets();
		for (Terms.Bucket transactionBucket : transactionBuckets) {
			String eventName = transactionBucket.getKey();
			DateHistogram dateHistogram = transactionBucket.getAggregations().get(dateIntervalAggregation);
			List<? extends DateHistogram.Bucket> dateHistogramBuckets = dateHistogram.getBuckets();
			Map<Long, Average> averages = new HashMap<Long, Average>();
			for (DateHistogram.Bucket dateHistogramBucket : dateHistogramBuckets) {
				long time = dateHistogramBucket.getKeyAsNumber().longValue();
				long count = dateHistogramBucket.getDocCount();
				Avg avg = dateHistogramBucket.getAggregations().get(avgResponsetimeAggregation);
				float average = new Double(avg.getValue()).floatValue();
				averages.put(time, new Average(average, count));
				storeWhenHighest(highest, eventName, average);
			}
			data.put(eventName, averages);
		}
		filterAveragesToMaxResults(maxMessages, highest, data);
		return data;

    }
	
	public Map<String, Map<Long, Long>> getApplicationMessagesCountStatistics(String application, Long startTime, Long endTime, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		
		final String dateIntervalAggregation = "date_interval";
		final String distinctDirectionAggregation = "distinct_directions";
		final String distinctTypeAggregation = "distinct_type";

		
		TermsBuilder typeTermsBuilder = AggregationBuilders.terms(distinctDirectionAggregation).field(this.tags.getTypeTag());
		TermsBuilder directionTermsBuilder = AggregationBuilders.terms(distinctDirectionAggregation).field(this.tags.getDirectionTag()).subAggregation(typeTermsBuilder);
		DateHistogramBuilder dateHistogramBuilder = AggregationBuilders.dateHistogram(dateIntervalAggregation).field(this.tags.getCreationTimeTag()).interval(timeUnit.toMillis(1)).subAggregation(directionTermsBuilder);	

		AndFilterBuilder filterBuilder = FilterBuilders.andFilter(
				FilterBuilders.rangeFilter(this.tags.getCreationTimeTag()).from(startTime).to(endTime),
				FilterBuilders.termFilter(this.tags.getApplicationTag(), application)
		);		
		SearchResponse searchResponse = this.elasticClient.prepareSearch(this.eventIndex)
				.setSearchType(SearchType.COUNT)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder))
				.addAggregation(dateHistogramBuilder)
				.get();
		
		final Map<String, Map<Long, Long>> data = new HashMap<String, Map<Long, Long>>();
		DateHistogram dateHistogram = searchResponse.getAggregations().get(dateIntervalAggregation);
		List<? extends DateHistogram.Bucket> dateHistogramBuckets = dateHistogram.getBuckets();
		for (DateHistogram.Bucket dateHistogramBucket : dateHistogramBuckets) {
			long time = dateHistogramBucket.getKeyAsNumber().longValue();
			Terms directionTerms = dateHistogramBucket.getAggregations().get(distinctDirectionAggregation);
			for (Terms.Bucket directionBucket : directionTerms.getBuckets()) {
				TelemetryEventDirection direction = TelemetryEventDirection.valueOf(directionBucket.getKey());
				Terms typeTerms = directionBucket.getAggregations().get(distinctTypeAggregation);
				for (Terms.Bucket typeBucket : typeTerms.getBuckets()) {
					TelemetryEventType type = TelemetryEventType.valueOf(typeBucket.getKey());
					addToTimeBasedCounterDataMap(data, directionToString(direction) + " " + typeToString(type) + " messages", time, typeBucket.getDocCount());
				}
			}
		}
	    return data;
    }
	
	private String directionToString(TelemetryEventDirection direction) {
		switch (direction) {
		case INCOMING:
			return "Incoming";
		case OUTGOING:
			return "Outgoing";
		default:
			return direction.name().toLowerCase();
		}
	}
	
	private String typeToString(TelemetryEventType type) {
		switch (type) {
		case MESSAGE_DATAGRAM:
			return "datagram";
		case MESSAGE_REQUEST:
			return "request";
		case MESSAGE_RESPONSE:
			return "response";
		default:
			return type.name().toLowerCase();
		}
	}
	
	public Map<String, Map<Long, Average>> getApplicationMessagesPerformanceStatistics(String application, Long startTime, Long endTime, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		if (application == null) {
			return Collections.emptyMap();
		}
		
		final String distinctMessagesAggregation = "distinct_messages";
		final String dateIntervalAggregation = "date_interval";
		final String avgResponsetimeAggregation = "avg_responsetime";
		
		AvgBuilder avgBuilder = AggregationBuilders.avg(avgResponsetimeAggregation).field(this.tags.getResponsetimeTag());
		DateHistogramBuilder dateHistogramBuilder = AggregationBuilders.dateHistogram(dateIntervalAggregation).field(this.tags.getCreationTimeTag()).interval(timeUnit.toMillis(1)).subAggregation(avgBuilder);
		TermsBuilder termsBuilder = AggregationBuilders.terms(distinctMessagesAggregation).field(this.tags.getNameTag()).subAggregation(dateHistogramBuilder);
	
		FilterBuilder filterBuilder = FilterBuilders.andFilter(
				FilterBuilders.rangeFilter(this.tags.getCreationTimeTag()).from(startTime).to(endTime),
				FilterBuilders.termFilter(this.tags.getApplicationTag(), application),
				FilterBuilders.existsFilter(this.tags.getResponsetimeTag())
		);
				
		SearchResponse searchResponse = this.elasticClient.prepareSearch(this.eventIndex)
				.setSearchType(SearchType.COUNT)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder))
				.addAggregation(termsBuilder)
				.get();

		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
		
		Terms terms = searchResponse.getAggregations().get(distinctMessagesAggregation);
		List<Terms.Bucket> messageBuckets = terms.getBuckets();
		for (Terms.Bucket messageBucket : messageBuckets) {
			String eventName = messageBucket.getKey();
			DateHistogram dateHistogram = messageBucket.getAggregations().get(dateIntervalAggregation);
			List<? extends DateHistogram.Bucket> dateHistogramBuckets = dateHistogram.getBuckets();
			Map<Long, Average> averages = new HashMap<Long, Average>();
			for (DateHistogram.Bucket dateHistogramBucket : dateHistogramBuckets) {
				long time = dateHistogramBucket.getKeyAsNumber().longValue();
				long count = dateHistogramBucket.getDocCount();
				Avg avg = dateHistogramBucket.getAggregations().get(avgResponsetimeAggregation);
				float average = new Double(avg.getValue()).floatValue();
				averages.put(time, new Average(average, count));
			}
			data.put(eventName, averages);
		}
		return data;
    }
	
	public Map<String, Map<Long, Long>> getApplicationMessageNamesStatistics(String application, Long startTime, Long endTime, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		
		final String distinctMessagesAggregation = "distinct_messages";
		final String dateIntervalAggregation = "date_interval";
		
		DateHistogramBuilder dateHistogramBuilder = AggregationBuilders.dateHistogram(dateIntervalAggregation).field(this.tags.getCreationTimeTag()).interval(timeUnit.toMillis(1));
		TermsBuilder termsBuilder = AggregationBuilders.terms(distinctMessagesAggregation).field(this.tags.getNameTag()).subAggregation(dateHistogramBuilder);

		
		FilterBuilder filterBuilder = FilterBuilders.andFilter(
				FilterBuilders.rangeFilter(this.tags.getCreationTimeTag()).from(startTime).to(endTime),
				FilterBuilders.termFilter(this.tags.getApplicationTag(), application)
		);		
		SearchResponse searchResponse = this.elasticClient.prepareSearch(this.eventIndex)
				.setSearchType(SearchType.COUNT)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder))
				.addAggregation(termsBuilder)
				.get();

		final Map<String, Map<Long, Long>> data = new HashMap<String, Map<Long, Long>>();
		
		Terms terms = searchResponse.getAggregations().get(distinctMessagesAggregation);
		List<Terms.Bucket> messageBuckets = terms.getBuckets();
		for (Terms.Bucket messageBucket : messageBuckets) {
			String eventName = messageBucket.getKey();
			DateHistogram dateHistogram = messageBucket.getAggregations().get(dateIntervalAggregation);
			List<? extends DateHistogram.Bucket> dateHistogramBuckets = dateHistogram.getBuckets();
			Map<Long, Long> counts = new HashMap<Long, Long>();
			for (DateHistogram.Bucket dateHistogramBucket : dateHistogramBuckets) {
				long time = dateHistogramBucket.getKeyAsNumber().longValue();
				long count = dateHistogramBucket.getDocCount();
				counts.put(time, count);
			}
			data.put(eventName, counts);
		}
		return data;		
    }
	
	public List<ExpiredMessage> getApplicationMessagesExpirationStatistics(String application, Long startTime, Long endTime, int maxExpirations) {
		if (application == null) {
			return Collections.emptyList();
		}
		return getExpiredMessages(application, startTime, endTime, maxExpirations);
    }
	
	public List<ExpiredMessage> getMessagesExpirationStatistics(Long startTime, Long endTime, int maxExpirations) {
		return getExpiredMessages(null, startTime, endTime, maxExpirations);
    }
	
	private List<ExpiredMessage> getExpiredMessages(String application, Long startTime, Long endTime, int maxExpirations) {
		if (startTime > endTime) {
			return Collections.emptyList();
		}
		AndFilterBuilder filterBuilder = FilterBuilders.andFilter(
				// The timeframe of the expired messages. This should always be in the past for this query to work.
				FilterBuilders.rangeFilter(this.tags.getExpiryTimeTag()).from(startTime).to(endTime),
				// Filter expired messages, or messages without a responsetime (and hence no response is logged).
				FilterBuilders.orFilter(FilterBuilders.termFilter("expired", true), FilterBuilders.notFilter(FilterBuilders.existsFilter(this.tags.getResponsetimeTag())))
				
		);
		if (application != null) {
			filterBuilder.add(FilterBuilders.termFilter(this.tags.getApplicationTag(), application));
		}
		
		SearchResponse searchResponse = this.elasticClient.prepareSearch(this.eventIndex)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder))
				.addSort(this.tags.getExpiryTimeTag(), SortOrder.DESC)
				.addField(this.tags.getNameTag())
				.addField(this.tags.getCreationTimeTag())
				.addField(this.tags.getExpiryTimeTag())
				.addField(this.tags.getApplicationTag())
				.setSize(maxExpirations)
				.get();

		List<ExpiredMessage> expiredMessages =  new ArrayList<ExpiredMessage>();
		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			Map<String, SearchHitField> fields = searchHit.getFields();
			String id = searchHit.getId();
			String eventName = "?";
			if (fields.containsKey(this.tags.getNameTag())) {
				eventName = fields.get(this.tags.getNameTag()).getValue();
			}
			long time = fields.get(this.tags.getCreationTimeTag()).getValue();
			Date creationTime = new Date(time);
			time = fields.get(this.tags.getExpiryTimeTag()).getValue();
			Date expiryTime = new Date(time);
			String applicationName = null;
			if (fields.containsKey(this.tags.getApplicationTag())) {
				applicationName = fields.get(this.tags.getApplicationTag()).getValue();
			}
			expiredMessages.add(new ExpiredMessage(id, eventName, creationTime, expiryTime, applicationName));
		
		}
		return expiredMessages;
	}
	
	public Map<String, Map<String, Long>> getApplicationCountStatistics(Long startTime, Long endTime, int maxApplications) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		final String distinctApplicationAggregation = "distinct_applications";
		final String distinctDirectionAggregation = "distinct_directions";
		final String distinctTypeAggregation = "distinct_type";

		
		TermsBuilder typeTermsBuilder = AggregationBuilders.terms(distinctTypeAggregation).field(this.tags.getTypeTag());
		TermsBuilder directionTermsBuilder = AggregationBuilders.terms(distinctDirectionAggregation).field(this.tags.getDirectionTag()).subAggregation(typeTermsBuilder);
		TermsBuilder applicationTermsBuilder = AggregationBuilders.terms(distinctApplicationAggregation).field(this.tags.getApplicationTag()).subAggregation(directionTermsBuilder);

		SearchResponse searchResponse = this.elasticClient.prepareSearch(this.eventIndex)
				.setSearchType(SearchType.COUNT)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), FilterBuilders.rangeFilter(this.tags.getCreationTimeTag()).from(startTime).to(endTime)))
				.addAggregation(applicationTermsBuilder)
				.get();
		
		final Map<String, Long> totals = new HashMap<String, Long>();
		final Map<String, Map<String, Long>> data = new HashMap<String, Map<String, Long>>();
		
		Terms applicationTerms = searchResponse.getAggregations().get(distinctApplicationAggregation);
		List<Terms.Bucket> applicationBuckets = applicationTerms.getBuckets();
		for (Terms.Bucket applicationBucket : applicationBuckets) {
			String applicationName = applicationBucket.getKey();
			Terms directionTerms = applicationBucket.getAggregations().get(distinctDirectionAggregation);
			Map<String, Long> appTotals = new HashMap<String, Long>();
			for (Terms.Bucket directionBucket : directionTerms.getBuckets()) {
				TelemetryEventDirection direction = TelemetryEventDirection.valueOf(directionBucket.getKey());
				Terms typeTerms = directionBucket.getAggregations().get(distinctTypeAggregation);
				for (Terms.Bucket typeBucket : typeTerms.getBuckets()) {
					TelemetryEventType type = TelemetryEventType.valueOf(typeBucket.getKey());
					appTotals.put(directionToString(direction) + " " + typeToString(type) + " messages", typeBucket.getDocCount());
					
				}
			}
			totals.put(applicationName, applicationBucket.getDocCount());
			data.put(applicationName, appTotals);
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
	
	private void filterAveragesToMaxResults(int maxResults, Map<String, Float> highest, Map<String, Map<Long, Average>> data) {
		List<Float> values = new ArrayList<>(highest.values().size());
		values.addAll(highest.values());
		Collections.sort(values);
		Collections.reverse(values);
		if (values.size() > maxResults) {
			for (int i = maxResults; i < values.size(); i++) {
				Float valueToRemove = values.get(i);
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
	
	private void addToTimeBasedCounterDataMap(Map<String, Map<Long, Long>> data, String key, Long timeUnitValue, Long count) {
		if (count == 0) {
			return;
		}
		if (!data.containsKey(key)) {
			Map<Long, Long> values = new HashMap<Long, Long>();
			values.put(timeUnitValue, new Long(count));
			data.put(key, values);
		} else {
			Map<Long, Long> values = data.get(key);
			if (!values.containsKey(timeUnitValue)) {
				values.put(timeUnitValue, new Long(count));
			} else {
				Long currentValue = values.get(timeUnitValue);
				values.put(timeUnitValue, new Long(currentValue + count));
			}
		}		
	}
		
	private void storeWhenHighest(Map<String, Float> highest, String key, float value) {
		if (!highest.containsKey(key)) {
			highest.put(key, value);
		} else {
			Float currentValue = highest.get(key);
			if (value > currentValue) {
				highest.put(key, value);
			}
		}
    }
		
}
