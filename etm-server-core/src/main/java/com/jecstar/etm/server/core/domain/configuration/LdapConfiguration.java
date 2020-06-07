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

package com.jecstar.etm.server.core.domain.configuration;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.JsonNamespace;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;
import com.jecstar.etm.server.core.converter.custom.PasswordConverter;

@JsonNamespace(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP)
public class LdapConfiguration {

    public enum ConnectionSecurity {

        SSL_TLS, STARTTLS;

        public static ConnectionSecurity safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return ConnectionSecurity.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    // Connection settings
    @JsonField("host")
    private String host;
    @JsonField("port")
    private int port;
    @JsonField(value = "connection_security", converterClass = EnumConverter.class)
    private ConnectionSecurity connectionSecurity;
    @JsonField("bind_dn")
    private String bindDn;
    @JsonField(value = "bind_password", converterClass = PasswordConverter.class)
    private String bindPassword;

    // connection pool
    @JsonField("min_pool_size")
    private int minPoolSize = 2;
    @JsonField("max_pool_size")
    private int maxPoolSize = 10;
    @JsonField("connection_test_base_dn")
    private String connectionTestBaseDn;
    @JsonField("connection_test_search_filter")
    private String connectionTestSearchFilter;

    // User filters
    @JsonField("user_base_dn")
    private String userBaseDn;
    @JsonField("user_search_filter")
    private String userSearchFilter;
    @JsonField("user_search_in_subtree")
    private boolean userSearchInSubtree;
    @JsonField("user_identifier_attribute")
    private String userIdentifierAttribute;
    @JsonField("user_full_name_attribute")
    private String userFullNameAttribute;
    @JsonField("user_email_attribute")
    private String userEmailAttribute;
    @JsonField("user_member_of_groups_attribute")
    private String userMemberOfGroupsAttribute;
    @JsonField("user_groups_query_base_dn")
    private String userGroupsQueryBaseDn;
    @JsonField("user_groups_query_filter")
    private String userGroupsQueryFilter;

    // Group filters.
    @JsonField("group_base_dn")
    private String groupBaseDn;
    @JsonField("group_search_filter")
    private String groupSearchFilter;

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ConnectionSecurity getConnectionSecurity() {
        return this.connectionSecurity;
    }

    public void setConnectionSecurity(ConnectionSecurity connectionSecurity) {
        this.connectionSecurity = connectionSecurity;
    }

    public String getBindDn() {
        return this.bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindPassword() {
        return this.bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

    public int getMinPoolSize() {
        return this.minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        if (minPoolSize > 1) {
            this.minPoolSize = minPoolSize;
        }
    }

    public int getMaxPoolSize() {
        return this.maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        if (maxPoolSize > 2) {
            this.maxPoolSize = maxPoolSize;
        }
    }

    public String getConnectionTestBaseDn() {
        return this.connectionTestBaseDn;
    }

    public void setConnectionTestBaseDn(String connectionTestBaseDn) {
        this.connectionTestBaseDn = connectionTestBaseDn;
    }

    public String getConnectionTestSearchFilter() {
        return this.connectionTestSearchFilter;
    }

    public void setConnectionTestSearchFilter(String connectionTestSearchFilter) {
        this.connectionTestSearchFilter = connectionTestSearchFilter;
    }

    public String getUserBaseDn() {
        return this.userBaseDn;
    }

    public void setUserBaseDn(String userBaseDn) {
        this.userBaseDn = userBaseDn;
    }

    public String getUserSearchFilter() {
        return this.userSearchFilter;
    }

    public void setUserSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
    }

    public boolean isUserSearchInSubtree() {
        return this.userSearchInSubtree;
    }

    public void setUserSearchInSubtree(boolean userSearchInSubtree) {
        this.userSearchInSubtree = userSearchInSubtree;
    }

    public String getUserIdentifierAttribute() {
        return this.userIdentifierAttribute;
    }

    public void setUserIdentifierAttribute(String userIdentifierAttribute) {
        this.userIdentifierAttribute = userIdentifierAttribute;
    }

    public String getUserFullNameAttribute() {
        return this.userFullNameAttribute;
    }

    public void setUserFullNameAttribute(String userFullNameAttribute) {
        this.userFullNameAttribute = userFullNameAttribute;
    }

    public String getUserEmailAttribute() {
        return this.userEmailAttribute;
    }

    public void setUserEmailAttribute(String userEmailAttribute) {
        this.userEmailAttribute = userEmailAttribute;
    }

    public String getUserMemberOfGroupsAttribute() {
        return this.userMemberOfGroupsAttribute;
    }

    public void setUserMemberOfGroupsAttribute(String userMemberOfGroupsAttribute) {
        this.userMemberOfGroupsAttribute = userMemberOfGroupsAttribute;
    }

    public String getUserGroupsQueryBaseDn() {
        return this.userGroupsQueryBaseDn;
    }

    public void setUserGroupsQueryBaseDn(String userGroupsQueryBaseDn) {
        this.userGroupsQueryBaseDn = userGroupsQueryBaseDn;
    }

    public String getUserGroupsQueryFilter() {
        return this.userGroupsQueryFilter;
    }

    public void setUserGroupsQueryFilter(String userGroupsQueryFilter) {
        this.userGroupsQueryFilter = userGroupsQueryFilter;
    }

    public String getGroupBaseDn() {
        return this.groupBaseDn;
    }

    public void setGroupBaseDn(String groupBaseDn) {
        this.groupBaseDn = groupBaseDn;
    }

    public String getGroupSearchFilter() {
        return this.groupSearchFilter;
    }

    public void setGroupSearchFilter(String groupSearchFilter) {
        this.groupSearchFilter = groupSearchFilter;
    }
}
