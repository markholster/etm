package com.jecstar.etm.gui.rest.services.search;

public class DefaultSearchTemplates {

	public String toJson() {
		String templates = "{\"search_templates\":[" +
				new SearchRequestParameters("endpoints.writing_endpoint_handler.handling_time: [now-1h TO now]").toJsonSearchTemplate("Events of last 60 mins") +
				"," + new SearchRequestParameters("endpoints.writing_endpoint_handler.handling_time: [now/d TO now/d]").toJsonSearchTemplate("Events of today") +
				"," + new SearchRequestParameters("endpoints.writing_endpoint_handler.handling_time: [now-1d/d TO now-1d/d]").toJsonSearchTemplate("Events of yesterday") +
				"]}";
		// WHEN CHANGING THESE TEMPLATES ALSO CHANGE THE ElasticSearchIdentityManager CLASS!!!!
		return templates;
	}
}
