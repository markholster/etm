package com.holster.etm.gui.rest.repository;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.holster.etm.core.TelemetryEventDirection;
import com.holster.etm.core.TelemetryEventType;
import com.holster.etm.core.cassandra.PartitionKeySuffixCreator;

public class QueryRepository {

	private static final long CORRELATION_SELECTION_OFFSET = 30_000;

	private final String keyspace = "etm";
	private final DateFormat format = new PartitionKeySuffixCreator();
	private final Session session;
	private final PreparedStatement findEventForSearchResultsStatement;
	private final PreparedStatement findEventForDetailsStatement;
	private final PreparedStatement findEventParentIdStatement;
	private final PreparedStatement findOverviewEventStatement;
	private final PreparedStatement findCorrelationDataStatement;
	private final PreparedStatement findChildEventCreationTimeStatement;
	private final PreparedStatement findCorrelationsByDataStatement;
	private final PreparedStatement findPotentialCorrelatingEventDataStatement;

	public QueryRepository(Session session) {
		this.session = session;
		this.findEventForSearchResultsStatement = this.session
		        .prepare("select application, correlationId, creationTime, endpoint, id, name, sourceCorrelationId, sourceId from "
		                + this.keyspace + ".telemetry_event where id = ?");
		this.findEventForDetailsStatement = this.session
		        .prepare("select application, content, correlationId, correlations, creationTime, direction, endpoint, expiryTime, name, sourceCorrelationId, sourceId, transactionId, transactionName, type from "
		                + this.keyspace + ".telemetry_event where id = ?");
		this.findEventParentIdStatement = this.session.prepare("select correlationId from " + this.keyspace
		        + ".telemetry_event where id = ?");
		this.findOverviewEventStatement = this.session
		        .prepare("select id, creationTime, application, direction, endpoint, expiryTime, name, type, correlations from "
		                + this.keyspace + ".telemetry_event where id = ?");
		this.findCorrelationDataStatement = this.session
		        .prepare("select application, correlationData, correlations, creationTime, expiryTime, type from " + this.keyspace
		                + ".telemetry_event where id = ?");
		this.findChildEventCreationTimeStatement = this.session.prepare("select creationTime, correlationId, type from " + this.keyspace
		        + ".telemetry_event where id = ?");
		this.findCorrelationsByDataStatement = this.session.prepare("select id, timeunit from " + this.keyspace
		        + ".correlation_data where name_timeunit = ? and name = ? and value = ? and timeunit >= ? and timeunit <= ?");
		this.findPotentialCorrelatingEventDataStatement = this.session.prepare("select creationTime, correlations, expiryTime, type from "
		        + this.keyspace + ".telemetry_event where id = ?");
	}

	public void addEvents(SolrDocumentList results, JsonGenerator generator) throws JsonGenerationException, IOException {
		for (SolrDocument solrDocument : results) {
			ResultSet resultSet = this.session.execute(this.findEventForSearchResultsStatement.bind(UUID.fromString((String) solrDocument
			        .get("id"))));
			Row row = resultSet.one();
			if (row != null) {
				generator.writeStartObject();
				writeStringValue("application", row.getString(0), generator);
				writeUUIDValue("correlationId", row.getUUID(1), generator);
				writeDateValue("creationTime", row.getDate(2), generator);
				writeStringValue("endpoint", row.getString(3), generator);
				writeUUIDValue("id", row.getUUID(4), generator);
				writeStringValue("name", row.getString(5), generator);
				writeStringValue("sourceCorrelationId", row.getString(6), generator);
				writeStringValue("sourceId", row.getString(7), generator);
				generator.writeEndObject();
			}
		}
	}

