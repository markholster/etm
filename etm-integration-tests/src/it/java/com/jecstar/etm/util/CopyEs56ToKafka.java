package com.jecstar.etm.util;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;

import java.util.Properties;

public class CopyEs56ToKafka {


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
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

        RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create("http://127.0.0.1:9200"));
        RestHighLevelClient client = new RestHighLevelClient(restClientBuilder);
        DataRepository dataRepository = new DataRepository(client);

        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)
                .setQuery(QueryBuilders.matchAllQuery())
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setSort(SortBuilders.fieldSort("_doc"))
                .setFetchSource(true);


        ScrollableSearch searchHits = new ScrollableSearch(dataRepository, searchRequestBuilder);
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
