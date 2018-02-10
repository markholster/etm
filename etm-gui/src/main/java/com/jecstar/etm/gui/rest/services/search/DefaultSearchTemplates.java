package com.jecstar.etm.gui.rest.services.search;

public class DefaultSearchTemplates {

	public String toJson() {
		String templates = "{\"search_templates\":[" +
                new SearchRequestParameters("*", "now-1h", "now").toJsonSearchTemplate("Events of last 60 mins") +
                "," + new SearchRequestParameters("*", "now/d", "now/d").toJsonSearchTemplate("Events of today") +
                "," + new SearchRequestParameters("*", "now-1d/d", " now-1d/d]").toJsonSearchTemplate("Events of yesterday") +
				"]}";
		return templates;
	}
}