	public void addEvent(UUID eventId, JsonGenerator generator) throws JsonGenerationException, IOException {
		ResultSet resultSet = this.session.execute(this.findEventForDetailsStatement.bind(eventId));
		Row row = resultSet.one();
		if (row != null) {
			writeStringValue("application", row.getString(0), generator);
			writeStringValue("content", row.getString(1), generator);
			writeUUIDValue("correlationId", row.getUUID(2), generator);
			generator.writeArrayFieldStart("childCorrelationIds");
			List<UUID> correlations = row.getList(3, UUID.class);
			for (UUID childCorrelation : correlations) {
				generator.writeString(childCorrelation.toString());
			}
			generator.writeEndArray();
			writeDateValue("creationTime", row.getDate(4), generator);
			writeStringValue("direction", row.getString(5), generator);
			writeStringValue("endpoint", row.getString(6), generator);
			writeDateValue("expiryTime", row.getDate(7), generator);
			writeUUIDValue("id", eventId, generator);
			writeStringValue("name", row.getString(8), generator);
			writeStringValue("sourceCorrelationId", row.getString(9), generator);
			writeStringValue("sourceId", row.getString(10), generator);
			writeUUIDValue("transactionId", row.getUUID(11), generator);
			writeStringValue("transactionName", row.getString(12), generator);
			writeStringValue("type", row.getString(13), generator);
		}
	}

