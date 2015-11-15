package com.jecstar.etm.gui.rest.repository.elastic;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.core.util.ObjectUtils;
import com.jecstar.etm.gui.rest.TelemetryEventConverterTagsJsonImpl;
import com.jecstar.etm.gui.rest.repository.CorrelationData;
import com.jecstar.etm.gui.rest.repository.OverviewEvent;
import com.jecstar.etm.gui.rest.repository.QueryRepository;
import com.jecstar.etm.gui.rest.repository.ResponseCorrelationData;

import net.sf.saxon.TransformerFactoryImpl;

public class QueryRepositoryElasticImpl implements QueryRepository {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(QueryRepositoryElasticImpl.class);
	
	private final String eventIndex = "etm_event_all";
	private final String eventIndexType = "event";
	
	private final Client elasticClient;
	private final TransformerFactoryImpl transformerFactory;
	private final TelemetryEventConverterTagsJsonImpl tags = new TelemetryEventConverterTagsJsonImpl();
	private final long dataCorrelationTimeOffset = 30000;
	private final int dataCorrelationMaxResults = 500;

	public QueryRepositoryElasticImpl(Client elasticClient) {
		this.elasticClient = elasticClient;
		this.transformerFactory = new TransformerFactoryImpl();
	}

	public void addEvents(SearchHits results, JsonGenerator generator) throws JsonGenerationException, IOException {
		for (SearchHit hit : results.getHits()) {
			Map<String, Object> valueMap = hit.getSource();
			generator.writeStartObject();
			writeStringValue("application", getStringValue(this.tags.getApplicationTag(), valueMap), generator);
			writeStringValue("correlationId", getStringValue(this.tags.getCorrelationIdTag(), valueMap), generator);
			writeDateValue("creationTime", getDateValue(this.tags.getCreationTimeTag(), valueMap), generator);
			writeStringValue("endpoint", getStringValue(this.tags.getEndpointTag(), valueMap), generator);
			writeStringValue("id", getStringValue(this.tags.getIdTag(), valueMap), generator);
			writeStringValue("name", getStringValue(this.tags.getNameTag(), valueMap), generator);
			generator.writeEndObject();
		}
	}

	public void addEvent(String eventId, JsonGenerator generator) throws JsonGenerationException, IOException {
		GetResponse getResponse = this.elasticClient.prepareGet(this.eventIndex, this.eventIndexType, eventId).get();
		if (!getResponse.isExists()) {
			return;
		}
		Map<String, Object> valueMap = getResponse.getSourceAsMap();
		writeStringValue("application", getStringValue(this.tags.getApplicationTag(), valueMap), generator);
		writeStringValue("content", formatXml(getStringValue(this.tags.getContentTag(), valueMap)), generator);
		writeStringValue("correlationId", getStringValue(this.tags.getCorrelationIdTag(), valueMap), generator);
		generator.writeArrayFieldStart("childCorrelationIds");
		List<String> childCorrelationIds = getStringListValue(this.tags.getChildCorrelationIdsTag(), valueMap);
		for (String childCorrelationId : childCorrelationIds) {
			generator.writeString(childCorrelationId);
		}
		generator.writeEndArray();
		writeDateValue("creationTime", getDateValue(this.tags.getCreationTimeTag(), valueMap), generator);
		writeStringValue("direction", getStringValue(this.tags.getDirectionTag(), valueMap), generator);
		writeStringValue("endpoint", getStringValue(this.tags.getEndpointTag(), valueMap), generator);
		writeDateValue("expiryTime", getDateValue(this.tags.getExpiryTimeTag(), valueMap), generator);
		writeStringValue("id", eventId, generator);
		writeStringValue("name", getStringValue(this.tags.getNameTag(), valueMap), generator);
		writeStringValue("transactionId", getStringValue(this.tags.getTransactionIdTag(), valueMap), generator);
		writeStringValue("transactionName", getStringValue(this.tags.getTransactionNameTag(), valueMap), generator);
		writeStringValue("type", getStringValue(this.tags.getTypeTag(), valueMap), generator);
	}
	
	private String formatXml(String unformattedXml) {
		if (unformattedXml != null && unformattedXml.length() > 0 && unformattedXml.startsWith("<")) {
			try {
			Transformer transformer = this.transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			StreamResult result = new StreamResult(new StringWriter());
			transformer.transform(new StreamSource(new StringReader(unformattedXml)), result);
			return result.getWriter().toString();
			} catch (Exception e) {
				return unformattedXml;
			}
		}
		return unformattedXml;
	}

