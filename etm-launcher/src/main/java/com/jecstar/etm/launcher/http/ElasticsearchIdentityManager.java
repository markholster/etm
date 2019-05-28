package com.jecstar.etm.launcher.http;

import com.jecstar.etm.gui.rest.services.search.DefaultSearchTemplates;
import com.jecstar.etm.launcher.http.auth.ApiKeyCredentials;
import com.jecstar.etm.server.core.domain.audit.builder.LoginAuditLogBuilder;
import com.jecstar.etm.server.core.domain.audit.converter.LoginAuditLogConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.BCrypt;
import com.jecstar.etm.server.core.util.DateUtils;
import com.jecstar.etm.server.core.util.LruCache;
import io.undertow.security.idm.*;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ElasticsearchIdentityManager implements IdentityManager {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(ElasticsearchIdentityManager.class);
    private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();

    private final DataRepository dataRepository;
    private final EtmConfiguration etmConfiguration;
    private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags etmPrincipalTags = etmPrincipalConverter.getTags();
    private final JsonConverter jsonConverter = new JsonConverter();
    private final LoginAuditLogConverter auditLogConverter = new LoginAuditLogConverter();

    private final LruCache<ApiKeyCredentials, EtmAccount> apiKeyCache = new LruCache<>(50, 30_000L);

    public ElasticsearchIdentityManager(final DataRepository dataRepository, final EtmConfiguration etmConfiguration) {
        this.dataRepository = dataRepository;
        this.etmConfiguration = etmConfiguration;
    }

    @Override
    public Account verify(Account account) {
        var etmAccount = (EtmAccount) account;
        if (System.currentTimeMillis() - etmAccount.getLastUpdated() > 60000 || etmAccount.getPrincipal().forceReload) {
            var principal = loadPrincipal(etmAccount.getPrincipal().getId());
            if (principal == null) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Account with id '" + etmAccount.getPrincipal().getId() + "' not found. Account will be invalidated.");
                }
                return null;
            }
            if (this.etmConfiguration.getDirectory() != null && this.etmConfiguration.getDirectory().isConnected()) {
                var ldapPrincipal = this.etmConfiguration.getDirectory().getPrincipal(principal.getId(), true);
                addLdapGroups(principal, ldapPrincipal.getGroups());
            }
            etmAccount = new EtmAccount(principal);
        }
        return etmAccount;
    }

    @Override
    public Account verify(String id, Credential credential) {
        var passwordHash = getPasswordHash(id);
        if (passwordHash != null) {
            if (!BCrypt.checkpw(new String(((PasswordCredential) credential).getPassword()), passwordHash)) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Invalid password for account with id '" + id + "'.");
                }
                logLoginAttempt(id, false);
                return null;
            }
            var principal = loadPrincipal(id);
            if (principal == null) {
                return null;
            }
            var etmAccount = new EtmAccount(principal);
            logLoginAttempt(id, true);
            return etmAccount;
        } else if (this.etmConfiguration.getDirectory() != null) {
            var principal = this.etmConfiguration.getDirectory().authenticate(id, new String(((PasswordCredential) credential).getPassword()));
            if (principal == null) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Invalid password for account with id '" + id + "'.");
                }
                logLoginAttempt(id, false);
                return null;
            }
            var storedPrincipal = loadPrincipal(principal.getId());
            if (storedPrincipal == null) {
                createLdapUser(principal);
                storedPrincipal = loadPrincipal(id);
            } else {
                updateLdapPrincipalWhenChanged(storedPrincipal, principal);
            }
            addLdapGroups(storedPrincipal, principal.getGroups());
            var etmAccount = new EtmAccount(storedPrincipal);
            logLoginAttempt(id, true);
            return etmAccount;

        }
        return null;
    }

    private void updateLdapPrincipalWhenChanged(EtmPrincipal storedPrincipal, EtmPrincipal principal) {
        var changed = false;
        if (!Objects.equals(storedPrincipal.getName(), principal.getName())) {
            storedPrincipal.setName(principal.getName());
            changed = true;
        }
        if (!Objects.equals(storedPrincipal.getEmailAddress(), principal.getEmailAddress())) {
            storedPrincipal.setEmailAddress(principal.getEmailAddress());
            changed = true;
        }
        if (changed) {
            var updateMap = new HashMap<String, Object>();
            var userObject = new HashMap<String, Object>();
            userObject.put(this.etmPrincipalTags.getNameTag(), storedPrincipal.getName());
            userObject.put(this.etmPrincipalTags.getEmailTag(), storedPrincipal.getEmailAddress());
            updateMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, userObject);
            this.dataRepository.update(new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + storedPrincipal.getId())
                    .setDoc(updateMap)
                    .setDetectNoop(true)
                    .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                    .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                    .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount()));
        }
    }

    private void createLdapUser(EtmPrincipal principal) {
        this.dataRepository.index(new IndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principal.getId())
                .setSource(this.etmPrincipalConverter.writePrincipal(principal), XContentType.JSON)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout())));
        if (this.etmConfiguration.getMaxSearchTemplateCount() >= 3) {
            this.dataRepository.update(new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + principal.getId())
                    .setDoc(new DefaultSearchTemplates().toJson(), XContentType.JSON)
                    .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                    .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                    .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount()));
        }
    }

    @Override
    public Account verify(Credential credential) {
        if (credential instanceof ApiKeyCredentials) {
            var apiKeyCredentials = (ApiKeyCredentials) credential;
            var etmAccount = this.apiKeyCache.get(apiKeyCredentials);
            if (etmAccount != null) {
                return etmAccount;
            }
            var searchResponse = this.dataRepository.search(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                    .setFetchSource(false)
                    .setQuery(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getApiKeyTag(), apiKeyCredentials.getApiKey()))
                    .setSize(1)
            );
            if (searchResponse.getHits().getTotalHits().value == 0) {
                return null;
            }
            var docId = searchResponse.getHits().getAt(0).getId();
            var principal = loadPrincipal(docId.substring(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX.length()));
            if (principal == null) {
                return null;
            }
            etmAccount = new EtmAccount(principal);
            this.apiKeyCache.put(apiKeyCredentials, etmAccount);
            return etmAccount;
        } else if (credential instanceof X509CertificateCredential) {
            var certificate = ((X509CertificateCredential) credential).getCertificate();
            var principal = loadPrincipal(certificate.getSerialNumber().toString());
            if (principal == null) {
                return null;
            }
            // TODO, de public key zou hier uitgelezen moeten worden, en vergeleken met hetgeen in de DB.
            var valid = BCrypt.checkpw(certificate.getIssuerX500Principal().getName(), principal.getPasswordHash());
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

    @SuppressWarnings("unchecked")
    private EtmPrincipal loadPrincipal(String userId) {
        var getResponse = this.dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId));
        if (!getResponse.isExists()) {
            return null;
        }
        var principal = this.etmPrincipalConverter.readPrincipal(getResponse.getSourceAsString());
        var objectMap = getResponse.getSourceAsMap();
        var userMap = (Map<String, Object>) objectMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
        Collection<String> groups = this.jsonConverter.getArray(this.etmPrincipalTags.getGroupsTag(), userMap);
        if (groups != null && !groups.isEmpty()) {
            var multiGetBuilder = new MultiGetRequestBuilder();
            for (var group : groups) {
                multiGetBuilder.add(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + group);
            }
            var multiGetResponse = this.dataRepository.get(multiGetBuilder);
            for (var item : multiGetResponse) {
                EtmGroup etmGroup = this.etmPrincipalConverter.readGroup(item.getResponse().getSourceAsString());
                principal.addGroup(etmGroup);
            }
        }
        return principal;
    }

    private void addLdapGroups(EtmPrincipal etmPrincipal, Collection<EtmGroup> set) {
        if (set != null && !set.isEmpty()) {
            var multiGetBuilder = new MultiGetRequestBuilder();
            for (var group : set) {
                multiGetBuilder.add(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + group.getName());
            }
            var multiGetResponse = this.dataRepository.get(multiGetBuilder);
            for (var item : multiGetResponse) {
                if (!item.isFailed() && item.getResponse().isExists() && !item.getResponse().isSourceEmpty()) {
                    var etmGroup = this.etmPrincipalConverter.readGroup(item.getResponse().getSourceAsString());
                    if (etmGroup.isLdapBase()) {
                        etmPrincipal.addGroup(etmGroup);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String getPasswordHash(String userId) {
        var getResponse = this.dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
                .setFetchSource(new String[]{
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getPasswordHashTag(),
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getLdapBaseTag()}, null));
        if (!getResponse.isExists()) {
            return null;
        }
        var source = getResponse.getSource();
        var userObject = (Map<String, Object>) source.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
        var ldapBase = this.jsonConverter.getBoolean(this.etmPrincipalTags.getLdapBaseTag(), userObject, Boolean.FALSE);
        if (ldapBase) {
            return null;
        }
        return this.jsonConverter.getString(this.etmPrincipalTags.getPasswordHashTag(), userObject);
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
        var now = Instant.now();
        var auditLogBuilder = new LoginAuditLogBuilder().setTimestamp(now).setHandlingTime(now).setPrincipalId(id).setSuccess(success);
        this.dataRepository.indexAsync(new IndexRequestBuilder(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now))
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setSource(this.auditLogConverter.write(auditLogBuilder.build()), XContentType.JSON), DataRepository.noopActionListener());
    }


}
