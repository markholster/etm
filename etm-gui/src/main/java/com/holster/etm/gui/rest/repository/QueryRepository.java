package com.holster.etm.gui.rest.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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

public class QueryRepository {

	private final String keyspace = "etm";
	private final Session session;
	private final PreparedStatement findEventForSearchResultsStatement;
	private final PreparedStatement findEventForDetailsStatement;
	private final PreparedStatement findEventParentId;
	private final PreparedStatement findOverviewEvent;

	public QueryRepository(Session session) {
		this.session = session;
		this.findEventForSearchResultsStatement = this.session.prepare("select application, correlationId, creationTime, endpoint, id, name, sourceCorrelationId, sourceId from " + this.keyspace + ".telemetry_event where id = ?");
		this.findEventForDetailsStatement = this.session.prepare("select application, content, correlationId, correlations, creationTime, direction, endpoint, expiryTime, name, sourceCorrelationId, sourceId, transactionId, transactionName, type from " + this.keyspace + ".telemetry_event where id = ?");
		this.findEventParentId = this.session.prepare("select correlationId from " + this.keyspace + ".telemetry_event where id = ?");
		this.findOverviewEvent = this.session.prepare("select id, creationTime, application, direction, endpoint, expiryTime, name, type, correlations from " + this.keyspace + ".telemetry_event where id = ?");
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
		ResultSet resultSet = this.session.execute(this.findEventParentId.bind(eventId));
		Row row = resultSet.one();
		if (row == null) {
			return eventId;
		}
		UUID parentId = row.getUUID(0);
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
	
	private void findChildren(UUID eventId, UUID parentEventId, List<OverviewEvent> overviewEvents) {
	    ResultSet resultSet = this.session.execute(this.findOverviewEvent.bind(eventId));
		Row row = resultSet.one();
		if (row == null) {
			return;
		}
		OverviewEvent overviewEvent = createOverviewEvent(row);
		overviewEvent.parentId = parentEventId;
		if (!overviewEvents.contains(overviewEvent)) {
			overviewEvents.add(overviewEvent);
			List<UUID> children = row.getList(8, UUID.class);
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


}