	public void addEventOverview(UUID eventId, JsonGenerator generator) throws JsonGenerationException, IOException {
		UUID rootEventId = findRootEventId(eventId, new ArrayList<UUID>());
		OverviewEvent tree = new OverviewEvent();
		findChildren(rootEventId, tree, new ArrayList<OverviewEvent>());
		OverviewEvent root = tree.children.get(0);
		sortTelemetryEventTree(root);
		calculateResponseTimes(root);

		long totalOverviewTime = getTotalOverviewTime(root);
		calculateFillColors(root, totalOverviewTime);

		AtomicInteger applicationCount = new AtomicInteger(1);
		AtomicInteger eventCount = new AtomicInteger(0);
		calculateCounters(root, applicationCount, eventCount);

		List<TimeFrame> timeFrames = new ArrayList<TimeFrame>();
		String[] applications = new String[applicationCount.intValue()];
		AtomicInteger eventDepth = new AtomicInteger(0);
		calculateTimeFrames(root, timeFrames, applicationCount.get(), eventDepth, applications);

		// Create the json
		generator.writeNumberField("eventCount", eventCount.intValue());
		generator.writeNumberField("applicationCount", applicationCount.intValue());
		generator.writeNumberField("timeframeCount", timeFrames.size());
		generator.writeArrayFieldStart("applications");
		for (String application : applications) {
			generator.writeString(application);
		}
		generator.writeEndArray();
		generator.writeArrayFieldStart("timeframes");
		for (TimeFrame timeFrame : timeFrames) {
			generator.writeStartArray();
			for (int i = 0; i < applicationCount.intValue(); i++) {
				generator.writeStartObject();
				OverviewEvent overviewEvent = timeFrame.overviewEvents[i];
				if (overviewEvent != null) {
					generator.writeStringField("application", overviewEvent.application);
					generator.writeStringField("eventName", overviewEvent.name);
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

	private void calculateCounters(OverviewEvent overviewEvent, AtomicInteger applicationCount, AtomicInteger eventCount) {
		eventCount.incrementAndGet();
		List<String> appNames = new ArrayList<String>();
		for (OverviewEvent childOverviewEvent : overviewEvent.children) {
			if (TelemetryEventType.MESSAGE_RESPONSE.equals(childOverviewEvent.type)) {
				continue;
			}
			if (childOverviewEvent.application != null) {
				if (!appNames.contains(childOverviewEvent.application) && !childOverviewEvent.application.equals(overviewEvent.application)) {
					appNames.add(childOverviewEvent.application);
				}
			}
		}
		applicationCount.addAndGet(appNames.size());
		for (OverviewEvent childOverviewEvent : overviewEvent.children) {
			calculateCounters(childOverviewEvent, applicationCount, eventCount);
		}
	}

	private void calculateTimeFrames(OverviewEvent overviewEvent, List<TimeFrame> timeFrames, long applicationCount,
	        AtomicInteger eventDepth, String[] applications) {
		if (timeFrames.size() == 0) {
			timeFrames.add(new TimeFrame(applicationCount));
		}
		TimeFrame currentTimeFrame = timeFrames.get(timeFrames.size() - 1);
		if (currentTimeFrame.overviewEvents[eventDepth.get()] != null) {
			currentTimeFrame = new TimeFrame(applicationCount);
			timeFrames.add(currentTimeFrame);
		}
		currentTimeFrame.overviewEvents[eventDepth.get()] = overviewEvent;
		if (applications[eventDepth.get()] == null && overviewEvent.application != null) {
			applications[eventDepth.get()] = overviewEvent.application;
		}

		for (OverviewEvent childOverviewEvent : overviewEvent.children) {
			if (childOverviewEvent.application != null && overviewEvent.application != null
			        && !overviewEvent.application.equals(childOverviewEvent.application)) {
				int addition = 0;
				for (int i = eventDepth.get() + 1; i < applications.length; i++) {
					if (applications[i] != null) {
						if (applications[i].equals(childOverviewEvent.application)) {
							addition = i - eventDepth.get();
							break;
						}
					} else {
						addition = i - eventDepth.get();
						break;
					}
				}
				eventDepth.addAndGet(addition);
				calculateTimeFrames(childOverviewEvent, timeFrames, applicationCount, eventDepth, applications);
				eventDepth.addAndGet(-addition);
			} else {
				calculateTimeFrames(childOverviewEvent, timeFrames, applicationCount, eventDepth, applications);
			}
		}
	}

	private UUID findRootEventId(UUID eventId, List<UUID> foundElements) {
		ResultSet resultSet = this.session.execute(this.findEventParentIdStatement.bind(eventId));
		Row row = resultSet.one();
		if (row == null) {
			return eventId;
		}
		if (!foundElements.contains(eventId)) {
			foundElements.add(eventId);
		}
		UUID parentId = row.getUUID(0);
		if (parentId == null) {
			parentId = findParentIdByDataCorrelation(eventId, null);
		}
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

	/**
	 * Find a parent event by data correlation.
	 * 
	 * @param eventId
	 *            The ID of the event to find the parent from.
	 * 
	 * @param considerdataFrom
	 *            The time range to search for parents. By default events within
	 *            the range from
	 *            <code>creationTime - CORRELATION_SELECTION_OFFSET</code> till
	 *            <code>creationTime</code> will be considered unless
	 *            <code>considerdataFrom</code> not is <code>null</code>. In
	 *            that case
	 *            <code>considerdataFrom> till <code>creationTime</code> will be
	 *            considered to find a parent.
	 * @return The ID of the parent or <code>null</code> if no parent could be
	 *         found.
	 */
	private UUID findParentIdByDataCorrelation(UUID eventId, Date considerdataFrom) {
		CorrelationData correlationData = getCorrelationData(eventId);
		if (correlationData == null || correlationData.data.size() == 0) {
			return null;
		}
		Date startQueryDate = new Date(correlationData.validFrom.getTime() - CORRELATION_SELECTION_OFFSET);
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
				int compare = o2.timestamp.compareTo(o1.timestamp);
				if (compare != 0) {
					return compare;
				}
				// 2 events at exactly the same time. Compare by processing order
				return o2.id.compareTo(o1.id);
			}
		});
		for (CorrelationResult correlationResult : correlatingResults) {
			Row row = this.session.execute(this.findPotentialCorrelatingEventDataStatement.bind(correlationResult.id)).one();
			if (row == null) {
				continue;
			}
			if (row.getDate(0).after(correlationData.validFrom)) {
				// The creationTime of this event is after the creation time of
				// the event we're try to find the parent from. This can
				// impossibly be a parent.
				continue;
			}
			TelemetryEventType type = null;
			try {
				type = TelemetryEventType.valueOf(row.getString(3));
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
				List<UUID> childIds = row.getList(1, UUID.class);
				if (childIds.size() == 0) {
					// A request without a response, hence the request had a
					// timeout
					Date expiryDate = row.getDate(2);
					if (expiryDate != null && !expiryDate.before(correlationData.validTill)) {
						// The expiry date from the request after the validity of the correlation data -> We have a match. 
						return correlationResult.id;
					} else if (expiryDate != null && correlationData.expired) {
						// The parent request expired, and this request expired as well.
						// It's a guess, but these requests probably have a
						// parent-child relationship.
						return correlationResult.id;
					}
				}
				for (UUID childId : childIds) {
					row = this.session.execute(this.findChildEventCreationTimeStatement.bind(childId)).one();
					if (row == null) {
						continue;
					} else if (correlationResult.id.equals(row.getUUID(1))
					        && TelemetryEventType.MESSAGE_RESPONSE.name().equals(row.getString(2))) {
						Date responseDate = row.getDate(0);
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
				// A message response could never be a parent event of any message type.
				continue;
			}
		}
		return null;
	}

	private List<String> determineDateSuffixes(Date startDate, Date endDate) {
		List<String> result = new ArrayList<String>();
		Calendar startCalendar = Calendar.getInstance();
		Calendar endCalendar = Calendar.getInstance();
		startCalendar.setTimeInMillis(startDate.getTime());
		endCalendar.setTimeInMillis(endDate.getTime());
		do {
			result.add(this.format.format(startCalendar.getTime()));
			startCalendar.add(PartitionKeySuffixCreator.SMALLEST_CALENDAR_UNIT, 1);
		} while (startCalendar.before(endCalendar)
		        || (!startCalendar.before(endCalendar) && startCalendar.get(PartitionKeySuffixCreator.SMALLEST_CALENDAR_UNIT) == endCalendar
		                .get(PartitionKeySuffixCreator.SMALLEST_CALENDAR_UNIT)));
		return result;
	}

	private void findChildren(UUID eventId, OverviewEvent parent, List<OverviewEvent> overviewEvents) {
		Row row = this.session.execute(this.findOverviewEventStatement.bind(eventId)).one();
		if (row == null) {
			return;
		}
		OverviewEvent overviewEvent = createOverviewEvent(row);
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
			List<UUID> children = new ArrayList<UUID>();
			children.addAll(row.getList(8, UUID.class));
			Collection<UUID> childIdsByDataCorrelation = findChildIdsByDataCorrelation(eventId);
			for (UUID uuid : childIdsByDataCorrelation) {
				if (!children.contains(uuid)) {
					children.add(uuid);
				}
			}
			for (UUID child : children) {
				findChildren(child, overviewEvent, overviewEvents);
			}
		}
	}

	private OverviewEvent createOverviewEvent(Row row) {
		OverviewEvent overviewEvent = new OverviewEvent();
		overviewEvent.id = row.getUUID(0);
		overviewEvent.creationTime = row.getDate(1).getTime();
		overviewEvent.application = row.getString(2);
		try {
			overviewEvent.direction = TelemetryEventDirection.valueOf(row.getString(3));
		} catch (IllegalArgumentException | NullPointerException e) {
		}
		overviewEvent.endpoint = row.getString(4);
		Date date = row.getDate(5);
		if (date != null) {
			overviewEvent.expirationTime = date.getTime();
		}
		overviewEvent.name = row.getString(6);
		try {
			overviewEvent.type = TelemetryEventType.valueOf(row.getString(7));
		} catch (IllegalArgumentException | NullPointerException e) {
		}

		return overviewEvent;
	}

	private Collection<UUID> findChildIdsByDataCorrelation(UUID eventId) {
		List<UUID> result = new ArrayList<UUID>();
		CorrelationData correlationData = getCorrelationData(eventId);
		if (correlationData == null || correlationData.data.size() == 0) {
			return result;
		}
		if (TelemetryEventType.MESSAGE_DATAGRAM.equals(correlationData.type)) {
			correlationData.validTill.setTime(correlationData.validTill.getTime() + CORRELATION_SELECTION_OFFSET);
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
				return o1.timestamp.compareTo(o2.timestamp);
			}
		});
		for (CorrelationResult correlationResult : correlatingResults) {
			// Make sure we return only direct children.
			if (eventId.equals(findParentIdByDataCorrelation(correlationResult.id, correlationData.validFrom))) {
				result.add(correlationResult.id);
			}
		}
		return result;
	}

	/**
	 * Find the events based on the same correlation data.
	 * 
	 * @param eventId
	 *            The ID of the event to match correlation data against.
	 * @param correlationData
	 *            The correlation data of the event with id <code>eventId</code>
	 *            .
	 * @param startTime
	 *            The start time of the data to consider.
	 * @param finishTime
	 *            The start time of the data to consider.
	 * @return A list with ID's of events that have a match in data correlation.
	 */
	private List<CorrelationResult> getCorrelationResults(UUID eventId, Map<String, String> correlationData, Date startTime, Date finishTime) {
		List<String> dateSuffixes = determineDateSuffixes(startTime, finishTime);
		List<CorrelationResult> correlatingResults = new ArrayList<CorrelationResult>();
		for (String correlationKey : correlationData.keySet()) {
			// Create the keys
			for (String suffix : dateSuffixes) {
				ResultSet resultSet = this.session.execute(this.findCorrelationsByDataStatement.bind(correlationKey + suffix,
				        correlationKey, correlationData.get(correlationKey), startTime, finishTime));
				Iterator<Row> rowIterator = resultSet.iterator();
				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();
					CorrelationResult correlationResult = new CorrelationResult(row.getUUID(0), row.getDate(1));
					if (correlationResult.id.equals(eventId)) {
						continue;
					}
					if (!correlatingResults.contains(correlationResult)) {
						correlatingResults.add(correlationResult);
					}
				}
			}
		}
		return correlatingResults;
	}

