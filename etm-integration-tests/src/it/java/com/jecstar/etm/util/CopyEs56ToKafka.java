package com.jecstar.etm.util;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

public class CopyEs56ToKafka {

    private static final String ES_CLUSTER_NAME = "etm2-acceptatie";

    public static void main(String[] args) throws UnknownHostException {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        KafkaProducer kafkaProducer = new KafkaProducer(props);

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
        int i = 1;
        while (searchHits.hasNext()) {
            SearchHit searchHit = searchHits.next();
            kafkaProducer.send(new ProducerRecord<>("etm-events", searchHit.getId(), "{ \"type\": \"messaging\", \"data\": " + searchHit.getSourceAsString() + "}"));
            if (i % 1000 == 0) {
                System.out.println(i);
            }
            if (i == 500_000) {
                break;
            }
            i++;
        }
        kafkaProducer.close();
        searchHits.clearScrollIds();
    }
}
