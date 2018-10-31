package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.*;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;

public class GraphConverter extends JsonEntityConverter<Graph> {

    public GraphConverter() {
        super(f -> {
            switch ((String) f.get(Graph.TYPE)) {
                case BarGraph.TYPE:
                    return new BarGraph();
                case LineGraph.TYPE:
                    return new LineGraph();
                case NumberGraph.TYPE:
                    return new NumberGraph();
                case StackedAreaGraph.TYPE:
                    return new StackedAreaGraph();
                default:
                    throw new IllegalArgumentException((String) f.get(Graph.TYPE));

            }
        });
    }
}