	private CorrelationData getCorrelationData(UUID eventId) {
		CorrelationData result = new CorrelationData();
		ResultSet resultSet = this.session.execute(this.findCorrelationDataStatement.bind(eventId));
		Row row = resultSet.one();
		if (row == null) {
			return null;
		}
		result.application = row.getString(0);
		result.data.putAll(row.getMap(1, String.class, String.class));
		if (result.data.size() == 0) {
			return null;
		}
		List<UUID> childCorrelations = row.getList(2, UUID.class);
		result.validFrom = row.getDate(3);
		result.validTill = new Date(result.validFrom.getTime());
		Date expiryTime = row.getDate(4);
		try {
			result.type = TelemetryEventType.valueOf(row.getString(5));
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
			for (UUID childId : childCorrelations) {
				row = this.session.execute(this.findChildEventCreationTimeStatement.bind(childId)).one();
				if (row == null) {
					continue;
				} else if (eventId.equals(row.getUUID(1)) && TelemetryEventType.MESSAGE_RESPONSE.name().equals(row.getString(2))) {
					// We've found the response. Set the finishTime to the time
					// the response was created.
					result.validTill.setTime(row.getDate(0).getTime());
					result.expired = false;
					break;
				}
			}
		}
		return result;
	}

	private void writeUUIDValue(String fieldName, UUID fieldValue, JsonGenerator generator) throws JsonGenerationException, IOException {
		if (fieldValue == null) {
			return;
		}
		generator.writeStringField(fieldName, fieldValue.toString());
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

	private class TimeFrame {

		public OverviewEvent[] overviewEvents;

		private TimeFrame(long applicationCount) {
			this.overviewEvents = new OverviewEvent[(int) applicationCount];
		}
	}

	private class CorrelationResult {

		private UUID id;

		private Date timestamp;

		private CorrelationResult(UUID id, Date timestamp) {
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