	public void addEventOverview(String eventId, JsonGenerator generator) throws JsonGenerationException, IOException {
		String rootEventId = findRootEventId(eventId, new ArrayList<String>());
		OverviewEvent tree = new OverviewEvent();
		findChildren(rootEventId, tree, new ArrayList<OverviewEvent>());
		OverviewEvent root = tree.children.get(0);
		sortTelemetryEventTree(root);
		calculateResponseTimes(root);

		long totalOverviewTime = getTotalOverviewTime(root);
		calculateFillColors(root, totalOverviewTime);

		List<String> applications = new ArrayList<String>();
		AtomicInteger eventCount = new AtomicInteger(0);
		calculateCounters(root, applications, eventCount, new AtomicInteger(0));

		List<TimeFrame> timeFrames = new ArrayList<TimeFrame>();
		calculateTimeFrames(root, timeFrames, new AtomicInteger(0), applications);

		// Create the json
		generator.writeNumberField("eventCount", eventCount.intValue());
		generator.writeNumberField("applicationCount", applications.size());
		generator.writeNumberField("timeframeCount", timeFrames.size());
		generator.writeArrayFieldStart("applications");
		for (String application : applications) {
			generator.writeString(application);
		}
		generator.writeEndArray();
		generator.writeArrayFieldStart("timeframes");
		for (TimeFrame timeFrame : timeFrames) {
			generator.writeStartArray();
			for (int i = 0; i < applications.size(); i++) {
				generator.writeStartObject();
				OverviewEvent overviewEvent = timeFrame.overviewEvents[i];
				if (overviewEvent != null) {
					generator.writeStringField("application", overviewEvent.application);
					generator.writeStringField("eventName", overviewEvent.name == null ? "?" : overviewEvent.name);
					generator.writeStringField("id", overviewEvent.id.toString());
					generator.writeStringField("endpoint", overviewEvent.endpoint);
					generator.writeStringField("color", overviewEvent.color);
					generator.writeNumberField("creationTime", overviewEvent.creationTime);
					if (overviewEvent.direction != null) {
						generator.writeStringField("direction", overviewEvent.direction.name());
					}
					if (overviewEvent.type != null) {
						generator.writeStringField("type", overviewEvent.type.name());
					}
					if (overviewEvent.responseTime != -1) {
						generator.writeNumberField("responseTime", overviewEvent.responseTime);
					}
					if (overviewEvent.absoluteResponseTime != -1) {
						generator.writeNumberField("absoluteResponseTime", overviewEvent.absoluteResponseTime);
					}
				}
				generator.writeEndObject();
			}
			generator.writeEndArray();

		}
		generator.writeEndArray();
	}

	private void sortTelemetryEventTree(OverviewEvent overviewEvent) {
		if (overviewEvent.children.size() > 0) {
			overviewEvent.children.sort(new Comparator<OverviewEvent>() {
				@Override
				public int compare(OverviewEvent o1, OverviewEvent o2) {
					// First check by time.
					if (o1.creationTime < o2.creationTime) {
						return -1;
					} else if (o1.creationTime == o2.creationTime) {
						return 1;
					}
					// if time is the same, and type is the same we stop
					// comparing.
					if (o1.type == null ^ o2.type == null || o1.type.equals(o2.type)) {
						return 0;
					}
					if (TelemetryEventType.MESSAGE_RESPONSE.equals(o1.type)) {
						// Response messages last.
						return 1;
					} else if (TelemetryEventType.MESSAGE_RESPONSE.equals(o2.type)) {
						// Response messages last
						return -1;
					}
					return 0;
				}
			});
			for (OverviewEvent childOverviewEvent : overviewEvent.children) {
				sortTelemetryEventTree(childOverviewEvent);
			}
		}
	}

