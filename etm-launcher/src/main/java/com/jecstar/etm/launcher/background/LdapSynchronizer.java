package com.jecstar.etm.launcher.background;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalTagsJsonImpl;
import com.jecstar.etm.server.core.ldap.Directory;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.ObjectUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.HashMap;
import java.util.Map;

/**
 * This class synchronizes all changes from LDAP to the internal user repository.
 * <p>
 * When the LDAP cannot be reached, no actions will be taken. This way we prevent accidentally removal of accounts in case
 * of a network outage.
 */
public class LdapSynchronizer extends AbstractJsonService implements Runnable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(LdapSynchronizer.class);

    private final EtmConfiguration etmConfiguration;
    private final Client client;
    private final EtmPrincipalTags tags = new EtmPrincipalTagsJsonImpl();

    public LdapSynchronizer(final EtmConfiguration etmConfiguration, final Client client) {
        this.etmConfiguration = etmConfiguration;
        this.client = client;
    }

    @Override
    public void run() {
        Directory directory = this.etmConfiguration.getDirectory();
        if (directory == null) {
            return;
        }
        if (!directory.isConnected()) {
            return;
        }
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Start synchronizing LDAP directory.");
        }
        try {
            SearchRequestBuilder searchRequestBuilder = enhanceRequest(client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME), etmConfiguration)
                    .setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE)
                    .setFetchSource(new String[]{
                            ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.tags.getIdTag(),
                            ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.tags.getNameTag(),
                            ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.tags.getEmailTag(),
                            ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.tags.getLdapBaseTag()
                    }, null)
                    .setQuery(
                            QueryBuilders.boolQuery().must(
                                    QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)
                            ).must(
                                    QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.tags.getLdapBaseTag(), true)
                            )
                    );
            ScrollableSearch scrollableSearch = new ScrollableSearch(client, searchRequestBuilder);
            if (!scrollableSearch.hasNext()) {
                return;
            }
            for (SearchHit searchHit : scrollableSearch) {
                Map<String, Object> values = toMapWithoutNamespace(searchHit.getSourceAsMap(), ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
                if (!getBoolean(this.tags.getLdapBaseTag(), values, false)) {
                    continue;
                }
                String userId = getString(this.tags.getIdTag(), values);
                EtmPrincipal principal = directory.getPrincipal(userId, false);
                if (principal == null) {
                    if (log.isInfoLevelEnabled()) {
                        log.logInfoMessage("User with id '" + userId + "' no longer found in LDAP directory. Removing user.");
                    }
                    enhanceRequest(
                            this.client.prepareDelete(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId),
                            etmConfiguration
                    ).get();
                } else {
                    updateLdapPrincipalWhenChanged(values, principal);
                }
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to synchronize with LDAP server", e);
            }
        }
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Finished synchronizing LDAP directory.");
        }
    }

    private void updateLdapPrincipalWhenChanged(Map<String, Object> values, EtmPrincipal ldapPrincipal) {
        boolean changed = false;
        if (!ObjectUtils.equalsNullProof(getString(this.tags.getNameTag(), values), ldapPrincipal.getName())) {
            changed = true;
        }
        if (!ObjectUtils.equalsNullProof(getString(this.tags.getEmailTag(), values), ldapPrincipal.getEmailAddress())) {
            changed = true;
        }
        if (changed) {
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("User with id '" + ldapPrincipal.getId() + "' changed in LDAP directory. Updating user.");
            }
            Map<String, Object> updateMap = new HashMap<>();
            Map<String, Object> userObject = new HashMap<>();
            userObject.put(this.tags.getNameTag(), ldapPrincipal.getName());
            userObject.put(this.tags.getEmailTag(), ldapPrincipal.getEmailAddress());
            updateMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, userObject);
            enhanceRequest(
                    client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + ldapPrincipal.getId()),
                    etmConfiguration
            )
                    .setDoc(updateMap)
                    .setDetectNoop(true)
                    .get();
        }

    }
}
