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

package com.jecstar.etm.server.core.elasticsearch;

import com.jecstar.etm.server.core.AbstractIntegrationTest;
import com.jecstar.etm.server.core.elasticsearch.builder.DeleteIndexRequestBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
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


        rc.index(new IndexRequest("test").id("id").source("{\"hello\": \"mark\"}", XContentType.JSON), RequestOptions.DEFAULT);
        GetResponse response = waitFor("test", "id");
        try {
            rc.update(new UpdateRequest("test", "id").setIfSeqNo(response.getSeqNo() + 1).setIfPrimaryTerm(response.getPrimaryTerm()).doc("{\"doc\": {\"hello\": \"mark2\"}}", XContentType.JSON), RequestOptions.DEFAULT);
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
