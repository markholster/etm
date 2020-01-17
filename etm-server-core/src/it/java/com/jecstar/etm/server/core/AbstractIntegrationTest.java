package com.jecstar.etm.server.core;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.function.BiConsumer;

/**
 * Super class for all integration tests. This class requires a running
 * Elasticsearch instance on localhost:9300. ETM templates and scripts should be
 * configured on that instance as well.
 *
 * @author Mark Holster
 */
public abstract class AbstractIntegrationTest {

    protected final EtmConfiguration etmConfiguration = new EtmConfiguration("integration-test");
    protected BulkProcessor bulkProcessor;
    private DataRepository dataRepository;

    protected abstract BulkProcessor.Listener createBulkListener();

    @BeforeEach
    public void setup() {
        this.etmConfiguration.setEventBufferSize(1);
        this.etmConfiguration.setLicenseKey("cqPQ3EgKLBGrYUJWZTyPVsFYCvlsvzWSr1oDqm8PV64AmtF7xshSDrnDUDgWG+0NCj9II5mzBp38ArEOPawJkU9evYUBkrBZqSCZAFdxMQRIb9kmJ/hOmxQOw1s3H92/cQ9hE3q+IxVV6WgaUoZ3OWnLJvTcOhWOqBCR4dwWbbEr43+0v0bQhEEPywP41htuWOjBFN3EY4dMELyoYM8U0qyibODlPNeMEpt/5a9+U2gU+CaVG/X8Ntr3i7CFijTXj4Smv71mhUxwjBC5+2JhXDvfEJ1OOP9CBN+XkcYVm51MVw6rZrovjHfxmlNly8hg9ZQ4r0ylG2QoAnNrZFE8+8vfkoDPQ6W7QympijK82srOyudm/NQ1geaLdfqxw5zmKYQOOZJG36bzXmJgfan61Oqv5NG46Got4Z5oVhBvbY6lDjofjGaFuYzwKnTiIDIyVxvlQo8ZIvCdtegw6fJRwDUtTiShptLS/pAsYA6PGjyrsjFro/n6om4gTk7fLeM9Z+0ulkMAFBIPoc2f+Im0c8BJ/cu9ZOy6jcv9o8kDQx3EJy6GUnsSBt0L2m7tQAanUFFBfpOn7S2iLbGXkESo9AjusuJV76hFZ25Zy/EvDYv7qR/qnf3rrLdnzjgz3QYQkMt3Az/UC5XsKT9qsQz3YrsIocMNmfHxsT3SukoTfsM=");
        RestHighLevelClient highLevelClient = new RestHighLevelClient(RestClient.builder(HttpHost.create("127.0.0.1:9200")));
        this.dataRepository = new DataRepository(highLevelClient);

        BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
                (request, bulkListener) ->
                        highLevelClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener);
        this.bulkProcessor = BulkProcessor.builder(bulkConsumer, createBulkListener())
                .setBulkActions(1)
                .build();
    }

    @AfterEach
    public void tearDown() {
        if (this.bulkProcessor != null) {
            this.bulkProcessor.close();
        }
    }

    protected GetResponse waitFor(String index, String id) throws InterruptedException {
        return waitFor(index, id, null);
    }

    protected GetResponse waitFor(String index, String id, Long version) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        do {
            GetResponse getResponse = this.dataRepository.get(new GetRequestBuilder(index, id));
            if (getResponse.isExists()) {
                if (version == null || getResponse.getVersion() == version) {
                    return getResponse;
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedException();
            }
        } while (System.currentTimeMillis() - startTime < 10_000);
        throw new NoSuchEventException(index, id, version);
    }

}
