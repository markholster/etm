package com.jecstar.etm.gui.rest.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.jecstar.etm.core.TelemetryMessageEventType;

public class CorrelationData {

	public UUID eventId;
	public String application;
	public Map<String, String> data = new HashMap<String, String>(); 
	public Date validFrom;
	public Date validTill;
	public TelemetryMessageEventType type;
	public boolean expired;

}
