package com.jecstar.etm.gui.rest.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;

public class OverviewEvent {

	public UUID id;
	
	public long creationTime;
	
	public long expirationTime;
	
	public String application;
	
	public TelemetryEventDirection direction;
	
	public TelemetryEventType type;
	
	public String name;
	
	public String endpoint;
	
	public String color;
	
	public long responseTime = -1;
	
	public long absoluteResponseTime = -1;
	
	public List<OverviewEvent> children = new ArrayList<OverviewEvent>();
	
	public OverviewEvent getMessageResponseOverviewEvent() {
		if (!TelemetryEventType.MESSAGE_REQUEST.equals(this.type)) {
			return null;
		}
		if (this.children.size() == 0) {
			return null;
		}
		Optional<OverviewEvent> optionalResponse = this.children.stream().filter(p -> TelemetryEventType.MESSAGE_RESPONSE.equals(p.type)).findFirst();
		if (!optionalResponse.isPresent()) {
			return null;
		}
		return optionalResponse.get();
    }
	
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
	    return this.name + ":" + this.application + " (" + this.id + ")";
	}
}
