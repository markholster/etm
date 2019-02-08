package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.GraphContainer;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;

public class GraphContainerConverter extends JsonEntityConverter<GraphContainer> {

    public GraphContainerConverter() {
        super(f -> new GraphContainer());
    }
}
