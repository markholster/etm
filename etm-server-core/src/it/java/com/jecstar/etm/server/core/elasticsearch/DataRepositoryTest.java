package com.jecstar.etm.server.core.elasticsearch;

import com.jecstar.etm.server.core.AbstractIntegrationTest;
import com.jecstar.etm.server.core.elasticsearch.builder.DeleteIndexRequestBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class DataRepositoryTest extends AbstractIntegrationTest {

    @Test
    public void testUpdateVersionConflictExceptionClass() throws IOException, InterruptedException {
        RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create("127.0.0.1:9200"));
        RestHighLevelClient rc = new RestHighLevelClient(restClientBuilder);
        DataRepository dataRepository = new DataRepository(rc);


        rc.index(new IndexRequest("test", "_doc", "id").source("{\"hello\": \"mark\"}", XContentType.JSON), RequestOptions.DEFAULT);
        waitFor("test", "_doc", "id");
        try {
            rc.update(new UpdateRequest("test", "_doc", "id").version(100).doc("{\"doc\": {\"hello\": \"mark2\"}}", XContentType.JSON), RequestOptions.DEFAULT);
            fail("ElasticsearchStatusException not thrown. com.jecstar.etm.signaler.Signaler needs to be adjusted to this new exception.");
        } catch (ElasticsearchStatusException e) {
            assertEquals(RestStatus.CONFLICT, e.status());
        } finally {
            dataRepository.indicesDelete(new DeleteIndexRequestBuilder().setIndices("test"));
        }
    }


    @Override
    protected BulkProcessor.Listener createBulkListener() {
        return new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
            }
        };
    }
}
