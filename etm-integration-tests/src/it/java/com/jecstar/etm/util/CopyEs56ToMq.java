package com.jecstar.etm.util;

import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

/**
 * Utility class that loads the contents of an Elasticsearch database to MQ messages.
 */
public class CopyEs56ToMq {

    public static void main(String[] args) throws IOException, MQException {
        Hashtable<String, Object> connectionProperties = new Hashtable<>();
        connectionProperties.put(CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES_CLIENT);
        connectionProperties.put(CMQC.HOST_NAME_PROPERTY, "192.168.122.150");
        connectionProperties.put(CMQC.PORT_PROPERTY, 1414);
        connectionProperties.put(CMQC.CHANNEL_PROPERTY, "ETM.SVRCONN");
        MQQueueManager queueManager = new MQQueueManager("QM1", connectionProperties);
        MQQueue queue = queueManager.accessQueue("ETM.QUEUE.1", MQConstants.MQOO_FAIL_IF_QUIESCING + MQConstants.MQOO_OUTPUT);

        MQPutMessageOptions putMessageOptions = new MQPutMessageOptions();
        putMessageOptions.options = MQConstants.MQPMO_LOGICAL_ORDER + MQConstants.MQPMO_VERSION_2;

        RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create("http://127.0.0.1:9200"));
        RestHighLevelClient client = new RestHighLevelClient(restClientBuilder);
        DataRepository dataRepository = new DataRepository(client);

        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)
                .setQuery(QueryBuilders.matchAllQuery())
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setSort(SortBuilders.fieldSort("_doc"))
                .setFetchSource(true);


        ScrollableSearch searchHits = new ScrollableSearch(dataRepository, searchRequestBuilder);
        int i = 0;
        while (searchHits.hasNext()) {
            SearchHit searchHit = searchHits.next();
            Map<String, Object> valueMap = searchHit.getSourceAsMap();

            MQMessage message = new MQMessage();
            message.messageFlags = MQConstants.MQMF_SEGMENTATION_ALLOWED;
            message.persistence = MQConstants.MQPER_PERSISTENT;
            String data = (String) valueMap.get("payload");
            if (data != null) {
                message.write(data.getBytes());
            }
            queue.put(message, putMessageOptions);
            if (++i % 1000 == 0) {
                System.out.println(i);
                queueManager.commit();
            }
        }
        queueManager.commit();
        searchHits.clearScrollIds();
    }
}
