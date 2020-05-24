/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.domain.principal.converter;

public interface EtmPrincipalTags {

    default String getAlwaysShowCorrelatedEventsTag() {
        return "always_show_correlated_events";
    }

    default String getApiKeyTag() {
        return "api_key";
    }

    default String getChangePasswordOnLogonTag() {
        return "change_password_on_logon";
    }

    default String getDashboardDatasourcesTag() {
        return "dashboard_datasources";
    }

    default String getDashboardsTag() {
        return "dashboards";
    }

    default String getDisplayNameTag() {
        return "display_name";
    }

    default String getEmailTag() {
        return "email";
    }

    default String getEventFieldDeniesTag() {
        return "event_field_denies";
    }

    default String getEventFieldGrantsTag() {
        return "event_field_grants";
    }

    default String getFilterQueryTag() {
        return "filter_query";
    }

    default String getFilterQueryOccurrenceTag() {
        return "filter_query_occurrence";
    }

    default String getGraphsTag() {
        return "graphs";
    }

    default String getGroupsTag() {
        return "groups";
    }

    default String getIdTag() {
        return "id";
    }

    default String getLdapBaseTag() {
        return "ldap_base";
    }

    default String getLocaleTag() {
        return "locale";
    }

    default String getNameTag() {
        return "name";
    }

    default String getNotifiersTag() {
        return "notifiers";
    }

    default String getPasswordHashTag() {
        return "password_hash";
    }

    default String getRolesTag() {
        return "roles";
    }

    default String getSearchHistoryTag() {
        return "search_history";
    }

    default String getSearchHistorySizeTag() {
        return "search_history_size";
    }

    default String getSearchTemplatesTag() {
        return "search_templates";
    }

    default String getSecondaryApiKeyTag() {
        return "secondary_api_key";
    }

    default String getSignalDatasourcesTag() {
        return "signal_datasources";
    }

    default String getSignalsTag() {
        return "signals";
    }

    default String getTimeZoneTag() {
        return "time_zone";
    }
}