	private void calculateResponseTimes(OverviewEvent overviewEvent) {
		if (TelemetryEventType.MESSAGE_REQUEST.equals(overviewEvent.type)) {
			OverviewEvent response = overviewEvent.getMessageResponseOverviewEvent();
			if (response != null) {
				long responseTime = response.creationTime - overviewEvent.creationTime;
				overviewEvent.responseTime = responseTime;
				response.responseTime = responseTime;
			} else if (overviewEvent.expirationTime > -1) {
				overviewEvent.responseTime = overviewEvent.expirationTime - overviewEvent.creationTime;
			}
		}
		for (OverviewEvent childOverviewEvent : overviewEvent.children) {
			calculateResponseTimes(childOverviewEvent);
		}
	}

	private long getTotalOverviewTime(OverviewEvent overviewEvent) {
		if (TelemetryEventType.MESSAGE_REQUEST.equals(overviewEvent.type)) {
			return overviewEvent.responseTime;
		}
		for (OverviewEvent childOverviewEvent : overviewEvent.children) {
			if (TelemetryEventType.MESSAGE_REQUEST.equals(childOverviewEvent.type)) {
				return childOverviewEvent.responseTime;
			}
		}
		for (OverviewEvent childOverviewEvent : overviewEvent.children) {
			return getTotalOverviewTime(childOverviewEvent);
		}
		return -1;

	}

	private void calculateFillColors(OverviewEvent overviewEvent, long totalOverviewTime) {
		if (TelemetryEventType.MESSAGE_REQUEST.equals(overviewEvent.type)) {
			OverviewEvent response = overviewEvent.getMessageResponseOverviewEvent();
			if (response != null) {
				long childResponseTimes = overviewEvent.children.stream().filter(c -> TelemetryEventType.MESSAGE_REQUEST.equals(c.type))
				        .mapToLong(c -> c.responseTime).sum();
				overviewEvent.absoluteResponseTime = overviewEvent.responseTime - childResponseTimes;
				response.absoluteResponseTime = overviewEvent.absoluteResponseTime;
				float redFactor = ((float) overviewEvent.absoluteResponseTime / (float) totalOverviewTime);
				if (redFactor > 1) {
					redFactor = 1;
				}
				String red = Integer.toHexString((int) (255 * redFactor));
				String green = Integer.toHexString((int) (255 * (1 - redFactor)));
				if (red.length() < 2) {
					red = "0" + red;
				}
				if (green.length() < 2) {
					green = "0" + green;
				}
				overviewEvent.color = "#" + red + green + "00";
				response.color = overviewEvent.color;
			} else if (overviewEvent.expirationTime > -1) {
				overviewEvent.color = "#ff0000";
			}
		} else if (TelemetryEventType.MESSAGE_DATAGRAM.equals(overviewEvent.type)) {
			overviewEvent.color = "#eeeeee";
		}
		for (OverviewEvent childOverviewEvent : overviewEvent.children) {
			calculateFillColors(childOverviewEvent, totalOverviewTime);
		}
	}

	private void calculateCounters(OverviewEvent overviewEvent, List<String> appNames, AtomicInteger eventCount, AtomicInteger eventDepth) {
		eventCount.incrementAndGet();
		if (!TelemetryEventType.MESSAGE_RESPONSE.equals(overviewEvent.type) && overviewEvent.application != null) {
			int appIx = appNames.lastIndexOf(overviewEvent.application);
			if (appIx == -1) {
				appNames.add(overviewEvent.application);
			} else if (appIx < eventDepth.get()) {
				appNames.add(overviewEvent.application);
			}
		}
		for (OverviewEvent childOverviewEvent : overviewEvent.children) {
			if (!ObjectUtils.equalsNullProof(overviewEvent.application, childOverviewEvent.application)) {
				eventDepth.incrementAndGet();
			}
			calculateCounters(childOverviewEvent, appNames, eventCount, eventDepth);
			if (!ObjectUtils.equalsNullProof(overviewEvent.application, childOverviewEvent.application)) {
				eventDepth.decrementAndGet();
			}
		}
	}

