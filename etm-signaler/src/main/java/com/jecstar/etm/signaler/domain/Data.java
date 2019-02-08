package com.jecstar.etm.signaler.domain;

import com.jecstar.etm.server.core.converter.JsonField;

public class Data {

    private static final String DATA_SOURCE = "data_source";
    public static final String FROM = "from";
    public static final String TILL = "till";
    public static final String TIME_FILTER_FIELD = "time_filter_field";
    public static final String QUERY = "query";

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

    public Data setDataSource(String dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public Data setFrom(String from) {
        this.from = from;
        return this;
    }

    public Data setTill(String till) {
        this.till = till;
        return this;
    }

    public Data setTimeFilterField(String timeFilterField) {
        this.timeFilterField = timeFilterField;
        return this;
    }

    public Data setQuery(String query) {
        this.query = query;
        return this;
    }

    protected void mergeFromColumn(Data columnData) {
        this.from = columnData.getFrom();
        this.till = columnData.getTill();
        this.timeFilterField = columnData.getTimeFilterField();
        this.query = columnData.getQuery();
    }
}
