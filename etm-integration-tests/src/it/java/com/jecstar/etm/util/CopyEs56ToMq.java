package com.jecstar.etm.util;

import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.jecstar.etm.gui.rest.services.ScrollableSearch;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Map;

/**
 * Utility class that loads the contents of an Elasticsearch database to MQ messages.
 */
public class CopyEs56ToMq {

    private static final String ES_CLUSTER_NAME = "etm2-acceptatie";

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

        Settings.Builder settingsBuilder = Settings.builder()
                .put("cluster.name", ES_CLUSTER_NAME)
                .put("client.transport.sniff", true);
        TransportClient client = new PreBuiltTransportClient(settingsBuilder.build())
                .addTransportAddress(new TransportAddress(InetAddress.getByName("127.0.0.1"), 9300));

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)
                .setQuery(QueryBuilders.matchAllQuery())
                .setTimeout(TimeValue.timeValueSeconds(30))
                .addSort(SortBuilders.fieldSort("_doc"))
                .setFetchSource(true);


        ScrollableSearch searchHits = new ScrollableSearch(client, searchRequestBuilder);
        int i=0;
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
