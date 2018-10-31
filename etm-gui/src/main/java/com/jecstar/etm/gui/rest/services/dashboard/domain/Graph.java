package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * A <code>Graph</code> holds all information to display a visual representation of one ore more groups of data. This class is the base class of all different graph types.
 */
public abstract class Graph {

    public static final String ID = "id";
    public static final String TYPE = "type";
    private static final String DATA_SOURCE = "data_source";
    public static final String FROM = "from";
    public static final String TILL = "till";
    public static final String TIME_FILTER_FIELD = "time_filter_field";
    public static final String QUERY = "query";

    @JsonField(ID)
    private String id;
    @JsonField(TYPE)
    private String type;
    @JsonField(DATA_SOURCE)
    private String dataSource;
    @JsonField(FROM)
    private String from;
    @JsonField(TILL)
    private String till;
    @JsonField(TIME_FILTER_FIELD)
    private String timeFilterField;
    @JsonField(QUERY)
    private String query;


    public String getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public String getDataSource() {
        return this.dataSource;
    }

    public String getFrom() {
        return this.from;
    }

    public String getTill() {
        return this.till;
    }

    public String getTimeFilterField() {
        return this.timeFilterField;
    }

    public String getQuery() {
        return this.query;
    }
}