	private void calculateTimeFrames(OverviewEvent overviewEvent, List<TimeFrame> timeFrames, AtomicInteger eventDepth, List<String> applications) {
		if (applications.size() == 0) {
			return;
		}
		if (timeFrames.size() == 0) {
			timeFrames.add(new TimeFrame(applications.size()));
		}
		TimeFrame currentTimeFrame = timeFrames.get(timeFrames.size() - 1);
		if (currentTimeFrame.overviewEvents[eventDepth.get()] != null) {
			currentTimeFrame = new TimeFrame(applications.size());
			timeFrames.add(currentTimeFrame);
		}
		currentTimeFrame.overviewEvents[eventDepth.get()] = overviewEvent;
		for (OverviewEvent childOverviewEvent : overviewEvent.children) {
			if (childOverviewEvent.application != null && overviewEvent.application != null
			        && !overviewEvent.application.equals(childOverviewEvent.application)) {
				int addition = 0;
				for (int i = eventDepth.get() + 1; i < applications.size(); i++) {
					if (applications.get(i).equals(childOverviewEvent.application)) {
						addition = i - eventDepth.get();
						break;
					}
				}
				eventDepth.addAndGet(addition);
				calculateTimeFrames(childOverviewEvent, timeFrames, eventDepth, applications);
				eventDepth.addAndGet(-addition);
			} else {
				calculateTimeFrames(childOverviewEvent, timeFrames, eventDepth, applications);
			}
		}
	}

	private String findRootEventId(String eventId, List<String> foundElements) {
		if (!foundElements.contains(eventId)) {
			foundElements.add(eventId);
		}
		String parentId = findParentId(eventId, null);
		if (parentId == null) {
			return eventId;
		}
		if (foundElements.contains(parentId)) {
			// Cyclic dependency
			return eventId;
		}
		foundElements.add(parentId);
		return findRootEventId(parentId, foundElements);
	}
	
	private String findParentId(String eventId, Date considerdataFrom) {
		GetResponse getResponse = this.elasticClient.prepareGet(this.eventIndex, this.eventIndexType, eventId)
				.setFields(this.tags.getCorrelationIdTag())
				.get();
		if (!getResponse.isExists()) {
			return null;
		}
		String parentId = null;
		if (getResponse.getFields().containsKey(this.tags.getCorrelationIdTag())) {
			parentId = getResponse.getField(this.tags.getCorrelationIdTag()).getValue().toString();
		}
		if (parentId == null) {
			parentId = findParentIdByDataCorrelation(eventId, considerdataFrom);
		}
		return parentId;
	}
	

