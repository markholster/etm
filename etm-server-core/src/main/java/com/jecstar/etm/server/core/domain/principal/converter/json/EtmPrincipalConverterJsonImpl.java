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

package com.jecstar.etm.server.core.domain.principal.converter.json;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.domain.QueryOccurrence;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class EtmPrincipalConverterJsonImpl implements EtmPrincipalConverter<String> {

    private final EtmPrincipalTags tags = new EtmPrincipalTagsJsonImpl();
    private final JsonConverter converter = new JsonConverter();

    @Override
    public String writePrincipal(EtmPrincipal etmPrincipal) {
        JsonBuilder builder = new JsonBuilder();
        builder.startObject();
        builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
        builder.startObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
        builder.field(this.tags.getIdTag(), etmPrincipal.getId());
        builder.field(this.tags.getEmailTag(), etmPrincipal.getEmailAddress(), true);
        builder.field(this.tags.getApiKeyTag(), etmPrincipal.getApiKey(), true);
        builder.field(this.tags.getSecondaryApiKeyTag(), etmPrincipal.getSecondaryApiKey(), true);
        builder.field(this.tags.getFilterQueryTag(), etmPrincipal.getFilterQuery(), true);
        builder.field(this.tags.getFilterQueryOccurrenceTag(), etmPrincipal.getFilterQueryOccurrence().name());
        builder.field(this.tags.getAlwaysShowCorrelatedEventsTag(), etmPrincipal.isAlwaysShowCorrelatedEvents());
        builder.field(this.tags.getSearchHistorySizeTag(), etmPrincipal.getHistorySize());
        builder.field(this.tags.getDefaultSearchRangeTag(), etmPrincipal.getDefaultSearchRange(), true);
        builder.field(this.tags.getLocaleTag(), etmPrincipal.getLocale().toLanguageTag());
        builder.field(this.tags.getNameTag(), etmPrincipal.getName(), true);
        builder.field(this.tags.getPasswordHashTag(), etmPrincipal.getPasswordHash());
        builder.field(this.tags.getChangePasswordOnLogonTag(), etmPrincipal.isChangePasswordOnLogon());
        builder.field(this.tags.getLdapBaseTag(), etmPrincipal.isLdapBase());
        builder.field(this.tags.getRolesTag(), etmPrincipal.getRoles());
        builder.field(this.tags.getDashboardDatasourcesTag(), etmPrincipal.getDashboardDatasources());
        builder.field(this.tags.getNotifiersTag(), etmPrincipal.getNotifiers());
        builder.field(this.tags.getGroupsTag(), etmPrincipal.getGroups().stream().filter(g -> !g.isLdapBase()).map(EtmGroup::getName).collect(Collectors.toSet()));
        builder.field(this.tags.getTimeZoneTag(), etmPrincipal.getTimeZone().getID());
        builder.endObject().endObject();
        return builder.build();
    }

    @Override
    public EtmPrincipal readPrincipal(String jsonContent) {
        return readPrincipal(this.converter.toMap(jsonContent));
    }

    @Override
    public String writeGroup(EtmGroup etmGroup) {
        JsonBuilder builder = new JsonBuilder();
        builder.startObject();
        builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
        builder.startObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP);
        builder.field(this.tags.getNameTag(), etmGroup.getName(), true);
        builder.field(this.tags.getDisplayNameTag(), etmGroup.getDisplayName(), true);
        builder.field(this.tags.getFilterQueryTag(), etmGroup.getFilterQuery(), true);
        builder.field(this.tags.getFilterQueryOccurrenceTag(), etmGroup.getFilterQueryOccurrence().name());
        builder.field(this.tags.getAlwaysShowCorrelatedEventsTag(), etmGroup.isAlwaysShowCorrelatedEvents());
        builder.field(this.tags.getLdapBaseTag(), etmGroup.isLdapBase());
        builder.field(this.tags.getRolesTag(), etmGroup.getRoles());
        builder.field(this.tags.getDashboardDatasourcesTag(), etmGroup.getDashboardDatasources());
        builder.field(this.tags.getSignalDatasourcesTag(), etmGroup.getSignalDatasources());
        builder.field(this.tags.getNotifiersTag(), etmGroup.getNotifiers());
        builder.endObject().endObject();
        return builder.build();
    }

    @Override
    public EtmGroup readGroup(String jsonContent) {
        return readGroup(this.converter.toMap(jsonContent));
    }

    public EtmPrincipal readPrincipal(Map<String, Object> valueMap) {
        valueMap = this.converter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, valueMap);
        EtmPrincipal principal = new EtmPrincipal(this.converter.getString(this.tags.getIdTag(), valueMap));
        principal.setPasswordHash(this.converter.getString(this.tags.getPasswordHashTag(), valueMap));
        principal.setName(this.converter.getString(this.tags.getNameTag(), valueMap));
        principal.setEmailAddress(this.converter.getString(this.tags.getEmailTag(), valueMap));
        principal.setApiKey(this.converter.getString(this.tags.getApiKeyTag(), valueMap));
        principal.setSecondaryApiKey(this.converter.getString(this.tags.getSecondaryApiKeyTag(), valueMap));
        principal.setFilterQuery(this.converter.getString(this.tags.getFilterQueryTag(), valueMap));
        principal.setFilterQueryOccurrence(QueryOccurrence.valueOf(this.converter.getString(this.tags.getFilterQueryOccurrenceTag(), valueMap)));
        principal.setAlwaysShowCorrelatedEvents(this.converter.getBoolean(this.tags.getAlwaysShowCorrelatedEventsTag(), valueMap));
        principal.setHistorySize(this.converter.getInteger(this.tags.getSearchHistorySizeTag(), valueMap, EtmPrincipal.DEFAULT_HISTORY_SIZE));
        principal.setDefaultSearchRange(this.converter.getLong(this.tags.getDefaultSearchRangeTag(), valueMap));
        principal.setChangePasswordOnLogon(this.converter.getBoolean(this.tags.getChangePasswordOnLogonTag(), valueMap, Boolean.FALSE));
        principal.setLdapBase(this.converter.getBoolean(this.tags.getLdapBaseTag(), valueMap, Boolean.FALSE));
        String value = this.converter.getString(this.tags.getLocaleTag(), valueMap);
        if (value != null) {
            principal.setLocale(Locale.forLanguageTag(value));
        }
        value = this.converter.getString(this.tags.getTimeZoneTag(), valueMap);
        if (value != null) {
            principal.setTimeZone(TimeZone.getTimeZone(value));
        }
        List<String> roles = this.converter.getArray(this.tags.getRolesTag(), valueMap);
        if (roles != null) {
            principal.addRoles(roles);
        }
        List<String> list = this.converter.getArray(this.tags.getDashboardDatasourcesTag(), valueMap);
        if (list != null) {
            principal.addDashboardDatasources(list);
        }
        list = this.converter.getArray(this.tags.getSignalDatasourcesTag(), valueMap);
        if (list != null) {
            principal.addSignalDatasources(list);
        }
        list = this.converter.getArray(this.tags.getNotifiersTag(), valueMap);
        if (list != null) {
            principal.addNotifiers(list);
        }
        // Add the dashboard names. These are readonly properties added by the DashboardService.
        List<Map<String, Object>> dashboards = this.converter.getArray(this.tags.getDashboardsTag(), valueMap);
        if (dashboards != null) {
            for (Map<String, Object> dashboard : dashboards) {
                principal.addDashboard(this.converter.getString(this.tags.getNameTag(), dashboard));
            }
        }
        return principal;
    }

    public EtmGroup readGroup(Map<String, Object> valueMap) {
        valueMap = this.converter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP, valueMap);
        EtmGroup group = new EtmGroup(this.converter.getString(this.tags.getNameTag(), valueMap));
        group.setDisplayName(this.converter.getString(this.tags.getDisplayNameTag(), valueMap));
        group.setFilterQuery(this.converter.getString(this.tags.getFilterQueryTag(), valueMap));
        group.setFilterQueryOccurrence(QueryOccurrence.valueOf(this.converter.getString(this.tags.getFilterQueryOccurrenceTag(), valueMap)));
        group.setAlwaysShowCorrelatedEvents(this.converter.getBoolean(this.tags.getAlwaysShowCorrelatedEventsTag(), valueMap));
        group.setLdapBase(this.converter.getBoolean(this.tags.getLdapBaseTag(), valueMap, Boolean.FALSE));
        List<String> roles = this.converter.getArray(this.tags.getRolesTag(), valueMap);
        if (roles != null) {
            group.addRoles(roles);
        }
        List<String> list = this.converter.getArray(this.tags.getDashboardDatasourcesTag(), valueMap);
        if (list != null) {
            group.addDashboardDatasources(list);
        }
        list = this.converter.getArray(this.tags.getSignalDatasourcesTag(), valueMap);
        if (list != null) {
            group.addSignalDatasources(list);
        }
        list = this.converter.getArray(this.tags.getNotifiersTag(), valueMap);
        if (list != null) {
            group.addNotifiers(list);
        }
        // Add the dashboard names. These are readonly properties added by the DashboardService.
        List<Map<String, Object>> dashboards = this.converter.getArray(this.tags.getDashboardsTag(), valueMap);
        if (dashboards != null) {
            for (Map<String, Object> dashboard : dashboards) {
                group.addDashboard(this.converter.getString(this.tags.getNameTag(), dashboard));
            }
        }
        return group;
    }

    @Override
    public EtmPrincipalTags getTags() {
        return this.tags;
    }

}
