package com.jecstar.etm.launcher.http;

import com.jecstar.etm.gui.rest.services.search.DefaultSearchTemplates;
import com.jecstar.etm.server.core.domain.audit.LoginAuditLog;
import com.jecstar.etm.server.core.domain.audit.builder.LoginAuditLogBuilder;
import com.jecstar.etm.server.core.domain.audit.converter.AuditLogConverter;
import com.jecstar.etm.server.core.domain.audit.converter.json.LoginAuditLogConverterJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.BCrypt;
import com.jecstar.etm.server.core.util.DateUtils;
import com.jecstar.etm.server.core.util.ObjectUtils;
import io.undertow.security.idm.*;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ElasticsearchIdentityManager implements IdentityManager {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ElasticsearchIdentityManager.class);
	private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();
	
	private final Client client;
	private final EtmConfiguration etmConfiguration;
	private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
	private final EtmPrincipalTags etmPrincipalTags = etmPrincipalConverter.getTags();
	private final JsonConverter jsonConverter = new JsonConverter();
	private final AuditLogConverter<String, LoginAuditLog> auditLogConverter = new LoginAuditLogConverterJsonImpl();

	
	public ElasticsearchIdentityManager(Client client, EtmConfiguration etmConfiguration) {
		this.client = client;
		this.etmConfiguration = etmConfiguration;
	}

	@Override
	public Account verify(Account account) {
		EtmAccount etmAccount = (EtmAccount) account;
		if (System.currentTimeMillis() - etmAccount.getLastUpdated() > 60000 || etmAccount.getPrincipal().forceReload) {
			EtmPrincipal principal = loadPrincipal(etmAccount.getPrincipal().getId());
			if (principal == null) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Account with id '" + etmAccount.getPrincipal().getId() + "' not found. Account will be invalidated.");
				}
				return null;
			}
			// Copy the LDAP groups. We don't refresh them over here because that would cause a lot of ldap network traffic.
			for (EtmGroup etmGroup : etmAccount.getPrincipal().getGroups()) {
				if (etmGroup.isLdapBase()) {
					principal.addGroup(etmGroup);
				}
			}
			etmAccount = new EtmAccount(principal);
		}
		return etmAccount;
	}

	@Override
	public Account verify(String id, Credential credential) {
		String passwordHash = getPasswordHash(id);
		if (passwordHash != null) {
			if (!BCrypt.checkpw(new String(((PasswordCredential) credential).getPassword()), passwordHash)) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Invalid password for account with id '" + id + "'.");
				}
				logLoginAttempt(id, false);
				return null;
			}
			EtmPrincipal principal = loadPrincipal(id);
			EtmAccount etmAccount = new EtmAccount(principal);
			logLoginAttempt(id, true);
			return etmAccount;
		} else if (this.etmConfiguration.getDirectory() != null) {
			EtmPrincipal principal = this.etmConfiguration.getDirectory().authenticate(id, new String(((PasswordCredential) credential).getPassword()));
			if (principal == null) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Invalid password for account with id '" + id + "'.");
				}
				logLoginAttempt(id, false);
				return null;				
			}
			EtmPrincipal storedPrincipal = loadPrincipal(principal.getId());
			if (storedPrincipal == null) {
				createLdapUser(principal);
				storedPrincipal = loadPrincipal(id);
			} else { 
				updateLdapPrincipalWhenChanged(storedPrincipal, principal);
			}
			if (principal.getGroups() != null) {
				addLdapGroups(storedPrincipal, principal.getGroups());
			}
			EtmAccount etmAccount = new EtmAccount(storedPrincipal);
			logLoginAttempt(id, true);
			return etmAccount;
			
		}
		return null;
	}

	private void updateLdapPrincipalWhenChanged(EtmPrincipal storedPrincipal, EtmPrincipal principal) {
		boolean changed = false;
		if (!ObjectUtils.equalsNullProof(storedPrincipal.getName(), principal.getName())) {
			storedPrincipal.setName(principal.getName());
			changed = true;
		} 
		if (!ObjectUtils.equalsNullProof(storedPrincipal.getEmailAddress(), principal.getEmailAddress())) {
			storedPrincipal.setEmailAddress(principal.getEmailAddress());
			changed = true;
		}
		if (changed) {
			Map<String, Object> updateMap = new HashMap<>();
				updateMap.put(this.etmPrincipalTags.getNameTag(), storedPrincipal.getName());
				updateMap.put(this.etmPrincipalTags.getEmailTag(), storedPrincipal.getEmailAddress());
			client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + storedPrincipal.getId())
				.setDoc(updateMap)
				.setDetectNoop(true)
				.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
				.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
				.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
				.get();			
		}
		
	}

	private void createLdapUser(EtmPrincipal principal) {
		client.prepareIndex(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principal.getId())
		.setSource(this.etmPrincipalConverter.writePrincipal(principal), XContentType.JSON)
		.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
		.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
		.get();
	if (etmConfiguration.getMaxSearchTemplateCount() >= 3) {
		client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principal.getId())
			.setDoc(new DefaultSearchTemplates().toJson(), XContentType.JSON)
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();		
		}	
	}

	@Override
	public Account verify(Credential credential) {
		if (credential instanceof X509CertificateCredential) {
			X509Certificate certificate = ((X509CertificateCredential) credential).getCertificate();
			EtmPrincipal principal = loadPrincipal(certificate.getSerialNumber().toString());
			if (principal == null) {
				return null;
			}
			// TODO, de public key zou hier uitgelezen moeten worden, en vergeleken met hetgeen in de DB.
			boolean valid = BCrypt.checkpw(certificate.getIssuerX500Principal().getName(), principal.getPasswordHash());
			if (!valid) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Invalid password (issuer name) for certificate with serial number '" + certificate.getSerialNumber().toString() + "'.");
				}
				return null;
			}
			return new EtmAccount(principal);
		}
		return null;
	}
	
	private EtmPrincipal loadPrincipal(String userId) {
		GetResponse getResponse = this.client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId).get();
		if (!getResponse.isExists()) {
			return null;
		}
		EtmPrincipal principal = this.etmPrincipalConverter.readPrincipal(getResponse.getSourceAsString());
		Collection<String> groups = this.jsonConverter.getArray(this.etmPrincipalTags.getGroupsTag(), getResponse.getSource());
		if (groups != null && !groups.isEmpty()) {
			MultiGetRequestBuilder multiGetBuilder = this.client.prepareMultiGet();
			for (String group : groups) {
				multiGetBuilder.add(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + group);
			}
			MultiGetResponse multiGetResponse = multiGetBuilder.get();
			for (MultiGetItemResponse item : multiGetResponse) {
				EtmGroup etmGroup = this.etmPrincipalConverter.readGroup(item.getResponse().getSourceAsString());
				principal.addGroup(etmGroup);
			}
		}
		return principal;
	}
	
	private void addLdapGroups(EtmPrincipal etmPrincipal, Collection<EtmGroup> set) {
		if (set != null && !set.isEmpty()) {
			MultiGetRequestBuilder multiGetBuilder = this.client.prepareMultiGet();
			for (EtmGroup group : set) {
				multiGetBuilder.add(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + group.getName());
			}
			MultiGetResponse multiGetResponse = multiGetBuilder.get();
			for (MultiGetItemResponse item : multiGetResponse) {
				if (!item.isFailed() && item.getResponse().isExists() && !item.getResponse().isSourceEmpty()) {
					EtmGroup etmGroup = this.etmPrincipalConverter.readGroup(item.getResponse().getSourceAsString());
					if (etmGroup.isLdapBase()) {
						etmPrincipal.addGroup(etmGroup);
					}
				}
			}
		}		
	}
	
	private String getPasswordHash(String userId) {
		GetResponse getResponse = this.client.prepareGet(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
				.setFetchSource(new String[] {this.etmPrincipalTags.getPasswordHashTag(), this.etmPrincipalTags.getLdapBaseTag()}, null)
				.get();
		if (!getResponse.isExists()) {
			return null;
		}
		Map<String, Object> source = getResponse.getSource();
		boolean ldapBase = this.jsonConverter.getBoolean(this.etmPrincipalTags.getLdapBaseTag(), source, Boolean.FALSE);
		if (ldapBase) {
			return null;
		}
		return this.jsonConverter.getString(this.etmPrincipalTags.getPasswordHashTag(), source);
	}
	
    private ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
    	if (-1 == etmConfiguration.getWaitForActiveShards()) {
    		return ActiveShardCount.ALL;
    	} else if (0 == etmConfiguration.getWaitForActiveShards()) {
    		return ActiveShardCount.NONE;
    	}
    	return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }


	private void logLoginAttempt(String id, boolean success) {
		ZonedDateTime now = ZonedDateTime.now();
		LoginAuditLogBuilder auditLogBuilder = new LoginAuditLogBuilder().setTimestamp(now).setHandlingTime(now).setPrincipalId(id).setSuccess(success);
		this.client.prepareIndex(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now), ElasticsearchLayout.ETM_DEFAULT_TYPE)
			.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setSource(this.auditLogConverter.write(auditLogBuilder.build()), XContentType.JSON)
			.execute();
	}
	

}