	/**
	 * Find a parent event by data correlation.
	 * 
	 * @param eventId
	 *            The ID of the event to find the parent from.
	 * 
	 * @param considerdataFrom
	 *            The time range to search for parents. By default events within
	 *            the range from
	 *            <code>creationTime - 30000 milliseconds</code> till
	 *            <code>creationTime</code> will be considered unless
	 *            <code>considerdataFrom</code> not is <code>null</code>. In
	 *            that case
	 *            <code>considerdataFrom</code> till <code>creationTime</code> will be
	 *            considered to find a parent.
	 * @return The ID of the parent or <code>null</code> if no parent could be
	 *         found.
	 */
	private String findParentIdByDataCorrelation(String eventId, Date considerdataFrom) {
		if (eventId == null) {
			return null;
		}
		CorrelationData correlationData = getCorrelationData(eventId);
		if (correlationData == null || correlationData.data.size() == 0) {
			return null;
		}
		Date startQueryDate = new Date(correlationData.validFrom.getTime() - this.dataCorrelationTimeOffset);
		if (considerdataFrom != null) {
			startQueryDate.setTime(considerdataFrom.getTime());
		}

		List<CorrelationResult> correlatingResults = getCorrelationResults(eventId, correlationData.data, startQueryDate,
		        correlationData.validFrom);
		if (correlatingResults.size() == 0) {
			return null;
		}
		// Sort the correlating events in order closest to the creationTime of
		// the given event.
		correlatingResults.sort(new Comparator<CorrelationResult>() {
			@Override
			public int compare(CorrelationResult o1, CorrelationResult o2) {
				if (o1.timestamp.before(o2.timestamp)) {
					return 1;
				}
				return -1;
			}
		});
		for (CorrelationResult correlationResult : correlatingResults) {
			GetResponse getResponse = this.elasticClient.prepareGet(this.eventIndex, this.eventIndexType, correlationResult.id).get();
			if (!getResponse.isExists()) {
				continue;
			}
			Map<String, Object> valueMap = getResponse.getSourceAsMap();
			if (getDateValue(this.tags.getCreationTimeTag(), valueMap).after(correlationData.validFrom)) {
				// The creationTime of this event is after the creation time of
				// the event we try to find the parent from. This can
				// impossibly be a parent.
				continue;
			}
			TelemetryEventType type = null;
			try {
				type = TelemetryEventType.valueOf(getStringValue(this.tags.getTypeTag(), valueMap));
			} catch (Exception e) {
				// Without a type we cannot securely correlate by data.
				continue;
			}
			if (TelemetryEventType.MESSAGE_DATAGRAM.equals(type)) {
				// Closest correlated event is a datagram message. It's still a
				// guess if this event is the parent event, but it's a pretty
				// good guess after all.
				return correlationResult.id;
			} else if (TelemetryEventType.MESSAGE_REQUEST.equals(type)) {
				// Check if the response of the found event is created after the
				// finish time of this event. If so, we've got a winner.
				List<String> childIds = getStringListValue(this.tags.getChildCorrelationIdsTag(), valueMap);
				if (childIds.size() == 0) {
					// A request without a response, hence the request had a
					// timeout
					Date expiryDate = getDateValue(this.tags.getExpiryTimeTag(), valueMap);
					if (expiryDate != null && !expiryDate.before(correlationData.validTill)) {
						// The expiry date from the request after the validity
						// of the correlation data -> We have a match.
						return correlationResult.id;
					} else if (expiryDate != null && correlationData.expired) {
						// The parent request expired, and this request expired
						// as well.
						// It's a guess, but these requests probably have a
						// parent-child relationship.
						return correlationResult.id;
					}
				}
				for (String childId : childIds) {
					GetResponse getChildResponse = this.elasticClient.prepareGet(this.eventIndex, this.eventIndexType, childId).get();
					if (!getChildResponse.isExists()) {
						continue;
					}
					Map<String, Object> childValueMap = getChildResponse.getSourceAsMap();
					if (correlationResult.id.equals(getStringValue(this.tags.getCorrelationIdTag(), childValueMap))
					        && TelemetryEventType.MESSAGE_RESPONSE.name().equals(getStringValue(this.tags.getTypeTag(), childValueMap))) {
						Date responseDate = getDateValue(this.tags.getCreationTimeTag(), childValueMap);
						if (!responseDate.before(correlationData.validTill)) {
							// The response creation time if not before this
							// events finish time -> It's a parent!
							return correlationResult.id;
						} else if (correlationData.expired) {
							// The parent expired, and this request is finished
							// after the parent expiry time. Sounds logical
							// we've found the parent.
							return correlationResult.id;
						}
					}
				}
			} else if (TelemetryEventType.MESSAGE_RESPONSE.equals(type)) {
				// On first sight it seems impossible that a response message is
				// a parent of any other event, but it is possible! When the
				// data of a response is used in a consecutive request the
				// relation is parent-child on data level. For example, when a
				// request is fired with a relationnr as correlation data and in
				// the response of that request a policynumber is returned as
				// correlation data. If that policynumber is used in a latter
				// request, that request is a child of the response.
				return correlationResult.id;
			}
		}
		return null;
	}

	private void findChildren(String eventId, OverviewEvent parent, List<OverviewEvent> overviewEvents) {
		GetResponse getResponse = this.elasticClient.prepareGet(this.eventIndex, this.eventIndexType, eventId).get();
		if (!getResponse.isExists()) {
			return;
		}
		Map<String, Object> valueMap = getResponse.getSourceAsMap();
		OverviewEvent overviewEvent = createOverviewEvent(valueMap);
		if (!overviewEvents.contains(overviewEvent)) {
			if (parent != null) {
				if (parent.direction != null && overviewEvent.direction != null) {
					if (parent.direction.equals(overviewEvent.direction)) {
						return;
					}
					if (parent.application != null && overviewEvent.application != null) {
						if (TelemetryEventDirection.INCOMING.equals(parent.direction)
						        && !parent.application.equals(overviewEvent.application)) {
							// If the parent event was an incoming event, the
							// child event should have the same application name
							return;
						}
					}
				}
				parent.children.add(overviewEvent);
			}
			overviewEvents.add(overviewEvent);
			List<String> children = new ArrayList<String>();
			children.addAll(getStringListValue(this.tags.getChildCorrelationIdsTag(), valueMap));
			Collection<String> childIdsByDataCorrelation = findChildIdsByDataCorrelation(eventId);
			for (String id : childIdsByDataCorrelation) {
				if (!children.contains(id)) {
					children.add(id);
				}
			}
			for (String child : children) {
				findChildren(child, overviewEvent, overviewEvents);
			}
		}
	}

