package com.jecstar.etm.gui.rest.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.jecstar.etm.core.TelemetryEventType;

public class CorrelationData {

	public String eventId;
	public String application;
	public Map<String, String> data = new HashMap<String, String>(); 
	public Date validFrom;
	public Date validTill;
	public TelemetryEventType type;
	public boolean expired;

}
