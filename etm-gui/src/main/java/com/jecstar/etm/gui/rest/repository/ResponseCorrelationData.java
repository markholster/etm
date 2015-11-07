package com.jecstar.etm.gui.rest.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ResponseCorrelationData {

	public String id;
	public Map<String, String> data = new HashMap<String, String>();
	public Date validFrom;

}