	private OverviewEvent createOverviewEvent(Map<String, Object> valueMap) {
		OverviewEvent overviewEvent = new OverviewEvent();
		overviewEvent.id = getStringValue(this.tags.getIdTag(), valueMap);
		overviewEvent.creationTime = getDateValue(this.tags.getCreationTimeTag(), valueMap).getTime();
		overviewEvent.application = getStringValue(this.tags.getApplicationTag(), valueMap);
		try {
			overviewEvent.direction = TelemetryEventDirection.valueOf(getStringValue(this.tags.getDirectionTag(), valueMap));
		} catch (IllegalArgumentException | NullPointerException e) {
		}
		overviewEvent.endpoint = getStringValue(this.tags.getEndpointTag(), valueMap);
		Date date = getDateValue(this.tags.getExpiryTimeTag(), valueMap);
		if (date != null) {
			overviewEvent.expirationTime = date.getTime();
		}
		overviewEvent.name = getStringValue(this.tags.getNameTag(), valueMap);
		try {
			overviewEvent.type = TelemetryEventType.valueOf(getStringValue(this.tags.getTypeTag(), valueMap));
		} catch (IllegalArgumentException | NullPointerException e) {
		}

		return overviewEvent;
	}

	private Collection<String> findChildIdsByDataCorrelation(String eventId) {
		List<String> result = new ArrayList<String>();
		CorrelationData correlationData = getCorrelationData(eventId);
		if (correlationData == null || correlationData.data.size() == 0) {
			return result;
		}
		if (TelemetryEventType.MESSAGE_DATAGRAM.equals(correlationData.type)) {
			correlationData.validTill.setTime(correlationData.validTill.getTime() + this.dataCorrelationTimeOffset);
		}
		List<CorrelationResult> correlatingResults = getCorrelationResults(eventId, correlationData.data, correlationData.validFrom,
		        correlationData.validTill);
		if (correlatingResults.size() == 0) {
			return result;
		}
		// Sort the correlating events in order closest to the creationTime of
		// the given event.
		correlatingResults.sort(new Comparator<CorrelationResult>() {
			@Override
			public int compare(CorrelationResult o1, CorrelationResult o2) {
				if (o1.timestamp.before(o2.timestamp)) {
					return -1;
				}
				return 1;
			}
		});
		for (CorrelationResult correlationResult : correlatingResults) {
			// Make sure we return only direct children.
			if (eventId.equals(findParentIdByDataCorrelation(correlationResult.id, correlationData.validFrom))) {
				result.add(correlationResult.id);
				// Get the response correlation data and see if other request match this response data.
				ResponseCorrelationData responseCorrelationData = getResponseCorrelationData(correlationResult.id);
				findChildrenByCorrelationResults(correlationData, responseCorrelationData, result);
			}
		}
		return result;
	}
	
	private void findChildrenByCorrelationResults(CorrelationData correlationData, ResponseCorrelationData responseCorrelationData, List<String> responseList) {
		// Get the response correlation data and see if other request match this response data.
		if (responseCorrelationData != null) {
			List<CorrelationResult> responseMatchedCorrelationResults = getCorrelationResults(responseCorrelationData.id, responseCorrelationData.data, responseCorrelationData.validFrom, correlationData.validTill);
			for (CorrelationResult responseMatchedCorrelationResult : responseMatchedCorrelationResults) {
				String parent = findParentId(responseMatchedCorrelationResult.id, correlationData.validFrom);
				// The parent of an event matched by response data is the response, we need to level up to the request
				if (parent != null) {
					parent = findParentId(parent, correlationData.validFrom);
				}
				// And if that parent has a match in the responseList we've found an event matched by response data.
				if (responseList.contains(parent)) {
					if (!responseList.contains(responseMatchedCorrelationResult.id)) {
						responseList.add(responseMatchedCorrelationResult.id);
						findChildrenByCorrelationResults(correlationData, getResponseCorrelationData(responseMatchedCorrelationResult.id), responseList);
					}
				}
			}
		}
	}

