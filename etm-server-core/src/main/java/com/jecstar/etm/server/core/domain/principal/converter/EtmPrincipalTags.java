package com.jecstar.etm.server.core.domain.principal.converter;

public interface EtmPrincipalTags {

    String getIdTag();

    String getEmailTag();

    String getApiKeyTag();

    String getSecondaryApiKeyTag();

    String getFilterQueryTag();

    String getFilterQueryOccurrenceTag();

    String getAlwaysShowCorrelatedEventsTag();

    String getSearchHistorySizeTag();

    String getDefaultSearchRangeTag();

    String getLocaleTag();

    String getNameTag();

    String getDisplayNameTag();

    String getPasswordHashTag();

    String getChangePasswordOnLogonTag();

    String getRolesTag();

    String getGroupsTag();

    String getTimeZoneTag();

    String getLdapBaseTag();

    String getDashboardsTag();

    String getGraphsTag();

    String getSearchHistoryTag();

    String getSearchTemplatesTag();

    String getSignalsTag();

    String getDashboardDatasourcesTag();

    String getSignalDatasourcesTag();

    String getNotifiersTag();
}
