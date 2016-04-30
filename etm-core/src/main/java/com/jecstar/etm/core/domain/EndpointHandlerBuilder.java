package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

public class EndpointHandlerBuilder {

	private final EndpointHandler endpointHandler;
	
	public EndpointHandlerBuilder() {
		this.endpointHandler = new EndpointHandler();
	}
	
	public EndpointHandler build() {
		return this.endpointHandler;
	}
	
	public EndpointHandlerBuilder setHandlingTime(ZonedDateTime handlingTime) {
		this.endpointHandler.handlingTime = handlingTime;
		return this;
	}

	public EndpointHandlerBuilder setTransactionId(String transactionId) {
		this.endpointHandler.transactionId = transactionId;
		return this;
	}
	
	public EndpointHandlerBuilder setApplication(Application application) {
		this.endpointHandler.application = application;
		return this;
	}
	
	public EndpointHandlerBuilder setApplication(ApplicationBuilder applicationBuilder) {
		this.endpointHandler.application = applicationBuilder.build();
		return this;
	}
	
	public EndpointHandlerBuilder setLocation(Location location) {
		this.endpointHandler.location = location;
		return this;
	}
	
	public EndpointHandlerBuilder setLocation(LocationBuilder locationBuilder) {
		this.endpointHandler.location = locationBuilder.build();
		return this;
	}

}
