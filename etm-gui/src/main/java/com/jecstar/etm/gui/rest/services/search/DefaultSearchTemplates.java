package com.jecstar.etm.gui.rest.services.search;

public class DefaultSearchTemplates {

	public String toJson() {
		String templates = "{\"search_templates\":[" +
				new SearchRequestParameters("timestamp: [now-1h TO now]").toJsonSearchTemplate("Events of last 60 mins") +
				"," + new SearchRequestParameters("timestamp: [now/d TO now/d]").toJsonSearchTemplate("Events of today") +
				"," + new SearchRequestParameters("timestamp: [now-1d/d TO now-1d/d]").toJsonSearchTemplate("Events of yesterday") +
				"]}";
		return templates;
	}
}
