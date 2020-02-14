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

package com.jecstar.etm.server.core.domain.principal;

import com.jecstar.etm.server.core.domain.QueryOccurrence;

import java.io.Serializable;
import java.util.*;

public class EtmGroup implements EtmSecurityEntity, Serializable {

    /**
     * The serialVersionUID for this class.
     */
    private static final long serialVersionUID = 7152085459917438053L;

    private final String name;
    private String displayName;
    private final Set<String> roles = new HashSet<>();
    private String filterQuery = null;
    private QueryOccurrence filterQueryOccurrence = QueryOccurrence.MUST;
    private boolean alwaysShowCorrelatedEvents = false;
    private boolean ldapBase;
    private Set<String> dashboards = new HashSet<>();
    private Set<String> notifiers = new HashSet<>();

    private Set<String> dashboardDatasources = new HashSet<>();
    private Set<String> signalDatasources = new HashSet<>();

    public EtmGroup(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return "group";
    }

    @Override
    public String getId() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMostSpecificName() {
        return getDisplayName() == null ? getName() : getDisplayName();
    }

    public String getFilterQuery() {
        return this.filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public QueryOccurrence getFilterQueryOccurrence() {
        return this.filterQueryOccurrence;
    }

    public void setFilterQueryOccurrence(QueryOccurrence filterQueryOccurrence) {
        this.filterQueryOccurrence = filterQueryOccurrence;
    }

    public boolean isAlwaysShowCorrelatedEvents() {
        return this.alwaysShowCorrelatedEvents;
    }

    public void setAlwaysShowCorrelatedEvents(boolean alwaysShowCorrelatedEvents) {
        this.alwaysShowCorrelatedEvents = alwaysShowCorrelatedEvents;
    }

    public boolean isLdapBase() {
        return this.ldapBase;
    }

    public void setLdapBase(boolean ldapBase) {
        this.ldapBase = ldapBase;
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(this.roles);
    }

    private void addRole(String role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    public void addRoles(Collection<String> roles) {
        if (roles == null) {
            return;
        }
        for (String role : roles) {
            addRole(role);
        }
    }

    public boolean isInRole(String role) {
        return this.roles.contains(role);
    }

    public boolean isInAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInAllRoles(String... roles) {
        for (String role : roles) {
            if (!this.roles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isAuthorizedForDashboardDatasource(String datasourceName) {
        return this.dashboardDatasources.contains(datasourceName);
    }

    public boolean isAuthorizedForSignalDatasource(String datasourceName) {
        return this.signalDatasources.contains(datasourceName);
    }

    @Override
    public boolean isAuthorizedForNotifier(String notifierName) {
        return this.notifiers.contains(notifierName);
    }

    public Set<String> getDashboards() {
        return this.dashboards;
    }

    public void addDashboard(String dashboard) {
        this.dashboards.add(dashboard);
    }

    public void addDashboardDatasources(List<String> datasources) {
        if (datasources != null) {
            this.dashboardDatasources.addAll(datasources);
        }
    }

    public Set<String> getDashboardDatasources() {
        return this.dashboardDatasources;
    }

    public Set<String> getNotifiers() {
        return this.notifiers;
    }

    public void addNotifiers(List<String> notifiers) {
        if (notifiers != null) {
            this.notifiers.addAll(notifiers);
        }
    }


    public Set<String> getSignalDatasources() {
        return this.signalDatasources;
    }

    public void addSignalDatasources(List<String> datasources) {
        if (datasources != null) {
            this.signalDatasources.addAll(datasources);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EtmGroup && this.name.equals(((EtmGroup) obj).getName());
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
