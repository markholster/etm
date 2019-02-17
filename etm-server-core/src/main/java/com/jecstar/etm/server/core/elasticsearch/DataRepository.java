package com.jecstar.etm.server.core.elasticsearch;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import com.jecstar.etm.server.core.elasticsearch.domain.IndicesStatsResponse;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.IndicesStatsResponseConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
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
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

public class DataRepository {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private final LogWrapper log = LogFactory.getLogger(getClass());

    private final RestHighLevelClient client;

    private final IndicesStatsResponseConverter indicesStatsResponseConverter = new IndicesStatsResponseConverter();

    private static final ActionListener<Object> NOOP_ACTION_LISTENER = new ActionListener<Object>() {

        @Override
        public void onResponse(Object response) {
        }

        @Override
        public void onFailure(Exception exception) {
        }
    };

    private final Sniffer sniffer;

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

    public IndexResponse index(IndexRequestBuilder builder) {
        try {
            return this.client.index(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public void indexAsync(IndexRequestBuilder builder, ActionListener<IndexResponse> listener) {
        this.client.indexAsync(builder.build(), RequestOptions.DEFAULT, listener);
    }

    public GetResponse get(GetRequestBuilder builder) {
        try {
            return this.client.get(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public MultiGetResponse get(MultiGetRequestBuilder builder) {
        try {
            return this.client.mget(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public void getAsync(GetRequestBuilder builder, SyncActionListener<GetResponse> listener) {
        this.client.getAsync(builder.build(), RequestOptions.DEFAULT, listener);
    }

    public SearchResponse search(SearchRequestBuilder builder) {
        try {
            return this.client.search(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public void searchAsync(SearchRequestBuilder builder, ActionListener<SearchResponse> listener) {
        this.client.searchAsync(builder.build(), RequestOptions.DEFAULT, listener);
    }

    public UpdateResponse update(UpdateRequestBuilder builder) {
        try {
            return this.client.update(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public void updateAsync(UpdateRequestBuilder builder, ActionListener<UpdateResponse> listener) {
        this.client.updateAsync(builder.build(), RequestOptions.DEFAULT, listener);
    }

    public DeleteResponse delete(DeleteRequestBuilder builder) {
        try {
            return this.client.delete(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public SearchResponse scroll(SearchScrollRequestBuilder builder) {
        try {
            return this.client.scroll(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public ClearScrollResponse clearScroll(ClearScrollRequestBuilder builder) {
        try {
            return this.client.clearScroll(builder.build(), RequestOptions.DEFAULT);
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
            return this.client.indices().get(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesDelete(DeleteIndexRequestBuilder builder) {
        try {
            return this.client.indices().delete(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public FlushResponse indicesFlush(FlushIndexRequestBuilder builder) {
        try {
            return this.client.indices().flush(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public CreateIndexResponse indicesCreate(CreateIndexRequestBuilder builder) {
        try {
            return this.client.indices().create(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesPutTemplate(PutIndexTemplateRequestBuilder requestBuilder) {
        try {
            return this.client.indices().putTemplate(requestBuilder.build(), RequestOptions.DEFAULT);
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
            return this.client.indices().getTemplate(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesDeleteTemplate(DeleteIndexTemplateRequestBuilder builder) {
        try {
            return this.client.indices().deleteTemplate(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public GetMappingsResponse indicesGetMappings(GetMappingsRequestBuilder builder) {
        try {
            return this.client.indices().getMapping(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesPutMapping(PutMappingRequestBuilder builder) {
        try {
            return this.client.indices().putMapping(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public GetFieldMappingsResponse indicesGetFieldMappings(GetFieldMappingsRequestBuilder builder) {
        try {
            return this.client.indices().getFieldMapping(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse indicesUpdateSettings(UpdateIndexSettingsRequestBuilder builder) {
        try {
            return this.client.indices().putSettings(builder.build(), RequestOptions.DEFAULT);
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
            return this.client.indices().getAlias(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public BulkResponse bulk(BulkRequestBuilder builder) {
        try {
            return this.client.bulk(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public AcknowledgedResponse putStoredScript(PutStoredScriptRequestBuilder builder) {
        try {
            return this.client.putScript(builder.build(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EtmException(EtmException.DATA_COMMUNICATION_EXCEPTION, e);
        }
    }

    public GetStoredScriptResponse getStoredScript(GetStoredScriptRequestBuilder builder) {
        try {
            return this.client.getScript(builder.build(), RequestOptions.DEFAULT);
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
            return this.client.cluster().health(builder.build(), RequestOptions.DEFAULT);
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
}
