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
import java.security.Principal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EtmPrincipal implements EtmSecurityEntity, Principal, Serializable {

    public static final int DEFAULT_HISTORY_SIZE = 5;

    /**
     * The serialVersionUID for this class.
     */
    private static final long serialVersionUID = 2297816478265968473L;
    private final String id;
    private final Set<String> roles = new HashSet<>();
    private final Set<EtmGroup> groups = new HashSet<>();
    private Set<String> dashboards = new HashSet<>();
    private Set<String> notifiers = new HashSet<>();
    private Set<String> dashboardDatasources = new HashSet<>();
    private Set<String> signalDatasources = new HashSet<>();
    private Set<String> eventFieldGrants = new HashSet<>();
    private Set<String> eventFieldDenies = new HashSet<>();
    private String passwordHash;
    private String name = null;
    private String emailAddress = null;
    private String apiKey;
    private String secondaryApiKey;
    private Locale locale = Locale.getDefault();
    private TimeZone timeZone = TimeZone.getDefault();
    private String filterQuery = null;
    private QueryOccurrence filterQueryOccurrence = QueryOccurrence.MUST;
    private boolean alwaysShowCorrelatedEvents;
    private int historySize = DEFAULT_HISTORY_SIZE;
    private Long defaultSearchRange;
    private boolean changePasswordOnLogon;
    private boolean ldapBase;
    // State properties. DO NOT CHANGE!!
    public boolean forceReload = false;

    public EtmPrincipal(String id) {
        this.id = id;
    }

    public EtmPrincipal(String id, String passwordHash) {
        this(id);
        setPasswordHash(passwordHash);
    }

    @Override
    public String getType() {
        return "user";
    }

    @Override
    public String getName() {
        return this.name == null ? this.id : this.name;
    }

    @Override
    public String getDisplayName() {
        return this.name == null ? this.id : this.id + " (" + this.name + ")";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecondaryApiKey() {
        return this.secondaryApiKey;
    }

    public void setSecondaryApiKey(String apiKey) {
        this.secondaryApiKey = apiKey;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public Long getDefaultSearchRange() {
        return this.defaultSearchRange;
    }

    public void setDefaultSearchRange(Long defaultSearchRange) {
        this.defaultSearchRange = defaultSearchRange;
    }

    public void setLocale(Locale locale) {
        if (locale == null) {
            return;
        }
        this.locale = locale;
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        if (timeZone == null) {
            return;
        }
        this.timeZone = timeZone;
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(this.roles);
    }

    public void addRole(String role) {
        if (role != null && !this.roles.contains(role) && SecurityRoles.isValidRole(role)) {
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

    public Set<EtmGroup> getGroups() {
        return this.groups;
    }

    public void addGroup(EtmGroup group) {
        if (group != null) {
            this.groups.add(group);
        }
    }

    public void addGroups(Collection<EtmGroup> groups) {
        if (groups == null) {
            return;
        }
        for (EtmGroup group : groups) {
            addGroup(group);
        }
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

    public Set<String> getEventFieldGrants() {
        return this.eventFieldGrants;
    }

    public void addEventFieldGrants(List<String> eventFieldGrants) {
        if (eventFieldGrants != null) {
            this.eventFieldGrants.addAll(eventFieldGrants);
        }
    }

    public Set<String> getEventFieldDenies() {
        return this.eventFieldDenies;
    }

    public void addEventFieldDenies(List<String> eventFieldDenies) {
        if (eventFieldDenies != null) {
            this.eventFieldDenies.addAll(eventFieldDenies);
        }
    }

    public boolean isInRole(String role) {
        if (this.roles.contains(role)) {
            return true;
        }
        for (EtmGroup group : this.groups) {
            if (group.isInRole(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        for (EtmGroup group : this.groups) {
            if (group.isInAnyRole(roles)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAuthorizedForDashboardDatasource(String datasourceName) {
        if (this.dashboardDatasources.contains(datasourceName)) {
            return true;
        }
        for (EtmGroup group : this.groups) {
            if (group.isAuthorizedForDashboardDatasource(datasourceName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuthorizedForSignalDatasource(String datasourceName) {
        if (this.signalDatasources.contains(datasourceName)) {
            return true;
        }
        for (EtmGroup group : this.groups) {
            if (group.isAuthorizedForSignalDatasource(datasourceName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAuthorizedForNotifier(String notifierName) {
        if (this.notifiers.contains(notifierName)) {
            return true;
        }
        for (EtmGroup group : this.groups) {
            if (group.isAuthorizedForNotifier(notifierName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInGroup(String groupName) {
        for (EtmGroup group : this.groups) {
            if (group.getName().equals(groupName)) {
                return true;
            }
        }
        return false;
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

    public int getHistorySize() {
        return this.historySize;
    }

    public void setHistorySize(int historySize) {
        if (historySize < 0) {
            this.historySize = 0;
        }
        this.historySize = historySize;
    }

    public boolean isChangePasswordOnLogon() {
        return this.changePasswordOnLogon;
    }

    public void setChangePasswordOnLogon(boolean changePasswordOnLogon) {
        this.changePasswordOnLogon = changePasswordOnLogon;
    }

    public boolean isLdapBase() {
        return this.ldapBase;
    }

    public void setLdapBase(boolean ldapBase) {
        this.ldapBase = ldapBase;
    }

    /**
     * Gives the full path of fields that should be redacted.
     * <p>
     * An field must be redacted when it is present in the {@link #getEventFieldDenies()} or corresponding
     * {@link EtmGroup#getEventFieldDenies()} set and not present in the {@link #getEventFieldGrants()} set.
     *
     * @return A <code>Set</code> with attributes that should be redacted, or <code>null</code> when no such attribute exists.
     */
    public Set<String> getRedactedFields() {
        var denies = new HashSet<String>();
        if (getEventFieldDenies() != null) {
            denies.addAll(getEventFieldDenies());
        }
        for (EtmGroup group : this.groups) {
            var groupDenies = group.getEventFieldDenies();
            if (groupDenies != null) {
                denies.addAll(groupDenies);
            }
        }
        if (getEventFieldGrants() != null) {
            denies.removeAll(getEventFieldGrants());
        }
        if (denies.size() == 0) {
            return null;
        }
        return denies;
    }

    public NumberFormat getNumberFormat() {
        return NumberFormat.getInstance(getLocale());
    }

    public DateTimeFormatter getISO8601DateFormat() {
        return getDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    public DateTimeFormatter getDateFormat(String format) {
        return DateTimeFormatter.ofPattern(format).withLocale(getLocale()).withZone(getTimeZone().toZoneId());
    }

}
