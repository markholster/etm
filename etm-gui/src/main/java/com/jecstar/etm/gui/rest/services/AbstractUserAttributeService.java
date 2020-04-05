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

package com.jecstar.etm.gui.rest.services;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.UpdateRequestBuilder;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import org.elasticsearch.action.get.GetResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract superclass for services that store their context state in a group or user. For example all dashboards and
 * signals are stored within the user or group entity.
 */
public abstract class AbstractUserAttributeService extends AbstractIndexMetadataService {

    /**
     * Loads the given attributes from an entity.
     *
     * @param dataRepository The <code>DataRepository</code>.
     * @param groupName      The name of the group to load the attributes from. If <code>null</code> the attributes of the current user will be loaded.
     * @param attributes     The attributes to return.
     * @return A <code>Map</code> with attributes from the given entity.
     */
    protected Map<String, Object> getEntity(DataRepository dataRepository, String groupName, String... attributes) {
        String[] prefixedAttributes = new String[attributes.length];
        GetRequestBuilder builder;
        if (groupName != null) {
            if (!getEtmPrincipal().isInGroup(groupName)) {
                throw new EtmException(EtmException.UNAUTHORIZED);
            }
            builder = new GetRequestBuilder(
                    ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                    ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName
            );
            for (int i = 0; i < attributes.length; i++) {
                prefixedAttributes[i] = ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + attributes[i];
            }
        } else {
            builder = new GetRequestBuilder(
                    ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                    ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId()
            );
            for (int i = 0; i < attributes.length; i++) {
                prefixedAttributes[i] = ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + attributes[i];
            }
        }
        GetResponse getResponse = dataRepository.get(builder.setFetchSource(prefixedAttributes, null));
        if (getResponse.isSourceEmpty() || getResponse.getSourceAsMap().isEmpty()) {
            return new HashMap<>();
        }
        if (groupName != null) {
            return getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP, getResponse.getSourceAsMap());
        }
        return getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, getResponse.getSourceAsMap());
    }

    /**
     * Updates an entity.
     *
     * @param dataRepository   The <code>DataRepository</code>.
     * @param etmConfiguration The <code>EtmConfiguration</code> instance for this node.
     * @param groupName        The name of the group to update. If <code>null</code> the graphs of the current user will be loaded.
     * @param source           The contents of the user or group.
     */
    protected void updateEntity(DataRepository dataRepository, EtmConfiguration etmConfiguration, String groupName, Map<String, Object> source) {
        var objectMap = new HashMap<String, Object>();
        var builder = new UpdateRequestBuilder().setIndex(ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
        if (groupName != null) {
            if (!getEtmPrincipal().isInGroup(groupName)) {
                throw new EtmException(EtmException.UNAUTHORIZED);
            }
            builder.setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName);
            objectMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP, source);
        } else {
            builder.setId(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId());
            objectMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, source);
        }
        new RequestEnhancer(etmConfiguration).enhance(builder)
                .setDoc(objectMap)
                .setDocAsUpsert(true);
        dataRepository.update(builder);
    }

    /**
     * Merge a <code>Collection</code> instance into another <code>Collection</code> instance.
     *
     * @param sourceMap      The <code>Map</code> that contains the source <code>Collection</code>.
     * @param destinationMap The <code>Map</code> that contains the destination <code>Collection</code>.
     * @param attribute      The attribute that holds the key to the <code>Collection</code>s in both <code>Map</code>s.
     */
    protected void mergeCollectionInValueMap(Map<String, Object> sourceMap, Map<String, Object> destinationMap, String attribute) {
        Collection<Object> array = getArray(attribute, sourceMap, null);
        if (array == null) {
            return;
        }
        Collection<Object> destinationArray = getArray(attribute, destinationMap, null);
        if (destinationArray == null) {
            destinationMap.put(attribute, array);
        } else {
            for (Object item : array) {
                if (item != null && !destinationArray.contains(item)) {
                    destinationArray.add(item);
                }
            }
        }
    }

}
