package com.jecstar.etm.gui.rest.services;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract superclass for services that store their context state in a group or user. For example all dashboards and
 * signals are stored within the user or group entity.
 */
public class AbstractUserAttributeService extends AbstractIndexMetadataService {

    /**
     * Loads the given attributes from an entity.
     *
     * @param client     The elasticsearch client.
     * @param groupName  The name of the group to load the attributes from. If <code>null</code> the attributes of the current user will be loaded.
     * @param attributes The attributes to return.
     * @return A <code>Map</code> with attributes from the given entity.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getEntity(Client client, String groupName, String... attributes) {
        String[] prefixedAttributes = new String[attributes.length];
        GetRequestBuilder builder;
        if (groupName != null) {
            if (!getEtmPrincipal().isInGroup(groupName)) {
                throw new EtmException(EtmException.UNAUTHORIZED);
            }
            builder = client.prepareGet(
                    ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                    ElasticsearchLayout.ETM_DEFAULT_TYPE,
                    ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName
            );
            for (int i = 0; i < attributes.length; i++) {
                prefixedAttributes[i] = ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP + "." + attributes[i];
            }
        } else {
            builder = client.prepareGet(
                    ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                    ElasticsearchLayout.ETM_DEFAULT_TYPE,
                    ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId()
            );
            for (int i = 0; i < attributes.length; i++) {
                prefixedAttributes[i] = ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + attributes[i];
            }
        }
        GetResponse getResponse = builder.setFetchSource(prefixedAttributes, null).get();
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
     * @param client           The elasticsearch client.
     * @param etmConfiguration The <code>EtmConfiguration</code> instance for this node.
     * @param groupName        The name of the group to update. If <code>null</code> the graphs of the current user will be loaded.
     * @param source           The contents of the user or group.
     */
    protected void updateEntity(Client client, EtmConfiguration etmConfiguration, String groupName, Map<String, Object> source) {
        Map<String, Object> objectMap = new HashMap<>();
        UpdateRequestBuilder builder;
        if (groupName != null) {
            if (!getEtmPrincipal().isInGroup(groupName)) {
                throw new EtmException(EtmException.UNAUTHORIZED);
            }
            builder = client.prepareUpdate(
                    ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                    ElasticsearchLayout.ETM_DEFAULT_TYPE,
                    ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName
            );
            objectMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP, source);
        } else {
            builder = client.prepareUpdate(
                    ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                    ElasticsearchLayout.ETM_DEFAULT_TYPE,
                    ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId()
            );
            objectMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, source);
        }
        enhanceRequest(builder, etmConfiguration)
                .setDoc(objectMap)
                .setDocAsUpsert(true)
                .get();
    }
}
