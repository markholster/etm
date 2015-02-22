package com.holster.etm.processor.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataRetention {

	public Date retentionTimestamp = new Date();
	public Date eventOccurrenceTimestamp = new Date();
	public UUID id;
	public String sourceId;
	public String partionKeySuffix;
	public String applicationName;
	public String eventName;
	public String transactionName;
	public Map<String, String> correlationData = new HashMap<String, String>();
	

	public void clear() {
		this.retentionTimestamp.setTime(0);
		this.eventOccurrenceTimestamp.setTime(0);
		this.id = null;
		this.sourceId = null;
		this.partionKeySuffix = null;
		this.applicationName = null;
		this.eventName = null;
		this.transactionName = null;
		this.correlationData.clear();
	}

}
