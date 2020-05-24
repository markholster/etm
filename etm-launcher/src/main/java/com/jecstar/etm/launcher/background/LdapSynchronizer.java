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

package com.jecstar.etm.launcher.background;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalTagsJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.DeleteRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.UpdateRequestBuilder;
import com.jecstar.etm.server.core.ldap.Directory;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.server.core.rest.AbstractJsonService;
import com.jecstar.etm.server.core.util.ObjectUtils;
import org.elasticsearch.index.query.QueryBuilders;

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
    private final DataRepository dataRepository;
    private final EtmPrincipalTags tags = new EtmPrincipalTagsJsonImpl();
    private final RequestEnhancer requestEnhancer;

    public LdapSynchronizer(final EtmConfiguration etmConfiguration, final DataRepository dataRepository) {
        this.etmConfiguration = etmConfiguration;
        this.dataRepository = dataRepository;
        this.requestEnhancer = new RequestEnhancer(etmConfiguration);
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
            SearchRequestBuilder searchRequestBuilder = this.requestEnhancer.enhance(new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME))
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
            ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder, null);
            if (!scrollableSearch.hasNext()) {
                return;
            }
            for (var searchHit : scrollableSearch) {
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
                    this.dataRepository.delete(this.requestEnhancer.enhance(
                            new DeleteRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + userId)
                    ));
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
            this.dataRepository.update(this.requestEnhancer.enhance(
                    new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + ldapPrincipal.getId())
                    )
                            .setDoc(updateMap)
                            .setDetectNoop(true)
            );
        }

    }
}
