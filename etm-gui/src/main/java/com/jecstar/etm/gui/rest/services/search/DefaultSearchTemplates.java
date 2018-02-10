package com.jecstar.etm.gui.rest.services.search;

public class DefaultSearchTemplates {

    public static final String TEMPLATE_NAME_EVENTS_OF_LAST_60_MINS = "Events of last 60 mins";
    public static final String TEMPLATE_NAME_EVENTS_OF_TODAY = "Events of today";
    public static final String TEMPLATE_NAME_EVENTS_OF_YESTERDAY = "Events of yesterday";

	public String toJson() {
		String templates = "{\"search_templates\":[" +
                new SearchRequestParameters("*", "now-1h", "now").toJsonSearchTemplate(TEMPLATE_NAME_EVENTS_OF_LAST_60_MINS) +
                "," + new SearchRequestParameters("*", "now/d", "now/d").toJsonSearchTemplate(TEMPLATE_NAME_EVENTS_OF_TODAY) +
                "," + new SearchRequestParameters("*", "now-1d/d", " now-1d/d]").toJsonSearchTemplate(TEMPLATE_NAME_EVENTS_OF_YESTERDAY) +
				"]}";
		return templates;
	}
}