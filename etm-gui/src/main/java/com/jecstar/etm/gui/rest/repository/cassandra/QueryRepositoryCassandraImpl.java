package com.jecstar.etm.gui.rest.repository.cassandra;

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
import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.cassandra.PartitionKeySuffixCreator;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.core.util.ObjectUtils;
import com.jecstar.etm.gui.rest.repository.CorrelationData;
import com.jecstar.etm.gui.rest.repository.OverviewEvent;
import com.jecstar.etm.gui.rest.repository.QueryRepository;
import com.jecstar.etm.gui.rest.repository.ResponseCorrelationData;

public class QueryRepositoryCassandraImpl implements QueryRepository {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(QueryRepositoryCassandraImpl.class);

	private final EtmConfiguration etmConfiguration;
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

	public QueryRepositoryCassandraImpl(Session session, EtmConfiguration etmConfiguration) {
		this.session = session;
		this.etmConfiguration = etmConfiguration;
		this.findEventForSearchResultsStatement = this.session
		        .prepare("select application, correlationId, creationTime, endpoint, id, name, sourceCorrelationId, sourceId from telemetry_event where id = ?");
		this.findEventForDetailsStatement = this.session
		        .prepare("select application, content, correlationId, correlations, creationTime, direction, endpoint, expiryTime, name, sourceCorrelationId, sourceId, transactionId, transactionName, type from telemetry_event where id = ?");
		this.findEventParentIdStatement = this.session.prepare("select correlationId from telemetry_event where id = ?");
		this.findOverviewEventStatement = this.session
		        .prepare("select id, creationTime, application, direction, endpoint, expiryTime, name, type, correlations from telemetry_event where id = ?");
		this.findCorrelationDataStatement = this.session
		        .prepare("select application, correlationData, correlations, creationTime, expiryTime, type from telemetry_event where id = ?");
		this.findChildEventCreationTimeStatement = this.session.prepare("select creationTime, correlationId, type from telemetry_event where id = ?");
		this.findCorrelationsByDataStatement = this.session.prepare("select id, timeunit from correlation_data where name_timeunit = ? and name = ? and value = ? and timeunit >= ? and timeunit <= ?");
		this.findPotentialCorrelatingEventDataStatement = this.session.prepare("select creationTime, correlations, expiryTime, type from telemetry_event where id = ?");
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

	private UUID findRootEventId(UUID eventId, List<UUID> foundElements) {
		if (!foundElements.contains(eventId)) {
			foundElements.add(eventId);
		}
		UUID parentId = findParentId(eventId, null);
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
	
	private UUID findParentId(UUID eventId, Date considerdataFrom) {
		Row row = this.session.execute(this.findEventParentIdStatement.bind(eventId)).one();
		if (row == null) {
			return null;
		}
		UUID parentId = row.getUUID(0);
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
		if (eventId == null) {
			return null;
		}
		CorrelationData correlationData = getCorrelationData(eventId);
		if (correlationData == null || correlationData.data.size() == 0) {
			return null;
		}
		Date startQueryDate = new Date(correlationData.validFrom.getTime() - this.etmConfiguration.getDataCorrelationTimeOffset());
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
				if (isBefore(o1.id, o1.timestamp, o2.id, o2.timestamp)) {
					return 1;
				}
				return -1;
			}
		});
		for (CorrelationResult correlationResult : correlatingResults) {
			Row row = this.session.execute(this.findPotentialCorrelatingEventDataStatement.bind(correlationResult.id)).one();
			if (row == null) {
				continue;
			}
			if (isAfter(correlationResult.id, row.getDate(0), eventId, correlationData.validFrom)) {
				// The creationTime of this event is after the creation time of
				// the event we try to find the parent from. This can
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
			correlationData.validTill.setTime(correlationData.validTill.getTime() + this.etmConfiguration.getDataCorrelationTimeOffset());
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
				if (isBefore(o1.id, o1.timestamp, o2.id, o2.timestamp)) {
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
	
	private void findChildrenByCorrelationResults(CorrelationData correlationData, ResponseCorrelationData responseCorrelationData, List<UUID> responseList) {
		// Get the response correlation data and see if other request match this response data.
		if (responseCorrelationData != null) {
			List<CorrelationResult> responseMatchedCorrelationResults = getCorrelationResults(responseCorrelationData.id, responseCorrelationData.data, responseCorrelationData.validFrom, correlationData.validTill);
			for (CorrelationResult responseMatchedCorrelationResult : responseMatchedCorrelationResults) {
				UUID parent = findParentId(responseMatchedCorrelationResult.id, correlationData.validFrom);
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
					UUID correlationId = row.getUUID(0);
					if (correlationId.equals(eventId)) {
						continue;
					}
					if (correlatingResults.size() > this.etmConfiguration.getDataCorrelationMax()) {
						if (log.isWarningLevelEnabled()) {
							log.logWarningMessage("Found more than " + this.etmConfiguration.getDataCorrelationMax() + " data correlations. Stop looking for more results. Overview may be incorrect.");
						}
						break;
					}
					CorrelationResult correlationResult = new CorrelationResult(correlationId, row.getDate(1));
					if (!correlatingResults.contains(correlationResult)) {
						correlatingResults.add(correlationResult);
					}
				}
			}
		}
		return correlatingResults;
	}
	
	/**
	 * Check if the given <code>UUID</code> represents a
	 * {@link TelemetryEventType.MESSAGE_REQUEST} and, if so, returns the
	 * correlation data of the corresponding response.
	 * 
	 * @param eventId
	 * @return
	 */
	private ResponseCorrelationData getResponseCorrelationData(UUID eventId) {
		Row row = this.session.execute(this.findCorrelationDataStatement.bind(eventId)).one();
		if (row == null) {
			return null;
		}
		if (!TelemetryEventType.MESSAGE_REQUEST.name().equals(row.getString(5))) {
			return null;
		}
		List<UUID> correlations = row.getList(2,UUID.class);
		if (correlations == null || correlations.size() == 0) {
			return null;
		}
		for (UUID correlationId : correlations) {
			row = this.session.execute(this.findCorrelationDataStatement.bind(correlationId)).one();
			if (row == null) {
				continue;
			}
			if (!TelemetryEventType.MESSAGE_RESPONSE.name().equals(row.getString(5))) {
				continue;
			}
			ResponseCorrelationData result = new ResponseCorrelationData();
			result.id = correlationId;
			result.validFrom = row.getDate(3);
			result.data = row.getMap(1, String.class, String.class);
			return result;
		}
		return null;
	}

	private CorrelationData getCorrelationData(UUID eventId) {
		CorrelationData result = new CorrelationData();
		result.eventId = eventId;
		Row row = this.session.execute(this.findCorrelationDataStatement.bind(eventId)).one();
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
					Date responseCreationTime = row.getDate(0);
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

	/**
	 * Checks of<code>date1</code> is before <code>date2</code>. If both dates
	 * are the exact same moment, the UUID's are compared. This way the processing order is taken into account.
	 * 
	 * @param eventId1 The first event ID.
	 * @param date1 The first date.
	 * @param eventId2 The second event ID.
	 * @param date2 The second date.
	 * @return <code>true</code> if <code>date1</code> is before <code>date2</code>, <code>false</code> otherwise.
	 */
	private boolean isBefore(UUID eventId1, Date date1, UUID eventId2, Date date2) {
		if (date1.equals(date2)) {
			if (eventId1.equals(eventId2)) {
				return false;
			}
			return eventId1.compareTo(eventId2) == -1;
		}
		return date1.before(date2);
	}

	/**
	 * Checks of<code>date1</code> is after <code>date2</code>. If both dates
	 * are the exact same moment, the UUID's are compared. This way the processing order is taken into account.
	 * 
	 * @param eventId1 The first event ID.
	 * @param date1 The first date.
	 * @param eventId2 The second event ID.
	 * @param date2 The second date.
	 * @return <code>true</code> if <code>date1</code> is after <code>date2</code>, <code>false</code> otherwise.
	 */
	private boolean isAfter(UUID eventId1, Date date1, UUID eventId2, Date date2) {
		if (date1.equals(date2)) {
			if (eventId1.equals(eventId2)) {
				return false;
			}
			return eventId1.compareTo(eventId2) == 1;
		}
		return date1.after(date2);
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