	/**
	 * Find the events based on the same correlation data.
	 * 
	 * @param eventId
	 *            The ID of the event to match correlation data against.
	 * @param correlationData
	 *            The correlation data of the event with id <code>eventId</code>.
	 * @param startTime
	 *            The start time of the data to consider.
	 * @param finishTime
	 *            The start time of the data to consider.
	 * @return A list with ID's of events that have a match in data correlation.
	 */
	private List<CorrelationResult> getCorrelationResults(String eventId, Map<String, String> correlationData, Date startTime, Date finishTime) {
		List<CorrelationResult> correlatingResults = new ArrayList<CorrelationResult>();
		for (String correlationKey : correlationData.keySet()) {
			SearchResponse searchResponse = this.elasticClient.prepareSearch(this.eventIndex)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), 
						FilterBuilders.andFilter(
								FilterBuilders.termFilter(this.tags.getCorrelationDataTag() + "." + correlationKey, correlationData.get(correlationKey)),
								FilterBuilders.rangeFilter(this.tags.getCreationTimeTag()).from(startTime.getTime()).to(finishTime.getTime()))))
				.setSize(this.dataCorrelationMaxResults)
				.addField(this.tags.getIdTag())
				.addField(this.tags.getCreationTimeTag())
				.get();

			for (SearchHit searchHit : searchResponse.getHits()) {
				Map<String, SearchHitField> valueMap = searchHit.getFields();
				
				String correlationId = getStringValue(this.tags.getIdTag(), valueMap);
				if (correlationId.equals(eventId)) {
					continue;
				}
				if (correlatingResults.size() > this.dataCorrelationMaxResults) {
					if (log.isWarningLevelEnabled()) {
						log.logWarningMessage("Found more than " + this.dataCorrelationMaxResults + " data correlations. Stop looking for more results. Overview may be incorrect.");
					}
					break;
				}
				CorrelationResult correlationResult = new CorrelationResult(correlationId, getDateValue(this.tags.getCreationTimeTag(), valueMap));
				if (!correlatingResults.contains(correlationResult)) {
					correlatingResults.add(correlationResult);
				}
			}
		}
		return correlatingResults;
	}
	
	/**
	 * Check if the given <code>eventId</code> represents a
	 * {@link TelemetryEventType.MESSAGE_REQUEST} and, if so, returns the
	 * correlation data of the corresponding response.
	 * 
	 * @param eventId
	 * @return
	 */
	private ResponseCorrelationData getResponseCorrelationData(String eventId) {
		GetResponse getResponse = this.elasticClient.prepareGet(this.eventIndex, this.eventIndexType, eventId).get();
		if (!getResponse.isExists()) {
			return null;
		}
		Map<String, Object> valueMap = getResponse.getSourceAsMap();
		if (!TelemetryEventType.MESSAGE_REQUEST.name().equals(getStringValue(this.tags.getTypeTag(), valueMap))) {
			return null;
		}
		List<String> correlations = getStringListValue(this.tags.getChildCorrelationIdsTag(), valueMap);
		if (correlations == null || correlations.size() == 0) {
			return null;
		}
		for (String correlationId : correlations) {
			GetResponse getChildResponse = this.elasticClient.prepareGet(this.eventIndex, this.eventIndexType, correlationId).get();
			if (!getResponse.isExists()) {
				continue;
			}
			Map<String, Object> childValueMap = getChildResponse.getSourceAsMap();
			if (!TelemetryEventType.MESSAGE_RESPONSE.name().equals(getStringValue(this.tags.getTypeTag(), childValueMap))) {
				continue;
			}
			ResponseCorrelationData result = new ResponseCorrelationData();
			result.id = correlationId;
			result.validFrom = getDateValue(this.tags.getCreationTimeTag(), childValueMap);
			getMapValue(this.tags.getCorrelationDataTag(), childValueMap).forEach((k,v) -> result.data.put(k, v.toString()));
			return result;
		}
		return null;
	}

	private CorrelationData getCorrelationData(String eventId) {
		CorrelationData result = new CorrelationData();
		result.eventId = eventId;
		
		GetResponse getResponse = this.elasticClient.prepareGet(this.eventIndex, this.eventIndexType, eventId).get();
		if (!getResponse.isExists()) {
			return null;
		}
		Map<String, Object> valueMap = getResponse.getSourceAsMap();

		result.application = getStringValue(this.tags.getApplicationTag(), valueMap);
		getMapValue(this.tags.getCorrelationDataTag(), valueMap).forEach((k, v) -> result.data.put(k, v.toString()));
		if (result.data.size() == 0) {
			return null;
		}
		List<String> childCorrelations = getStringListValue(this.tags.getChildCorrelationIdsTag(), valueMap);
		result.validFrom = getDateValue(this.tags.getCreationTimeTag(), valueMap);
		result.validTill = new Date(result.validFrom.getTime());
		Date expiryTime = getDateValue(this.tags.getExpiryTimeTag(), valueMap);
		try {
			result.type = TelemetryEventType.valueOf(getStringValue(this.tags.getTypeTag(), valueMap));
		} catch (Exception e) {
			// Without a type we cannot securely correlate by data.
			return null;
		}
		if (TelemetryEventType.MESSAGE_RESPONSE.equals(result.type)) {
			// A response should always be correlated by it's
			// sourceCorrelationId, not by data.
		} else if (TelemetryEventType.MESSAGE_REQUEST.equals(result.type)) {
			// Initially set the scope of a request to it's expiry time.
			if (expiryTime != null && expiryTime.getTime() != 0) {
				result.validTill.setTime(expiryTime.getTime());
				result.expired = true;
			}
			for (String childId : childCorrelations) {
				GetResponse childResponse = this.elasticClient.prepareGet(this.eventIndex, this.eventIndexType, childId).get();
				if (!childResponse.isExists()) {
					continue;
				}
				Map<String, Object> childValueMap = childResponse.getSourceAsMap();
				if (eventId.equals(getStringValue(this.tags.getIdTag(), childValueMap)) && 
						TelemetryEventType.MESSAGE_RESPONSE.name().equals(getStringValue(this.tags.getTypeTag(), childValueMap))) {
					// We've found the response. Set the finishTime to the time
					// the response was created.
					Date responseCreationTime = getDateValue(this.tags.getCreationTimeTag(), childValueMap);
					if (responseCreationTime.before(result.validTill)) {
						// Response was before the expiry time.
						result.validTill.setTime(responseCreationTime.getTime());
						result.expired = false;
					}
					break;
				}
			}
		}
		return result;
	}

	private void writeStringValue(String fieldName, String fieldValue, JsonGenerator generator) throws JsonGenerationException, IOException {
		if (fieldValue == null) {
			return;
		}
		generator.writeStringField(fieldName, fieldValue);
	}

	private void writeDateValue(String fieldName, Date fieldValue, JsonGenerator generator) throws JsonGenerationException, IOException {
		if (fieldValue == null) {
			return;
		}
		generator.writeNumberField(fieldName, fieldValue.getTime());
	}
	
	
	private String getStringValue(String key, Map<String, ?> valueMap) {
		if (valueMap.containsKey(key)) {
			Object object = valueMap.get(key);
			if (object instanceof SearchHitField) {
				return ((SearchHitField)object).getValue();
			}
			return valueMap.get(key).toString();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private List<String> getStringListValue(String key, Map<String, Object> valueMap) {
		if (valueMap.containsKey(key)) {
			Object object = valueMap.get(key);
			if (object instanceof SearchHitField) {
				return ((SearchHitField)object).getValue();
			}
			return (List<String>) valueMap.get(key);
		}
		return Collections.emptyList();
	}

	
	private Date getDateValue(String key, Map<String, ?> valueMap) {
		if (valueMap.containsKey(key)) {
			Object object = valueMap.get(key);
			if (object instanceof SearchHitField) {
				long time = ((SearchHitField)object).getValue(); 
				return new Date(time);
			}
			return new Date(((Number)valueMap.get(key)).longValue());
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getMapValue(String key, Map<String, Object> valueMap) {
		if (valueMap.containsKey(key)) {
			return (Map<String, Object>) valueMap.get(key);
		}
		return Collections.emptyMap();		
	}


	private class TimeFrame {

		public OverviewEvent[] overviewEvents;

		private TimeFrame(long applicationCount) {
			this.overviewEvents = new OverviewEvent[(int) applicationCount];
		}
	}

	private class CorrelationResult {

		private String id;

		private Date timestamp;

		private CorrelationResult(String id, Date timestamp) {
			this.id = id;
			this.timestamp = timestamp;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CorrelationResult) {
				CorrelationResult other = (CorrelationResult) obj;
				return other.id.equals(this.id);
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}
	}
}
