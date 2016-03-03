package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.jecstar.etm.core.util.ObjectUtils;

public class Endpoint {

	/**
	 * The name of the endpoint
	 */
	public String name;
	
	/**
	 * The handlers that were reading the event.
	 */
	public List<EndpointHandler> readingEndpointHandlers = new ArrayList<EndpointHandler>();
	
	/**
	 * The handler that was writing the event.
	 */
	public EndpointHandler writingEndpointHandler = new EndpointHandler();

	public void initialize() {
		this.name = null;
		this.readingEndpointHandlers.clear();		
		this.writingEndpointHandler.initialize();		
	}
	
	public void initialize(Endpoint copy) {
		this.name = copy.name;
		this.readingEndpointHandlers.clear();
		for (EndpointHandler endpointHandler : copy.readingEndpointHandlers) {
			EndpointHandler copyEndpointHandler = new EndpointHandler();
			copyEndpointHandler.initialize(endpointHandler);
			this.readingEndpointHandlers.add(copyEndpointHandler);
		}
		this.writingEndpointHandler.initialize(copy.writingEndpointHandler);		
	}
	
	public ZonedDateTime getEarliestHandlingTime() {
		if (this.writingEndpointHandler.handlingTime != null) {
			return this.writingEndpointHandler.handlingTime;
		}
		ZonedDateTime earliest = null;
		for (EndpointHandler endpointHandler : this.readingEndpointHandlers) {
			if (earliest == null || (endpointHandler.handlingTime != null && endpointHandler.handlingTime.isBefore(earliest))) {
				earliest = endpointHandler.handlingTime;
			}
		}
		return earliest;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Endpoint) {
			return ObjectUtils.equalsNullProof(this.name, ((Endpoint)obj).name);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		if (this.name == null) {
			return 1;
		}
		return this.name.hashCode();
	}
}
