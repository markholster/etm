package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.DataConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.GraphConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.graph.Graph;
import com.jecstar.etm.server.core.converter.JsonField;

import java.util.Objects;

public class Column {

    public static final String ID = "id";
    public static final String WIDTH = "width";
    public static final String TITLE = "title";
    public static final String GRAPH_NAME = "graph_name";
    public static final String BORDERED = "bordered";
    public static final String REFRESH_RATE = "refresh_rate";


    @JsonField(ID)
    private String id;
    @JsonField(WIDTH)
    private int width;
    @JsonField(TITLE)
    private String title;
    @JsonField(GRAPH_NAME)
    private String graphName;

    @JsonField(value = GraphContainer.DATA, converterClass = DataConverter.class)
    private Data data;

    @JsonField(value = GraphContainer.GRAPH, converterClass = GraphConverter.class)
    private Graph<?> graph;

    @JsonField(BORDERED)
    private boolean bordered;

    @JsonField(REFRESH_RATE)
    private Integer refreshRate;

    public String getId() {
        return this.id;
    }

    public int getWidth() {
        return this.width;
    }

    public String getGraphName() {
        return this.graphName;
    }

    public String getTitle() {
        return this.title;
    }

    public Data getData() {
        return this.data;
    }

    public Graph<?> getGraph() {
        return this.graph;
    }

    public boolean isBordered() {
        return this.bordered;
    }

    public Integer getRefreshRate() {
        return this.refreshRate;
    }

    public Column setId(String id) {
        this.id = id;
        return this;
    }

    public Column setWidth(int width) {
        this.width = width;
        return this;
    }

    public Column setGraphName(String graphName) {
        this.graphName = graphName;
        return this;
    }

    public Column setTitle(String title) {
        this.title = title;
        return this;
    }

    public Column setData(Data data) {
        this.data = data;
        return this;
    }

    public Column setGraph(Graph<?> graph) {
        this.graph = graph;
        return this;
    }

    public Column setBordered(boolean bordered) {
        this.bordered = bordered;
        return this;
    }

    public Column setRefreshRate(Integer refreshRate) {
        this.refreshRate = refreshRate;
        return this;
    }

    public boolean removeGraph(String graphName) {
        if (!Objects.equals(graphName, getGraphName())) {
            return false;
        }
        this.graphName = null;
        this.data = null;
        this.graph = null;
        return true;
    }


}
