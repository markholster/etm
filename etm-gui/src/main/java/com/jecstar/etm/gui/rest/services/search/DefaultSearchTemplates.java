package com.jecstar.etm.gui.rest.services.search;

import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;

public class DefaultSearchTemplates {

    public static final String TEMPLATE_NAME_EVENTS_OF_LAST_60_MINS = "Events of last 60 mins";
    public static final String TEMPLATE_NAME_EVENTS_OF_TODAY = "Events of today";
    public static final String TEMPLATE_NAME_EVENTS_OF_YESTERDAY = "Events of yesterday";

    public String toJson(EtmPrincipal etmPrincipal) {
        String templates = "{\"user\": { \"search_templates\":[" +
                new SearchRequestParameters("*", "now-1h", "now", etmPrincipal).toJsonSearchTemplate(TEMPLATE_NAME_EVENTS_OF_LAST_60_MINS) +
                "," + new SearchRequestParameters("*", "now/d", "now/d", etmPrincipal).toJsonSearchTemplate(TEMPLATE_NAME_EVENTS_OF_TODAY) +
                "," + new SearchRequestParameters("*", "now-1d/d", "now-1d/d", etmPrincipal).toJsonSearchTemplate(TEMPLATE_NAME_EVENTS_OF_YESTERDAY) +
                "]}}";
        return templates;
    }
}