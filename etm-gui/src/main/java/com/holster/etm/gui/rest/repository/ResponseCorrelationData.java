package com.holster.etm.gui.rest.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResponseCorrelationData {

	public UUID id;
	public Map<String, String> data = new HashMap<String, String>();
	public Date validFrom;

}
