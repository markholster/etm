package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.domain.writer.json.JsonWriter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;

public abstract class Graph<T extends Graph> {

    public static final String TYPE = "type";
    protected final JsonWriter jsonWriter = new JsonWriter();

    @JsonField(TYPE)
    private String type;

    public String getType() {
        return this.type;
    }

    protected Graph<T> setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Add the aggregators to the <code>SearchRequestBuilder</code>
     *
     * @param searchRequest The <code>SearchRequestBuilder</code> instance on which the aggregators should be added.
     */
    public abstract void addAggregators(SearchRequestBuilder searchRequest);

    /**
     * Append the highcharts configuration (as json) to the current config.
     *
     * @param config The current configuration
     */
    public abstract void appendHighchartsConfig(StringBuilder config);

    /**
     * Gives the D3 format of the values in the graph.
     *
     * @return The format of the values in the graph.
     */
    public abstract String getValueFormat();

    /**
     * Merge an other <code>Graph</code> instance configured at a <code>Dasboards</code>'s <code>Column</code>.
     *
     * @param graph The <code>Graph</code> to merge from.
     */
    public abstract void mergeFromColumn(T graph);

    /**
     * Prepares the <code>Graph</code> for the search to be executed. This gives <code>Aggregator</code>s the option to configure themselves in the context of the search to be executed. As an example <code>Aggregator</code>s with an automatic range can determine their range by inspecting the range configured in the query.
     *
     * @param dataRepository        The <code>DataRepository</code>.
     * @param searchRequestBuilder  The <code>SearchRequestBuilder</code> to be executed in the next step.
     */
    public abstract void prepareForSearch(DataRepository dataRepository, SearchRequestBuilder searchRequestBuilder);
}
