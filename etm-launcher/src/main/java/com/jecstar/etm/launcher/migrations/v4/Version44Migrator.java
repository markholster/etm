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

package com.jecstar.etm.launcher.migrations.v4;

import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.converter.custom.PasswordConverter;
import com.jecstar.etm.server.core.domain.cluster.notifier.EmailNotifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.SnmpNotifier;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationTagsJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetIndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.IndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.persisting.RedactedSearchHit;
import com.jecstar.etm.server.core.util.BCrypt;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;

/**
 * Migrator that migrates the following things:
 * - Migrates base64 fields to encrypted passwords.
 */
public class Version44Migrator extends AbstractEtmMigrator {

    private final DataRepository dataRepository;
    private final String migrationIndexPrefix = "migetm_";

    public Version44Migrator(final DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldBeExecuted() {
        boolean indexExist = this.dataRepository.indicesExist(new GetIndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        if (!indexExist) {
            return false;
        }
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)
                .setFetchSource(true)
        );
        var currentNodeObject = getResponse.getSourceAsMap();
        var currentValues = (Map<String, Object>) currentNodeObject.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
        return !currentValues.containsKey(new EtmConfigurationTagsJsonImpl().getSecretHashTag());
    }

    @Override
    public void migrate(boolean forced) {
        if (!shouldBeExecuted() && !forced) {
            return;
        }
        checkAndCleanupPreviousRun(this.dataRepository, this.migrationIndexPrefix);

        boolean indexExist = this.dataRepository.indicesExist(new GetIndexRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME));
        if (!indexExist) {
            return;
        }
        Function<RedactedSearchHit, DocWriteRequest<?>> processor = searchHit -> {
            IndexRequestBuilder builder = new IndexRequestBuilder(
                    Version44Migrator.this.migrationIndexPrefix + searchHit.getIndex(), searchHit.getId()
            ).setSource(determineSource(searchHit));
            return builder.build();
        };
        var listener = new FailureDetectingBulkProcessorListener();
        var succeeded = migrateEtmConfiguration(this.dataRepository, createBulkProcessor(this.dataRepository, listener), listener, processor);
        if (!succeeded) {
            System.out.println("Errors detected. Quitting migration. Migrated indices are prefixed with '" + this.migrationIndexPrefix + "' and are still existent in your Elasticsearch cluster!");
            return;
        }
        refreshAndFlushIndices(this.dataRepository, this.migrationIndexPrefix + "*");

        deleteIndices(this.dataRepository, "old index", ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
        reindexTemporaryIndicesToNew(this.dataRepository, listener, this.migrationIndexPrefix);
        deleteIndices(this.dataRepository, "temporary indices", this.migrationIndexPrefix + "*");
        deleteTemporaryIndexTemplates(this.dataRepository, this.migrationIndexPrefix);
        checkAndCreateIndexExistence(this.dataRepository, ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
    }

    private boolean migrateEtmConfiguration(DataRepository dataRepository, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<RedactedSearchHit, DocWriteRequest<?>> processor) {
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.matchAllQuery())
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        return migrateEntity(searchRequestBuilder, "configuration", dataRepository, bulkProcessor, listener, processor);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> determineSource(RedactedSearchHit searchHit) {
        Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
        if (searchHit.getId().startsWith(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX)) {
            // Migrate IIB nodes
            var objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE);
            updateEncryption(objectMap, "password");
        } else if (searchHit.getId().startsWith(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX)) {
            // Migrate notifiers
            var objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER);
            updateEncryption(objectMap, EmailNotifier.PASSWORD);
            updateEncryption(objectMap, SnmpNotifier.PASSWORD);
            updateEncryption(objectMap, SnmpNotifier.SNMP_COMMUNITY);
            updateEncryption(objectMap, SnmpNotifier.SNMP_PRIVACY_PASSPHRASE);
        } else if (searchHit.getId().startsWith(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP_ID_PREFIX)) {
            // Migrate LDAP
            var objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LDAP);
            updateEncryption(objectMap, "bind_password");
        } else if (searchHit.getId().equals(ElasticsearchLayout.CONFIGURATION_OBJECT_ID_NODE_DEFAULT)) {
            var objectMap = (Map<String, Object>) sourceAsMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE);
            var hash = BCrypt.hashpw(EtmConfiguration.secret, BCrypt.gensalt());
            objectMap.put(new EtmConfigurationTagsJsonImpl().getSecretHashTag(), hash);
        }
        return sourceAsMap;
    }

    private void updateEncryption(Map<String, Object> objectMap, String key) {
        if (objectMap.containsKey(key)) {
            String value = objectMap.get(key).toString();
            if (value != null) {
                try {
                    objectMap.put(key, new PasswordConverter().encrypt(decodeBase64(value)));
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public String decodeBase64(String stringToDecode) {
        if (stringToDecode == null) {
            return null;
        }
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decoded = stringToDecode.getBytes();
        for (int i = 0; i < 7; i++) {
            decoded = decoder.decode(decoded);
        }
        return new String(decoded);
    }

}
