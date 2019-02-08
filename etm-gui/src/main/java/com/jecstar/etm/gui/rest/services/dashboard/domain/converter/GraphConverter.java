package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.graph.*;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;

public class GraphConverter extends NestedObjectConverter<Graph> {

    public GraphConverter() {
        super(f -> {
            switch ((String) f.get(Graph.TYPE)) {
                case AreaGraph.TYPE:
                    return new AreaGraph();
                case BarGraph.TYPE:
                    return new BarGraph();
                case LineGraph.TYPE:
                    return new LineGraph();
                case NumberGraph.TYPE:
                    return new NumberGraph();
                case PieGraph.TYPE:
                    return new PieGraph();
                case ScatterGraph.TYPE:
                    return new ScatterGraph();
                default:
                    throw new IllegalArgumentException((String) f.get(Graph.TYPE));

            }
        });
    }
}
