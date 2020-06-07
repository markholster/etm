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

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.LicenseRateLimiter;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.elasticsearch.domain.IndicesStatsResponse;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.IndicesStatsResponseConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.elastic.RequestUnitCalculatingOutputStream;
import com.jecstar.etm.server.core.persisting.elastic.RequestUnitCalculatingStreamOutput;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptResponse;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

public class DataRepository {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private final LogWrapper log = LogFactory.getLogger(getClass());

    private final RestHighLevelClient client;

    private final IndicesStatsResponseConverter indicesStatsResponseConverter = new IndicesStatsResponseConverter();

    private static final ActionListener<Object> NOOP_ACTION_LISTENER = new ActionListener<>() {

        @Override
        public void onResponse(Object response) {
        }

        @Override
        public void onFailure(Exception exception) {
        }
    };

    private final Sniffer sniffer;
    private LicenseRateLimiter licenseRateLimiter = new LicenseRateLimiter(null);

    public DataRepository(RestHighLevelClient client) {
        this.client = client;
        this.sniffer = Sniffer.builder(client.getLowLevelClient())
                .setSniffIntervalMillis(30000).build();
    }

    public void close() {
        this.sniffer.close();
        try {
            this.client.close();
        } catch (IOException e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Unable to close elasticsearch client", e);
            }
        }
    }

    public RestHighLevelClient getClient() {
        return this.client;
    }

    public void setLicenseRateLimiter(LicenseRateLimiter licenseRateLimiter) {
        this.licenseRateLimiter = licenseRateLimiter;
    }

    public IndexResponse index(IndexRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.index(builder.build(), RequestOptions.DEFAULT);
            this.licenseRateLimiter.addRequestUnitsAndShape(builder.calculateIndexRequestUnits(), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public void indexAsync(IndexRequestBuilder builder, ActionListener<IndexResponse> listener) {
        this.client.indexAsync(builder.build(), RequestOptions.DEFAULT, listener);
    }

    public GetResponse get(GetRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.get(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public MultiGetResponse get(MultiGetRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.mget(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public void getAsync(GetRequestBuilder builder, SyncActionListener<GetResponse> listener) {
        this.client.getAsync(builder.build(), RequestOptions.DEFAULT, listener);
    }

    public SearchResponse search(SearchRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.search(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public void searchAsync(SearchRequestBuilder builder, ActionListener<SearchResponse> listener) {
        this.client.searchAsync(builder.build(), RequestOptions.DEFAULT, listener);
    }

    public UpdateResponse update(UpdateRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.update(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(builder.calculateIndexRequestUnits() + calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public void updateAsync(UpdateRequestBuilder builder, ActionListener<UpdateResponse> listener) {
        this.client.updateAsync(builder.build(), RequestOptions.DEFAULT, listener);
    }

    public DeleteResponse delete(DeleteRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.delete(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public SearchResponse scroll(SearchScrollRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.scroll(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public ClearScrollResponse clearScroll(ClearScrollRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.clearScroll(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public boolean indicesExist(GetIndexRequestBuilder builder) {
        try {
            return this.client.indices().exists(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public GetIndexResponse indicesGet(GetIndexRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().get(builder.build(), RequestOptions.DEFAULT);
//            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (ElasticsearchStatusException e) {
            if (RestStatus.NOT_FOUND.equals(e.status())) {
                return null;
            }
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesDelete(DeleteIndexRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().delete(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(builder.build()), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public RefreshResponse indicesRefresh(RefreshIndexRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().refresh(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }

    }

    public FlushResponse indicesFlush(FlushIndexRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().flush(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public CreateIndexResponse indicesCreate(CreateIndexRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().create(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesPutTemplate(PutIndexTemplateRequestBuilder requestBuilder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().putTemplate(requestBuilder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits((ActionRequest) requestBuilder.build()), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public boolean indicesTemplatesExist(IndexTemplatesExistRequestBuilder builder) {
        try {
            return this.client.indices().existsTemplate(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public GetIndexTemplatesResponse indicesGetTemplate(GetIndexTemplateRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().getIndexTemplate(builder.build(), RequestOptions.DEFAULT);
//            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesDeleteTemplate(DeleteIndexTemplateRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().deleteTemplate(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(builder.build()), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public GetMappingsResponse indicesGetMappings(GetMappingsRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().getMapping(builder.build(), RequestOptions.DEFAULT);
//            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesPutMapping(PutMappingRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().putMapping(builder.build(), RequestOptions.DEFAULT);
//            shapeRequest(calculateIndexRequestUnits(builder.build()), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public GetFieldMappingsResponse indicesGetFieldMappings(GetFieldMappingsRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().getFieldMapping(builder.build(), RequestOptions.DEFAULT);
//            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public GetSettingsResponse indicesGetSettings(GetSettingsRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().getSettings(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesUpdateSettings(UpdateIndexSettingsRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().putSettings(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits((ActionRequest) builder.build()), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public IndicesStatsResponse indicesGetStats(IndicesStatsRequestBuilder builder) {
        try {
            Response response;
            Request request = builder.request();
            if (builder.getTimeout() != null) {
                SyncResponseListener listener = new SyncResponseListener(builder.getTimeout().millis());
                this.client.getLowLevelClient().performRequestAsync(request, listener);
                response = listener.get();
            } else {
                response = this.client.getLowLevelClient().performRequest(request);
            }
            return this.indicesStatsResponseConverter.read(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public void indicesGetStatsAsync(IndicesStatsRequestBuilder builder, ActionListener<IndicesStatsResponse> listener) {
        this.client.getLowLevelClient().performRequestAsync(builder.request(), new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                try {
                    IndicesStatsResponse indicesStatsResponse = DataRepository.this.indicesStatsResponseConverter.read(EntityUtils.toString(response.getEntity()));
                    listener.onResponse(indicesStatsResponse);
                } catch (IOException e) {
                    listener.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    public GetAliasesResponse indicesGetAliases(GetAliasesRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.indices().getAlias(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public BulkResponse bulk(BulkRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.bulk(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(builder.build()), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse putStoredScript(PutStoredScriptRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.putScript(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits((ActionRequest) builder.build()), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse deleteStoredScript(DeleteStoredScriptRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.deleteScript(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        } catch (ElasticsearchStatusException e) {
            if (RestStatus.NOT_FOUND.equals(e.status())) {
                return null;
            }
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public GetStoredScriptResponse getStoredScript(GetStoredScriptRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.getScript(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        } catch (ElasticsearchStatusException e) {
            if (RestStatus.NOT_FOUND.equals(e.status())) {
                return null;
            }
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public ClusterHealthResponse clusterHealth(ClusterHealthRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.cluster().health(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public ClusterGetSettingsResponse clusterGetSettings(ClusterGetSettingsRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.cluster().getSettings(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public ClusterUpdateSettingsResponse clusterUpdateSettings(ClusterUpdateSettingsRequestBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            var response = this.client.cluster().putSettings(builder.build(), RequestOptions.DEFAULT);
            shapeRequest(calculateIndexRequestUnits(response), System.currentTimeMillis() - startTime);
            return response;
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ActionListener<T> noopActionListener() {
        return (ActionListener<T>) DataRepository.NOOP_ACTION_LISTENER;
    }

    @SuppressWarnings("unchecked")
    public static <T> SyncActionListener<T> syncActionListener(long timeout) {
        return (SyncActionListener<T>) new SyncActionListener<>(timeout);
    }

    /**
     * Calculates the request units of an <code>ActionResponse</code>.
     *
     * @param response The <code>ActionResponse</code> to calculate the amount of request units from.
     * @return The amount of request units.
     */
    private double calculateIndexRequestUnits(ToXContent response) {
        RequestUnitCalculatingOutputStream stream = new RequestUnitCalculatingOutputStream();
        try {
            response.toXContent(new XContentBuilder(JsonXContent.jsonXContent, stream), ToXContent.EMPTY_PARAMS);
            return stream.getRequestUnits();
        } catch (IOException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    /**
     * Calculates the request units of an <code>ActionRequest</code>.
     *
     * @param request The <code>ActionRequest</code> to calculate the amount of request units from.
     * @return The amount of request units.
     */
    private double calculateIndexRequestUnits(ActionRequest request) {
        RequestUnitCalculatingStreamOutput streamOutput = new RequestUnitCalculatingStreamOutput();
        try {
            request.writeTo(streamOutput);
            return streamOutput.getRequestUnits();
        } catch (IOException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    /**
     * Shape the request when a <code>LicenseRateLimiter</code> is set.
     *
     * @param requestUnits  The number of request units that were consumed.
     * @param executionTime The time it took to consume those request units.
     */
    private void shapeRequest(double requestUnits, long executionTime) {
        if (this.licenseRateLimiter == null) {
            return;
        }
        this.licenseRateLimiter.addRequestUnitsAndShape(requestUnits, executionTime);
    }

}
