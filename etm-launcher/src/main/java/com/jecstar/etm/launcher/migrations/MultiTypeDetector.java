package com.jecstar.etm.launcher.migrations;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsAction;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;

/**
 * Class that detects the presence of multi mappings in a single event index.
 *
 * Detection of this fact makes executing queries more sophisticated because there's no need to query on the _type
 * attributes anymore.
 */
public class MultiTypeDetector {

    public void detect(Client client) {
        GetMappingsResponse mappingsResponse = new GetMappingsRequestBuilder(client, GetMappingsAction.INSTANCE, ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL).get();
        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappingsResponse.getMappings();
        for (ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> mappingsCursor : mappings) {
            for (ObjectObjectCursor<String, MappingMetaData> mappingMetadataCursor : mappingsCursor.value) {
                if (ElasticsearchLayout.ETM_DEFAULT_TYPE.equals(mappingMetadataCursor.key)) {
                    continue;
                }
                ElasticsearchLayout.OLD_EVENT_TYPES_PRESENT = true;
                return;
            }
        }
        ElasticsearchLayout.OLD_EVENT_TYPES_PRESENT = false;
    }
}
