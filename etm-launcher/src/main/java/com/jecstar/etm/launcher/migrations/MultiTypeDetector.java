package com.jecstar.etm.launcher.migrations;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetIndexRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;

/**
 * Class that detects the presence of multi mappings in a single event index.
 * <p>
 * Detection of this fact makes executing queries more sophisticated because there's no need to query on the _type
 * attributes anymore.
 */
public class MultiTypeDetector {

    public void detect(DataRepository dataRepository) {
        boolean indicesExists = dataRepository.indicesExist(new GetIndexRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL));
        if (!indicesExists) {
            ElasticsearchLayout.OLD_EVENT_TYPES_PRESENT = false;
            return;
        }
        GetMappingsResponse mappingsResponse = dataRepository.indicesGetMappings(new GetMappingsRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL));
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
