package com.jecstar.etm.server.core.ldap;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.ReturnAttributes;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.SearchScope;
import org.ldaptive.SortBehavior;
import org.ldaptive.auth.AccountState;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.BindAuthenticationHandler;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.auth.SearchEntryResolver;
import org.ldaptive.auth.ext.PasswordPolicyAuthenticationResponseHandler;
import org.ldaptive.control.PasswordPolicyControl;
import org.ldaptive.pool.AbstractConnectionPool;
import org.ldaptive.pool.BlockingConnectionPool;
import org.ldaptive.pool.ConnectionPool;
import org.ldaptive.pool.PoolConfig;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SearchValidator;
import org.ldaptive.ssl.SslConfig;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.configuration.LdapConfiguration;
import com.jecstar.etm.server.core.configuration.LdapConfiguration.ConnectionSecurity;
import com.jecstar.etm.server.core.domain.EtmGroup;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.ssl.TrustAllTrustManager;
import com.jecstar.etm.server.core.util.ObjectUtils;

public class Directory implements AutoCloseable {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Directory.class);
	
	private static final Duration CONNECTION_TIMEOUT = Duration.ofMillis(2500);
	
	private final Pattern attributePattern = Pattern.compile("\\{(.*?)\\}");
	private LdapConfiguration ldapConfiguration;
	private AbstractConnectionPool connectionPool;
	private ConnectionFactory connectionFactory;
	
	public Directory(LdapConfiguration ldapConfiguration) {
		this.ldapConfiguration = ldapConfiguration;
	}

	public void close() {
		if (this.connectionPool == null) {
			return;
		}
		this.connectionPool.close();
		this.connectionPool = null;
		this.connectionFactory = null;
		return;
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
		return this.connectionPool != null;
	}
	
	/**
	 * Authenticates a user and attaches the <code>EtmGroup</code>s the user is
	 * a member of. 
	 * 
	 * Neither the <code>EtmPrincipal</code> nor the
	 * <code>EtmGroups</code> are fully initialized and need to be loaded from
	 * the database after calling this method.
	 * 
	 * @param userId The userId to authenticate.
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
		BindAuthenticationHandler authHandler = new BindAuthenticationHandler(this.connectionFactory);
		authHandler.setAuthenticationControls(new PasswordPolicyControl());
		Authenticator auth = new Authenticator(dnResolver, authHandler);
		SearchEntryResolver searchEntryResolver = new SearchEntryResolver(this.connectionFactory);
		searchEntryResolver.setBaseDn(this.ldapConfiguration.getUserBaseDn() == null ? "" : this.ldapConfiguration.getUserBaseDn());
		auth.setEntryResolver(searchEntryResolver);
		// TODO hier is ook een ActiveDirectoryResponseHandler mogelijk
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

	public List<EtmGroup> getGroups() {
		List<EtmGroup> groups = new ArrayList<>();
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
		List<EtmGroup> groups = getGroups();
		Iterator<EtmGroup> groupIterator = groups.iterator();
		while (groupIterator.hasNext()) {
			EtmGroup etmGroup = groupIterator.next();
			if (!groupDn.equals(etmGroup.getName())) {
				groupIterator.remove();
			}
		}
		if (groups.size() != 1) {
			// TODO logging
			return null;		
		}
		return groups.get(0);
	}

	/**
	 * Search for principals. This method does not fully load found
	 * <code>EtmPrincipal</code>s but only sets the user id, full name and email address.
	 * 
	 * @param query
	 *            The query to execute.
	 * @return A list with <code>EtmPrincipal</code>s.
	 */
	public List<EtmPrincipal> searchPrincipal(String query) {
		List<EtmPrincipal> principals = new ArrayList<>();
		if (!checkConnected()) {
			return principals;
		}	
		SearchExecutor executor = new SearchExecutor();
		executor.setBaseDn(this.ldapConfiguration.getUserBaseDn() == null ? "" : this.ldapConfiguration.getUserBaseDn());
		executor.setSizeLimit(15);
		executor.setSortBehavior(SortBehavior.SORTED);
		try {
			SearchResult searchResult = executor.search(
				this.connectionFactory, 
				getSearchFilter(this.ldapConfiguration, query), 
				new String[] {
					this.ldapConfiguration.getUserIdentifierAttribute(), 
					this.ldapConfiguration.getUserFullNameAttribute(),
					this.ldapConfiguration.getUserEmailAttribute()
				}
			).getResult();
			for (LdapEntry ldapEntry : searchResult.getEntries()) {
				LdapAttribute attribute = ldapEntry.getAttribute(this.ldapConfiguration.getUserIdentifierAttribute());
				EtmPrincipal principal = new EtmPrincipal(attribute.getStringValue());
				principal.setLdapBase(true);
				setAttributeFromLdapEntity(this.ldapConfiguration.getUserFullNameAttribute(), ldapEntry, principal::setName);
				setAttributeFromLdapEntity(this.ldapConfiguration.getUserEmailAttribute(), ldapEntry, principal::setEmailAddress);
				principals.add(principal);
			}
		} catch (LdapException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}		
		return principals;
	}
	
	public EtmPrincipal getPrincipal(String userId) {
		List<EtmPrincipal> principals = searchPrincipal(userId);
		if (principals.size() != 1) {
			// TODO logging
			return null;		
		}
		return principals.get(0);
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
			return true;
		} catch (Exception e) {
			if (this.connectionPool.isInitialized()) {
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
			this.connect();
		} else {
			ConnectionPool oldPool = this.connectionPool;
			ConnectionConfig connectionConfig = createConnectionConfig(ldapConfiguration);
			AbstractConnectionPool newConnectionPool = createConnectionPool(ldapConfiguration.getMinPoolSize(), ldapConfiguration.getMaxPoolSize(), ldapConfiguration, connectionConfig);
			newConnectionPool.initialize();
			ConnectionFactory newConnectionFactory = new PooledConnectionFactory(newConnectionPool);
			
			this.connectionFactory = newConnectionFactory;
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
			connect();
		}
		return this.connectionPool != null;
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
			// TODO trustmanager zou eigenlijk configurabel moeten zijn
			connectionConfig.setSslConfig(new SslConfig(new TrustAllTrustManager()));
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
		Matcher matcher = attributePattern.matcher(ldapConfiguration.getUserGroupsQueryFilter());
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
		try {
			SearchResult result = executor.search(this.connectionFactory).getResult();
			for (LdapEntry entry : result.getEntries()) {
				EtmGroup etmGroup = new EtmGroup(entry.getDn());
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
		for (String groupDn : groupDns) {
			EtmGroup etmGroup = new EtmGroup(groupDn);
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
		return this.ldapConfiguration.getGroupSearchFilter().replaceAll("\\{group\\}", groupName);
	}
	
	private String getSearchFilter(LdapConfiguration ldapConfiguration, String userName) {
		return this.ldapConfiguration.getUserSearchFilter().replaceAll("\\{user\\}", userName);
	}

	public synchronized void merge(LdapConfiguration ldapConfiguration) {
		if (ldapConfiguration == null) {
			return;
		}
		boolean reconnectNecessary= false;
		if (!ObjectUtils.equalsNullProof(this.ldapConfiguration.getHost(), ldapConfiguration.getHost())) {
			reconnectNecessary = true;
		} else if (this.ldapConfiguration.getPort() != ldapConfiguration.getPort()) {
			reconnectNecessary = true;
		} else if (!ObjectUtils.equalsNullProof(this.ldapConfiguration.getConnectionSecurity(), ldapConfiguration.getConnectionSecurity())) {
			reconnectNecessary = true;
		} else if (!ObjectUtils.equalsNullProof(this.ldapConfiguration.getBindDn(), ldapConfiguration.getBindDn())) {
			reconnectNecessary = true;
		} else if (!ObjectUtils.equalsNullProof(this.ldapConfiguration.getBindPassword(), ldapConfiguration.getBindPassword())) {
			reconnectNecessary = true;
		} else if (this.ldapConfiguration.getMinPoolSize() != ldapConfiguration.getMinPoolSize()) {
			reconnectNecessary = true;
		} else if (this.ldapConfiguration.getMaxPoolSize() != ldapConfiguration.getMaxPoolSize()) {
			reconnectNecessary = true;
		} else if (!ObjectUtils.equalsNullProof(this.ldapConfiguration.getConnectionTestBaseDn(), ldapConfiguration.getConnectionTestBaseDn())) {
			reconnectNecessary = true;
		} else if (!ObjectUtils.equalsNullProof(this.ldapConfiguration.getConnectionTestSearchFilter(), ldapConfiguration.getConnectionTestSearchFilter())) {
			reconnectNecessary = true;
		}
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
