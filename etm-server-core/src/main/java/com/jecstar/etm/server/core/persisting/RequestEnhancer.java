package com.jecstar.etm.server.core.persisting;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.builder.*;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.common.unit.TimeValue;

/**
 * Class that is capable of enhancing the Elasticsearch request with the configured parameters in ETM.
 */
public class RequestEnhancer {

    private final EtmConfiguration etmConfiguration;

    /**
     * Constructs a new <code>RequestEnhancer</code> instance.
     *
     * @param etmConfiguration The <code>EtmConfiguration</code> instance that holds the request variables like timout etc.
     */
    public RequestEnhancer(EtmConfiguration etmConfiguration) {
        this.etmConfiguration = etmConfiguration;
    }

    /**
     * Enhances a <code>DeleteRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder The <code>DeleteRequestBuilder</code> that needs to be enhanced.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    public DeleteRequestBuilder enhance(DeleteRequestBuilder builder) {
        return builder.setWaitForActiveShards(getActiveShardCount(this.etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
    }

    /**
     * Enhances an <code>UpdateRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder The <code>UpdateRequestBuilder</code> that needs to be enhanced.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    public UpdateRequestBuilder enhance(UpdateRequestBuilder builder) {
        return builder.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setRetryOnConflict(etmConfiguration.getRetryOnConflictCount());
    }

    /**
     * Enhances an <code>IndexRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder The <code>IndexRequestBuilder</code> that needs to be enhanced.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    public IndexRequestBuilder enhance(IndexRequestBuilder builder) {
        return builder.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
    }

    /**
     * Enhances an <code>SearchRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder The <code>SearchRequestBuilder</code> that needs to be enhanced.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    public SearchRequestBuilder enhance(SearchRequestBuilder builder) {
        builder.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()));
        return builder;
    }

    /**
     * Enhances an <code>BulkRequestBuilder</code> with the defaults from the <code>EtmConfiguration</code>.
     *
     * @param builder The <code>BulkRequestBuilder</code> that needs to be enhanced.
     * @return The enhanced <code>EtmConfiguration</code>.
     */
    public BulkRequestBuilder enhance(BulkRequestBuilder builder) {
        return builder.setWaitForActiveShards(getActiveShardCount(etmConfiguration));
    }

    private ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
        if (-1 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.ALL;
        } else if (0 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.NONE;
        }
        return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }

}
