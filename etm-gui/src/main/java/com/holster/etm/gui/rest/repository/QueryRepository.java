package com.holster.etm.gui.rest.repository;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
		this.findEventForSearchResultsStatement = this.session.prepare("select application, correlationId, creationTime, endpoint, id, name, sourceCorrelationId, sourceId from " + this.keyspace + ".telemetry_event where id = ?");
		this.findEventForDetailsStatement = this.session.prepare("select application, content, correlationId, correlations, creationTime, direction, endpoint, expiryTime, name, sourceCorrelationId, sourceId, transactionId, transactionName, type from " + this.keyspace + ".telemetry_event where id = ?");
		this.findEventParentIdStatement = this.session.prepare("select correlationId from " + this.keyspace + ".telemetry_event where id = ?");
		this.findOverviewEventStatement = this.session.prepare("select id, creationTime, application, direction, endpoint, expiryTime, name, type, correlations from " + this.keyspace + ".telemetry_event where id = ?");
		this.findCorrelationDataStatement = this.session.prepare("select correlationData, correlations, creationTime, expiryTime, type from " + this.keyspace + ".telemetry_event where id = ?");
		this.findChildEventCreationTimeStatement = this.session.prepare("select creationTime, correlationId, type from " + this.keyspace + ".telemetry_event where id = ?");
		this.findCorrelationsByDataStatement = this.session.prepare("select id, timeunit from " + this.keyspace + ".correlation_data where name_timeunit = ? and name = ? and value = ? and timeunit >= ? and timeunit <= ?");
		this.findPotentialCorrelatingEventDataStatement = this.session.prepare("select creationTime, correlations, type from " + this.keyspace + ".telemetry_event where id = ?");
    }

	public void addEvents(SolrDocumentList results, JsonGenerator generator) throws JsonGenerationException, IOException {
		for (SolrDocument solrDocument : results) {
			ResultSet resultSet = this.session.execute(this.findEventForSearchResultsStatement.bind(UUID.fromString((String) solrDocument.get("id"))));
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
		List<OverviewEvent> overviewEvents = new ArrayList<OverviewEvent>();
		findChildren(rootEventId, null, overviewEvents);
		overviewEvents = sortEventsByHierarchy(overviewEvents, rootEventId);
		
		// Calculate response times
		long totalOverviewTime = -1;
		for (OverviewEvent overviewEvent : overviewEvents) {
			if (TelemetryEventType.MESSAGE_REQUEST.equals(overviewEvent.type)) {
				Optional<OverviewEvent> optionalResponse = overviewEvents.stream().filter(c -> overviewEvent.id.equals(c.parentId) && TelemetryEventType.MESSAGE_RESPONSE.equals(c.type)).findFirst();
				if (optionalResponse.isPresent()) {
					OverviewEvent response = optionalResponse.get();
					long responseTime = response.creationTime - overviewEvent.creationTime;
					if (totalOverviewTime == -1) {
						// First request in the overview contains the total transaction time. Maybe it's best to calculate the difference between the first and last overviewEvent..
						totalOverviewTime = responseTime;
					}
					overviewEvent.responseTime = responseTime;
					response.responseTime = responseTime;
				} else if (overviewEvent.expirationTime > -1) {
					overviewEvent.responseTime = overviewEvent.expirationTime - overviewEvent.creationTime;
				}
			}
		}
		
		// Calculate fill colors
		for (OverviewEvent overviewEvent : overviewEvents) {
			if (TelemetryEventType.MESSAGE_REQUEST.equals(overviewEvent.type)) {
				Optional<OverviewEvent> optionalResponse = overviewEvents.stream().filter(c -> overviewEvent.id.equals(c.parentId) && TelemetryEventType.MESSAGE_RESPONSE.equals(c.type)).findFirst();
				if (optionalResponse.isPresent()) {
					OverviewEvent response = optionalResponse.get();
					long childResponseTimes = overviewEvents.stream().filter(c -> overviewEvent.id.equals(c.parentId) && TelemetryEventType.MESSAGE_REQUEST.equals(c.type)).mapToLong(c -> c.responseTime).sum();
					overviewEvent.absoluteResponseTime = overviewEvent.responseTime - childResponseTimes;
					response.absoluteResponseTime = overviewEvent.absoluteResponseTime;
					float redFactor = ((float)overviewEvent.absoluteResponseTime / (float)totalOverviewTime);
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
		}
		
		long applicationCount = overviewEvents.stream().map(e -> e.application == null ? "?" : e.application).distinct().count();
		List<String> applications = new ArrayList<String>();
		String lastApplication = null;
		List<TimeFrame> timeFrames = new ArrayList<TimeFrame>();
		for (int i=0; i < overviewEvents.size(); i++) {
			OverviewEvent overviewEvent = overviewEvents.get(i);
			String currentApplication = overviewEvent.application;
			if (currentApplication == null) {
				currentApplication  = "?";
			}
			if (currentApplication.equals(lastApplication)) {
				timeFrames.add(new TimeFrame(applicationCount));
			} else if (i == 0) {
				timeFrames.add(new TimeFrame(applicationCount));
			} else if (applications.contains(currentApplication) && timeFrames.get(timeFrames.size() -1).overviewEvents[applications.indexOf(currentApplication)] != null) {
				timeFrames.add(new TimeFrame(applicationCount));
			}
			if (!applications.contains(currentApplication)) {
				applications.add(currentApplication);
			}
			int applicationIx = applications.indexOf(currentApplication);
			TimeFrame timeFrame = timeFrames.get(timeFrames.size() -1);
			timeFrame.overviewEvents[applicationIx] = overviewEvent;
			lastApplication = currentApplication;
		}		
		
		// Create the json
		generator.writeNumberField("eventCount", overviewEvents.size());
		generator.writeNumberField("applicationCount", applicationCount);
		generator.writeNumberField("timeframeCount", timeFrames.size());
		generator.writeArrayFieldStart("applications");
		for (String application : applications) {
			generator.writeString(application);
		}
		generator.writeEndArray();
		generator.writeArrayFieldStart("timeframes");
		for (TimeFrame timeFrame : timeFrames) {
			generator.writeStartArray();
			for (int i=0; i < applicationCount; i++) {
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

	private List<OverviewEvent> sortEventsByHierarchy(List<OverviewEvent> overviewEvents, UUID eventId) {
		List<OverviewEvent> result = new ArrayList<OverviewEvent>(overviewEvents.size());
		OverviewEvent root = overviewEvents.stream().filter(p -> p.id.equals(eventId)).findFirst().get();
		result.add(root);
		List<OverviewEvent> children = overviewEvents.stream().filter(c -> root.id.equals(c.parentId)).collect(Collectors.toList());
		children.sort(new Comparator<OverviewEvent>() {
			@Override
            public int compare(OverviewEvent o1, OverviewEvent o2) {
				// First check by time.
				if (o1.creationTime < o2.creationTime) {
					return -1;
				} else if (o1.creationTime == o2.creationTime) {
					return 1;
				}
				// if time is the same, and type is the same we stop comparing.
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
            }});
		for (OverviewEvent overviewEvent : children) {
			result.addAll(sortEventsByHierarchy(overviewEvents, overviewEvent.id));
		}
	    return result;
    }

	private UUID findRootEventId(UUID eventId, List<UUID> foundElements) {
		ResultSet resultSet = this.session.execute(this.findEventParentIdStatement.bind(eventId));
		Row row = resultSet.one();
		if (row == null) {
			return eventId;
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
	
	private UUID findParentIdByDataCorrelation(UUID eventId, Date considerdataFrom) {
		Map<String, String> correlationData = new HashMap<String, String>();
		Date creationTime = new Date();
		Date finishTime = new Date();
		getCorrelationData(eventId, correlationData, creationTime, finishTime);
		if (correlationData.size() == 0) {
			return null;
		}
		Date startQueryDate = new Date(creationTime.getTime() - CORRELATION_SELECTION_OFFSET);
		if (considerdataFrom != null) {
			startQueryDate.setTime(considerdataFrom.getTime());
		}
		
		List<CorrelationResult> correlatingResults = getCorrelationResults(eventId, correlationData, startQueryDate, creationTime);
		if (correlatingResults.size() == 0) {
			return null;
		}
		// Sort the correlating events in order closest to the creationTime of the given event.
		correlatingResults.sort(new Comparator<CorrelationResult>() {
			@Override
            public int compare(CorrelationResult o1, CorrelationResult o2) {
	            return o2.timestamp.compareTo(o1.timestamp);
            }});
		for (CorrelationResult correlationResult : correlatingResults) {
			ResultSet resultSet = this.session.execute(this.findPotentialCorrelatingEventDataStatement.bind(correlationResult.id));
			Row row = resultSet.one();
			if (row == null) {
				continue;
			}
			TelemetryEventType type = null;
			try {
				type = TelemetryEventType.valueOf(row.getString(2));
			} catch (Exception e) {
				// Without a type we cannot securely correlate by data.
				continue;
			}
			if (TelemetryEventType.MESSAGE_DATAGRAM.equals(type)) {
				// Closest correlated event is a datagram message. It's still a guess if this event is the parent event, but it's a pretty good guess after all.
				return correlationResult.id;
			} else if (TelemetryEventType.MESSAGE_REQUEST.equals(type)) {
				// Check if the response of the found event is created after the finishtime of this event. If so, we've got a winner.
				List<UUID> childIds = row.getList(1, UUID.class);
				for (UUID childId : childIds) {
					ResultSet childResultSet = this.session.execute(this.findChildEventCreationTimeStatement.bind(childId));
					row = childResultSet.one();
					if (row == null) {
						continue;
					} else if (correlationResult.id.equals(row.getUUID(1)) && TelemetryEventType.MESSAGE_RESPONSE.name().equals(row.getString(2))) {
						Date responseDate = row.getDate(0);
						if (!responseDate.before(finishTime)) {
							// The response creation time if not before this events finishtime -> It's a parent!
							return correlationResult.id;
						}
					}
				}
			} else if (TelemetryEventType.MESSAGE_RESPONSE.equals(type)) {
				// A response with a creationtime before this events creationtime could impossibly be a parent event.
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
	    } while (startCalendar.before(endCalendar) || (!startCalendar.before(endCalendar) && startCalendar.get(PartitionKeySuffixCreator.SMALLEST_CALENDAR_UNIT) == endCalendar.get(PartitionKeySuffixCreator.SMALLEST_CALENDAR_UNIT)));
	    return result;
    }

	private void findChildren(UUID eventId, UUID parentEventId, List<OverviewEvent> overviewEvents) {
	    ResultSet resultSet = this.session.execute(this.findOverviewEventStatement.bind(eventId));
		Row row = resultSet.one();
		if (row == null) {
			return;
		}
		OverviewEvent overviewEvent = createOverviewEvent(row);
		overviewEvent.parentId = parentEventId;
		if (!overviewEvents.contains(overviewEvent)) {
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
				findChildren(child, eventId, overviewEvents);
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
		} catch (IllegalArgumentException | NullPointerException e) {}
		overviewEvent.endpoint = row.getString(4);
		Date date = row.getDate(5);
		if (date != null) {
			overviewEvent.expirationTime = date.getTime();
		}
		overviewEvent.name = row.getString(6);
		try {
			overviewEvent.type = TelemetryEventType.valueOf(row.getString(7));
		} catch (IllegalArgumentException  | NullPointerException e) {}

		return overviewEvent;
    }

	private Collection<UUID> findChildIdsByDataCorrelation(UUID eventId) {
		List<UUID> result = new ArrayList<UUID>();
		Map<String, String> correlationData = new HashMap<String, String>();
		Date creationTime = new Date();
		Date finishTime = new Date();
		TelemetryEventType type = getCorrelationData(eventId, correlationData, creationTime, finishTime);
		if (correlationData.size() == 0) {
			return result;
		}
		if (TelemetryEventType.MESSAGE_DATAGRAM.equals(type)) {
			finishTime.setTime(finishTime.getTime() + CORRELATION_SELECTION_OFFSET);
		}
		List<CorrelationResult> correlatingResults = getCorrelationResults(eventId, correlationData, creationTime, finishTime);
		if (correlatingResults.size() == 0) {
			return result;
		}
		// Sort the correlating events in order closest to the creationTime of the given event.
		correlatingResults.sort(new Comparator<CorrelationResult>() {
			@Override
            public int compare(CorrelationResult o1, CorrelationResult o2) {
	            return o1.timestamp.compareTo(o2.timestamp);
            }});
		for (CorrelationResult correlationResult : correlatingResults) {
			// Make sure we return only direct children.
			if (eventId.equals(findParentIdByDataCorrelation(correlationResult.id, creationTime))) {
				result.add(correlationResult.id);
			}
		}
	    return result;
    }
	
	
	private List<CorrelationResult> getCorrelationResults(UUID eventId, Map<String, String> correlationData, Date startTime, Date finishTime) {
		List<String> dateSuffixes = determineDateSuffixes(startTime, finishTime);
		List<CorrelationResult> correlatingResults = new ArrayList<CorrelationResult>();
		for (String correlationKey : correlationData.keySet()) {
			// Create the keys
			for (String suffix : dateSuffixes) {
				ResultSet resultSet = this.session.execute(this.findCorrelationsByDataStatement.bind(correlationKey + suffix, correlationKey, correlationData.get(correlationKey), startTime, finishTime));
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

	private TelemetryEventType getCorrelationData(UUID eventId, Map<String, String> correlationData, Date creationTime, Date finishTime) {
		ResultSet resultSet = this.session.execute(this.findCorrelationDataStatement.bind(eventId));
		Row row = resultSet.one();
		if (row == null) {
			return null;
		}
		correlationData.putAll(row.getMap(0, String.class, String.class));
		if (correlationData.size() == 0) {
			return null;
		}
		List<UUID> childCorrelations =  row.getList(1, UUID.class);
		creationTime.setTime(row.getDate(2).getTime());
		finishTime.setTime(creationTime.getTime());
		Date expiryTime = row.getDate(3);
		TelemetryEventType type = null;
		try {
			type = TelemetryEventType.valueOf(row.getString(4));
		} catch (Exception e) {
			// Without a type we cannot securely correlate by data.
			return null;
		}
		if (TelemetryEventType.MESSAGE_RESPONSE.equals(type)) {
			// A response should always be correlated by it's sourceCorrelationId, not by data.
		} else if (TelemetryEventType.MESSAGE_REQUEST.equals(type)) {
			// First set the scope of a request to it's expiry time.
			if (expiryTime != null && expiryTime.getTime() != 0) {
				finishTime.setTime(expiryTime.getTime());
			}
			for (UUID childId : childCorrelations) {
				resultSet = this.session.execute(this.findChildEventCreationTimeStatement.bind(childId));
				row = resultSet.one();
				if (row == null) {
					continue;
				} else if (eventId.equals(row.getUUID(1)) && TelemetryEventType.MESSAGE_RESPONSE.name().equals(row.getString(2))) {
					// We've found the response. Set the finishTime to the time the response was created.
					finishTime.setTime(row.getDate(0).getTime());
					break;
				}
			}
		}
		return type;
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
	
	private class OverviewEvent {

		private UUID id;
		
		private UUID parentId;
		
		private long creationTime;
		
		private long expirationTime;
		
		private String application;
		
		private TelemetryEventDirection direction;
		
		private TelemetryEventType type;
		
		private String name;
		
		private String endpoint;
		
		private String color;
		
		private long responseTime = -1;
		
		private long absoluteResponseTime = -1;
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof OverviewEvent) {
				OverviewEvent other = (OverviewEvent) obj;
				return other.id.equals(this.id);
			}
		    return super.equals(obj);
		}
		
		@Override
		public int hashCode() {
		    return this.id.hashCode();
		}
		
		@Override
		public String toString() {
		    return this.name + "(" + this.id + ")";
		}
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
