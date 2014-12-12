package com.holster.etm.repository;

import java.util.Date;
import java.util.UUID;

public class CorrelationBySourceIdResult {
	
	public UUID id;
	public UUID transactionId;
	public String transactionName;
	public Date eventTime = new Date(0);
	
	public CorrelationBySourceIdResult(UUID id, UUID transactionId, String transactionName, long eventTime) {
		this();
	    this.id = id;
	    this.transactionId = transactionId;
	    this.transactionName = transactionName;
	    this.eventTime.setTime(eventTime);
    }
	
	public CorrelationBySourceIdResult() {
	}

	public CorrelationBySourceIdResult initialize() {
		this.id = null;
		this.transactionId = null;
		this.transactionName = null;
		this.eventTime.setTime(0);
		return this;
	}

}
