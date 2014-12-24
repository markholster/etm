package com.holster.etm.processor.rest;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.TelemetryEventType;
import com.holster.etm.processor.processor.TelemetryEventProcessor;

@Path("/event")
public class RestTelemetryEventProcessor {

	@Inject
	private TelemetryEventProcessor telemetryEventProcessor;
	
	private final TelemetryEvent telemetryEvent = new TelemetryEvent();

	private final JsonFactory jsonFactory = new JsonFactory();
	
	@POST
	@Path("/add")
	@Consumes(MediaType.APPLICATION_JSON)
	public void addEvent(InputStream data) {
		try {
			this.telemetryEvent.initialize();
	        JsonParser parser = this.jsonFactory.createJsonParser(data);
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				String name = parser.getCurrentName();
				if ("application".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.application = parser.getText(); 
				} else if ("content".equals(name)) {
					this.telemetryEvent.content = parser.getText();
				} else if ("type".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.type = determineEventType(parser.getText());
				} else {
					parser.nextToken();
				}
				// TODO parse de rest van het event.
			}
			this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
        } catch (IOException e) {
        	// TODO logging
        }
	}
	
	private TelemetryEventType determineEventType(String eventType) {
		try {
			TelemetryEventType telemetryEventType = TelemetryEventType.valueOf(eventType);
			return telemetryEventType;
		} catch (IllegalArgumentException e) {
			// TODO logging
		}
		return null;
	}
	
}
