package com.jecstar.etm.gui.rest.services.search;

public class DefaultSearchTemplates {

	public String toJson() {
		StringBuilder templates = new StringBuilder();
		templates.append("{\"search_templates\":[");
		// WHEN CHANGING THESE TEMPLATES ALSO CHANGE THE ElasticSearchIdentityManager CLASS!!!!
		templates.append(new SearchRequestParameters("endpoints.writing_endpoint_handler.handling_time: [now-1h TO now]").toJsonSearchTemplate("Events of last 60 mins"));
		templates.append("," + new SearchRequestParameters("endpoints.writing_endpoint_handler.handling_time: [now/d TO now/d]").toJsonSearchTemplate("Events of today"));
		templates.append("," + new SearchRequestParameters("endpoints.writing_endpoint_handler.handling_time: [now-1d/d TO now-1d/d]").toJsonSearchTemplate("Events of yesterday"));
		templates.append("]}");
		return templates.toString();
	}
}
