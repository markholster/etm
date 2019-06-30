package com.jecstar.etm.server.core.ldap;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.cluster.certificate.Usage;
import com.jecstar.etm.server.core.domain.configuration.LdapConfiguration;
import com.jecstar.etm.server.core.domain.configuration.LdapConfiguration.ConnectionSecurity;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.tls.ElasticBackedTrustManager;
import org.ldaptive.*;
import org.ldaptive.auth.*;
import org.ldaptive.auth.ext.PasswordPolicyAuthenticationResponseHandler;
import org.ldaptive.control.PasswordPolicyControl;
import org.ldaptive.pool.*;
import org.ldaptive.ssl.DefaultHostnameVerifier;
import org.ldaptive.ssl.SslConfig;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Directory implements AutoCloseable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(Directory.class);

    private static final Duration CONNECTION_TIMEOUT = Duration.ofMillis(2500);

    private final Pattern attributePattern = Pattern.compile("\\{(.*?)}");
    private final DataRepository dataRepository;
    private LdapConfiguration ldapConfiguration;
    private AbstractConnectionPool connectionPool;
    private PooledConnectionFactory connectionFactory;

    public Directory(DataRepository dataRepository, LdapConfiguration ldapConfiguration) {
        this.dataRepository = dataRepository;
        this.ldapConfiguration = ldapConfiguration;
    }

    public void close() {
        if (this.connectionPool == null) {
            return;
        }
        this.connectionPool.close();
        this.connectionPool = null;
        this.connectionFactory = null;
    }

    public void test() {
        ConnectionConfig connectionConfig = createConnectionConfig(this.ldapConfiguration);
        AbstractConnectionPool connectionPool = createConnectionPool(this.ldapConfiguration.getMinPoolSize(), this.ldapConfiguration.getMaxPoolSize(), this.ldapConfiguration, connectionConfig);
        try {
            connectionPool.initialize();
        } finally {
            if (connectionPool.isInitialized()) {
                connectionPool.close();
            }
        }
    }

    public boolean isConnected() {
        return this.connectionPool != null && this.connectionPool.availableCount() > 0;
    }

    /**
     * Authenticates a user and attaches the <code>EtmGroup</code>s the user is
     * a member of.
     * <p>
     * Neither the <code>EtmPrincipal</code> nor the
     * <code>EtmGroups</code> are fully initialized and need to be loaded from
     * the database after calling this method.
     *
     * @param userId   The userId to authenticate.
     * @param password The password used to authenticate.
     * @return An uninitialized <code>EtmPrincipal</code> or null if the authentication fails.
     */
    public EtmPrincipal authenticate(String userId, String password) {
        if (!checkConnected()) {
            return null;
        }
        SearchDnResolver dnResolver = new SearchDnResolver(this.connectionFactory);
        dnResolver.setBaseDn(this.ldapConfiguration.getUserBaseDn() == null ? "" : this.ldapConfiguration.getUserBaseDn());
        dnResolver.setUserFilter(this.ldapConfiguration.getUserSearchFilter());
        dnResolver.setSubtreeSearch(this.ldapConfiguration.isUserSearchInSubtree());

        PooledBindAuthenticationHandler authHandler = new PooledBindAuthenticationHandler(this.connectionFactory);
        authHandler.setAuthenticationControls(new PasswordPolicyControl());

        SearchEntryResolver searchEntryResolver = new SearchEntryResolver(this.connectionFactory);
        searchEntryResolver.setBaseDn(this.ldapConfiguration.getUserBaseDn() == null ? "" : this.ldapConfiguration.getUserBaseDn());
        searchEntryResolver.setSubtreeSearch(this.ldapConfiguration.isUserSearchInSubtree());

        Authenticator auth = new Authenticator(dnResolver, authHandler);
        auth.setEntryResolver(searchEntryResolver);
        auth.setAuthenticationResponseHandlers(new PasswordPolicyAuthenticationResponseHandler());

        AuthenticationResponse response;
        try {
            response = auth.authenticate(new AuthenticationRequest(userId, new Credential(password), createUserAttributes(this.ldapConfiguration).toArray(new String[0])));
        } catch (LdapException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
        if (response.getResult()) {
            LdapEntry ldapEntry = response.getLdapEntry();
            LdapAttribute attribute = ldapEntry.getAttribute(this.ldapConfiguration.getUserIdentifierAttribute());
            EtmPrincipal principal = new EtmPrincipal(attribute.getStringValue());
            principal.setLdapBase(true);
            setAttributeFromLdapEntity(this.ldapConfiguration.getUserFullNameAttribute(), ldapEntry, principal::setName);
            setAttributeFromLdapEntity(this.ldapConfiguration.getUserEmailAttribute(), ldapEntry, principal::setEmailAddress);
            attribute = ldapEntry.getAttribute(this.ldapConfiguration.getUserMemberOfGroupsAttribute());
            if (attribute != null) {
                addGroups(principal, attribute.getStringValues());
            } else {
                addGroups(this.ldapConfiguration, principal, ldapEntry);
            }
            return principal;
        } else {
            if (log.isDebugLevelEnabled()) {
                AccountState accountState = response.getAccountState();
                if (accountState != null && accountState.getError() != null) {
                    log.logDebugMessage("Failed to authenticate user '" + userId + "'. Error " + accountState.getError().getCode() + ": " + accountState.getError().getMessage());
                } else {
                    log.logDebugMessage("Failed to authenticate user '" + userId + "'.");
                }
            }
        }
        return null;
    }

    public Set<EtmGroup> getGroups() {
        Set<EtmGroup> groups = new HashSet<>();
        if (!checkConnected()) {
            return groups;
        }
        SearchExecutor executor = new SearchExecutor();
        executor.setBaseDn(this.ldapConfiguration.getGroupBaseDn() == null ? "" : this.ldapConfiguration.getGroupBaseDn());
        try {
            SearchResult searchResult = executor.search(this.connectionFactory, getGroupFilter(this.ldapConfiguration, "*")).getResult();
            for (LdapEntry entry : searchResult.getEntries()) {
                EtmGroup group = new EtmGroup(entry.getDn());
                group.setLdapBase(true);
                groups.add(group);
            }
        } catch (LdapException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
        return groups;
    }

    public EtmGroup getGroup(String groupDn) {
        if (!checkConnected()) {
            return null;
        }
        // Jikes, dunno how to query based on DN.
        Set<EtmGroup> groups = getGroups();
        groups.removeIf(etmGroup -> !groupDn.equals(etmGroup.getName()));
        if (groups.size() != 1) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("No ldap group found with DN '" + groupDn + "'");
            }
            return null;
        }
        return groups.iterator().next();
    }

    /**
     * Search for principals. This method does not fully load found
     * <code>EtmPrincipal</code>s but only sets the user id, full name and email address.
     *
     * @param query The query to execute.
     * @return A list with <code>EtmPrincipal</code>s.
     */
    public List<EtmPrincipal> searchPrincipal(String query) {
        return searchPrincipal(query, false);
    }

    /**
     * Gets a principals. This method does not fully load the found
     * <code>EtmPrincipal</code> and <code>EtmGroup</code>s but only sets the
     * user id, full name and email address. For the <code>EtmGroup</code>s only
     * the name and ldapBase are set.
     *
     * @param userId The id of the user to retrieve
     * @return The <code>EtmPrincipal</code> or <code>null</code> if not exactly
     * one principal is found.
     */
    public EtmPrincipal getPrincipal(String userId, boolean includeGroups) {
        List<EtmPrincipal> principals = searchPrincipal(userId, includeGroups);
        if (principals.size() != 1) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Expected a single principal, but found " + principals.size() + " principals with id '" + userId + "'.");
            }
            return null;
        }
        return principals.get(0);
    }

    private List<EtmPrincipal> searchPrincipal(String query, boolean includeGroups) {
        List<EtmPrincipal> principals = new ArrayList<>();
        if (!checkConnected()) {
            return principals;
        }
        SearchExecutor executor = new SearchExecutor();
        executor.setBaseDn(this.ldapConfiguration.getUserBaseDn() == null ? "" : this.ldapConfiguration.getUserBaseDn());
        executor.setSizeLimit(15);
        executor.setSortBehavior(SortBehavior.SORTED);
        executor.setSearchScope(this.ldapConfiguration.isUserSearchInSubtree() ? SearchScope.SUBTREE : SearchScope.ONELEVEL);
        try {
            SearchResult searchResult = executor.search(
                    this.connectionFactory,
                    getSearchFilter(this.ldapConfiguration, query),
                    createUserAttributes(this.ldapConfiguration).toArray(new String[0])
            ).getResult();
            for (LdapEntry ldapEntry : searchResult.getEntries()) {
                LdapAttribute attribute = ldapEntry.getAttribute(this.ldapConfiguration.getUserIdentifierAttribute());
                EtmPrincipal principal = new EtmPrincipal(attribute.getStringValue());
                principal.setLdapBase(true);
                setAttributeFromLdapEntity(this.ldapConfiguration.getUserFullNameAttribute(), ldapEntry, principal::setName);
                setAttributeFromLdapEntity(this.ldapConfiguration.getUserEmailAttribute(), ldapEntry, principal::setEmailAddress);
                principals.add(principal);
                if (includeGroups) {
                    attribute = ldapEntry.getAttribute(this.ldapConfiguration.getUserMemberOfGroupsAttribute());
                    if (attribute != null) {
                        addGroups(principal, attribute.getStringValues());
                    } else {
                        addGroups(this.ldapConfiguration, principal, ldapEntry);
                    }
                }
            }
        } catch (LdapException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
        return principals;
    }


    private boolean connect() {
        if (this.connectionPool != null) {
            return false;
        }
        try {
            ConnectionConfig connectionConfig = createConnectionConfig(this.ldapConfiguration);
            this.connectionPool = createConnectionPool(this.ldapConfiguration.getMinPoolSize(), this.ldapConfiguration.getMaxPoolSize(), this.ldapConfiguration, connectionConfig);
            this.connectionPool.initialize();
            this.connectionFactory = new PooledConnectionFactory(this.connectionPool);
            this.connectionFactory.getConnection().close();
            return true;
        } catch (Exception e) {
            if (this.connectionPool != null && this.connectionPool.isInitialized()) {
                this.connectionPool.close();
            }
            this.connectionPool = null;
            this.connectionFactory = null;
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unable to connect to ldap server.", e);
            }
        }
        return false;
    }

    private void hotReconnect(LdapConfiguration ldapConfiguration) {
        if (this.connectionPool == null) {
            this.ldapConfiguration = ldapConfiguration;
            connect();
        } else {
            ConnectionPool oldPool = this.connectionPool;
            ConnectionConfig connectionConfig = createConnectionConfig(ldapConfiguration);
            AbstractConnectionPool newConnectionPool = createConnectionPool(ldapConfiguration.getMinPoolSize(), ldapConfiguration.getMaxPoolSize(), ldapConfiguration, connectionConfig);
            newConnectionPool.initialize();

            this.connectionFactory = new PooledConnectionFactory(newConnectionPool);
            this.ldapConfiguration = ldapConfiguration;
            this.connectionPool = newConnectionPool;

            oldPool.close();
        }
    }

    private AbstractConnectionPool createConnectionPool(int minPoolSize, int maxPoolSize, LdapConfiguration ldapConfiguration, ConnectionConfig connectionConfig) {
        PoolConfig poolConfig = new PoolConfig();
        poolConfig.setMinPoolSize(minPoolSize);
        poolConfig.setMaxPoolSize(maxPoolSize);
        poolConfig.setValidateTimeout(CONNECTION_TIMEOUT);
        if (ldapConfiguration.getConnectionTestSearchFilter() != null) {
            poolConfig.setValidateOnCheckOut(true);
            // Also validate on connection checkin. This is necessary because when a user provides a wrong password the
            // connection in the connection pool is invalid. This wil result in a ValidationException the next time the
            // connection is used.
            poolConfig.setValidateOnCheckIn(true);
        }
        AbstractConnectionPool connectionPool = new BlockingConnectionPool(poolConfig, new DefaultConnectionFactory(connectionConfig));

        if (ldapConfiguration.getConnectionTestSearchFilter() != null) {
            SearchRequest connectionSearchRequest = new SearchRequest();
            connectionSearchRequest.setBaseDn(ldapConfiguration.getConnectionTestBaseDn() == null ? "" : ldapConfiguration.getConnectionTestBaseDn());
            connectionSearchRequest.setSearchFilter(new SearchFilter(ldapConfiguration.getConnectionTestSearchFilter()));
            connectionSearchRequest.setReturnAttributes(ReturnAttributes.NONE.value());
            connectionSearchRequest.setSearchScope(SearchScope.OBJECT);
            connectionSearchRequest.setSizeLimit(1);
            connectionPool.setValidator(new SearchValidator(connectionSearchRequest));
        }
        return connectionPool;
    }


    private boolean checkConnected() {
        // TODO doe dit 1 keer per minuut o.i.d.
        if (this.connectionPool == null) {
            return connect();
        }
        return true;
    }

    private ConnectionConfig createConnectionConfig(LdapConfiguration ldapConfiguration) {
        ConnectionConfig connectionConfig = new ConnectionConfig("ldap://" + ldapConfiguration.getHost() + ":" + ldapConfiguration.getPort());
        connectionConfig.setUseSSL(ConnectionSecurity.SSL_TLS.equals(ldapConfiguration.getConnectionSecurity()));
        connectionConfig.setUseStartTLS(ConnectionSecurity.STARTTLS.equals(ldapConfiguration.getConnectionSecurity()));
        connectionConfig.setConnectTimeout(CONNECTION_TIMEOUT);
        if (this.ldapConfiguration.getBindDn() != null) {
            connectionConfig.setConnectionInitializer(new BindConnectionInitializer(ldapConfiguration.getBindDn(), new Credential(ldapConfiguration.getBindPassword())));
        }
        if (connectionConfig.getUseSSL() || connectionConfig.getUseStartTLS()) {
            SslConfig sslConfig = new SslConfig(new ElasticBackedTrustManager(Usage.LDAP, this.dataRepository));
            sslConfig.setHostnameVerifier(new DefaultHostnameVerifier());
            connectionConfig.setSslConfig(sslConfig);
        }
        return connectionConfig;
    }

    private void setAttributeFromLdapEntity(String attributeName, LdapEntry ldapEntry, Consumer<String> consumer) {
        if (ldapEntry == null || attributeName == null) {
            return;
        }
        LdapAttribute attribute = ldapEntry.getAttribute(attributeName);
        if (attribute == null) {
            return;
        }
        consumer.accept(attribute.getStringValue());
    }

    private void addGroups(LdapConfiguration ldapConfiguration, EtmPrincipal principal, LdapEntry ldapEntry) {
        if (principal == null || ldapEntry == null || this.ldapConfiguration.getUserGroupsQueryFilter() == null) {
            return;
        }
        SearchExecutor executor = new SearchExecutor();
        executor.setBaseDn(ldapConfiguration.getUserGroupsQueryBaseDn() == null ? "" : ldapConfiguration.getUserGroupsQueryBaseDn());
        SearchFilter searchFilter = new SearchFilter(ldapConfiguration.getUserGroupsQueryFilter());
        Matcher matcher = this.attributePattern.matcher(ldapConfiguration.getUserGroupsQueryFilter());
        while (matcher.find()) {
            if ("dn".equalsIgnoreCase(matcher.group(1))) {
                searchFilter.setParameter("dn", ldapEntry.getDn());
            } else {
                LdapAttribute attribute = ldapEntry.getAttribute(matcher.group(1));
                if (attribute == null) {
                    continue;
                }
                searchFilter.setParameter(matcher.group(1), attribute.getStringValue());
            }
        }
        executor.setSearchFilter(searchFilter);
        Set<EtmGroup> availableEtmGroups = getGroups();
        try {
            SearchResult result = executor.search(this.connectionFactory).getResult();
            for (LdapEntry entry : result.getEntries()) {
                EtmGroup etmGroup = new EtmGroup(entry.getDn());
                if (!availableEtmGroups.contains(etmGroup)) {
                    continue;
                }
                etmGroup.setLdapBase(true);
                principal.addGroup(etmGroup);
            }
        } catch (LdapException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    private void addGroups(EtmPrincipal principal, Collection<String> groupDns) {
        if (principal == null || groupDns == null) {
            return;
        }
        Set<EtmGroup> availableEtmGroups = getGroups();
        for (String groupDn : groupDns) {
            EtmGroup etmGroup = new EtmGroup(groupDn);
            if (!availableEtmGroups.contains(etmGroup)) {
                continue;
            }
            etmGroup.setLdapBase(true);
            principal.addGroup(etmGroup);
        }
    }

    private List<String> createUserAttributes(LdapConfiguration ldapConfiguration) {
        List<String> attributes = new ArrayList<>();
        attributes.add(ldapConfiguration.getUserIdentifierAttribute());
        if (ldapConfiguration.getUserFullNameAttribute() != null) {
            attributes.add(ldapConfiguration.getUserFullNameAttribute());
        }
        if (ldapConfiguration.getUserEmailAttribute() != null) {
            attributes.add(ldapConfiguration.getUserEmailAttribute());
        }
        if (ldapConfiguration.getUserMemberOfGroupsAttribute() != null) {
            attributes.add(ldapConfiguration.getUserMemberOfGroupsAttribute());
        }
        if (ldapConfiguration.getUserGroupsQueryFilter() != null) {
            Matcher matcher = this.attributePattern.matcher(ldapConfiguration.getUserGroupsQueryFilter());
            while (matcher.find()) {
                if (!attributes.contains(matcher.group(1))) {
                    attributes.add(matcher.group(1));
                }
            }
        }
        return attributes;
    }

    private String getGroupFilter(LdapConfiguration ldapConfiguration, String groupName) {
        return ldapConfiguration.getGroupSearchFilter().replaceAll("\\{group}", groupName);
    }

    private String getSearchFilter(LdapConfiguration ldapConfiguration, String userName) {
        return ldapConfiguration.getUserSearchFilter().replaceAll("\\{user}", userName);
    }

    public synchronized void merge(LdapConfiguration ldapConfiguration) {
        if (ldapConfiguration == null) {
            return;
        }
        boolean reconnectNecessary = false;
        if (!Objects.equals(this.ldapConfiguration.getHost(), ldapConfiguration.getHost())) {
            reconnectNecessary = true;
        } else if (this.ldapConfiguration.getPort() != ldapConfiguration.getPort()) {
            reconnectNecessary = true;
        } else if (!Objects.equals(this.ldapConfiguration.getConnectionSecurity(), ldapConfiguration.getConnectionSecurity())) {
            reconnectNecessary = true;
        } else if (!Objects.equals(this.ldapConfiguration.getBindDn(), ldapConfiguration.getBindDn())) {
            reconnectNecessary = true;
        } else if (!Objects.equals(this.ldapConfiguration.getBindPassword(), ldapConfiguration.getBindPassword())) {
            reconnectNecessary = true;
        } else if (this.ldapConfiguration.getMinPoolSize() != ldapConfiguration.getMinPoolSize()) {
            reconnectNecessary = true;
        } else if (this.ldapConfiguration.getMaxPoolSize() != ldapConfiguration.getMaxPoolSize()) {
            reconnectNecessary = true;
        } else if (!Objects.equals(this.ldapConfiguration.getConnectionTestBaseDn(), ldapConfiguration.getConnectionTestBaseDn())) {
            reconnectNecessary = true;
        } else if (!Objects.equals(this.ldapConfiguration.getConnectionTestSearchFilter(), ldapConfiguration.getConnectionTestSearchFilter())) {
            reconnectNecessary = true;
        }
        this.ldapConfiguration = ldapConfiguration;
        if (reconnectNecessary) {
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("Detected a change in the configuration that needs a reconnect to the ldap server.");
            }
            try {
                hotReconnect(ldapConfiguration);
            } catch (Exception e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Failed to reconnect to the ldap server. Your ldap connection is in an unknown state.", e);
                }
            }
        }
    }

}
