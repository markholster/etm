package com.jecstar.etm.server.core.domain.principal.converter.json;

import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;

public class EtmPrincipalTagsJsonImpl implements EtmPrincipalTags {

    @Override
    public String getIdTag() {
        return "id";
    }

    @Override
    public String getEmailTag() {
        return "email";
    }

    @Override
    public String getFilterQueryTag() {
        return "filter_query";
    }

    @Override
    public String getFilterQueryOccurrenceTag() {
        return "filter_query_occurrence";
    }

    @Override
    public String getAlwaysShowCorrelatedEventsTag() {
        return "always_show_correlated_events";
    }

    @Override
    public String getSearchHistorySizeTag() {
        return "search_history_size";
    }

    @Override
    public String getDefaultSearchRangeTag() {
        return "default_search_range";
    }

    @Override
    public String getLocaleTag() {
        return "locale";
    }

    @Override
    public String getNameTag() {
        return "name";
    }

    @Override
    public String getDisplayNameTag() {
        return "display_name";
    }

    @Override
    public String getPasswordHashTag() {
        return "password_hash";
    }

    @Override
    public String getChangePasswordOnLogonTag() {
        return "change_password_on_logon";
    }

    @Override
    public String getRolesTag() {
        return "roles";
    }

    @Override
    public String getGroupsTag() {
        return "groups";
    }

    @Override
    public String getTimeZoneTag() {
        return "time_zone";
    }

    @Override
    public String getSearchHistoryTag() {
        return "search_history";
    }

    @Override
    public String getSearchTemplatesTag() {
        return "search_templates";
    }

    @Override
    public String getLdapBaseTag() {
        return "ldap_base";
    }

    @Override
    public String getDashboardsTag() {
        return "dashboards";
    }

    @Override
    public String getGraphsTag() {
        return "graphs";
    }

    @Override
    public String getSignalsTag() {
        return "signals";
    }

    @Override
    public String getDashboardDatasourcesTag() {
        return "dashboard_datasources";
    }

    @Override
    public String getSignalDatasourcesTag() {
        return "signal_datasources";
    }

    @Override
    public String getNotifiersTag() {
        return "notifiers";
    }
}
