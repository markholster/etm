package com.jecstar.etm.server.core.domain.configuration;

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
    private String host;
    private int port;
    private ConnectionSecurity connectionSecurity;
    private String bindDn;
    private String bindPassword;

    // connection pool
    private int minPoolSize = 2;
    private int maxPoolSize = 10;
    private String connectionTestBaseDn;
    private String connectionTestSearchFilter;

    // User filters
    private String userBaseDn;
    private String userSearchFilter;
    private boolean userSearchInSubtree;
    private String userIdentifierAttribute;
    private String userFullNameAttribute;
    private String userEmailAttribute;
    private String userMemberOfGroupsAttribute;
    private String userGroupsQueryBaseDn;
    private String userGroupsQueryFilter;

    // Group filters.
    private String groupBaseDn;
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
