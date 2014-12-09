package com.holster.etm.repository;

import java.util.UUID;

public class CorrelationBySourceIdResult {
	
	public UUID id;
	public UUID transactionId;
	public String transactionName;
	
	public CorrelationBySourceIdResult initialize() {
		this.id = null;
		this.transactionId = null;
		this.transactionName = null;
		return this;
	}

}
