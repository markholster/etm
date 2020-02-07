package com.jecstar.etm.gui.rest.services.search.graphs;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

/**
 * Class representing a vertex in a directed graph.
 */
public interface Vertex {

    /**
     * The unique id of the <code>Vertex</code>.
     *
     * @return The id of the <code>Vertex</code>.
     */
    String getVertexId();

    /**
     * Returns the parent <code>Vertex</code> of this <code>Vertex</code>.
     *
     * @return The parent <code>Vertex</code> of this <code>Vertex</code> or <code>null</code> when this <code>Vertex</code> has no parent.
     */
    Vertex getParent();

    /**
     * Adds the object to the buffer as a json string.
     *
     * @param builder The <code>JsonBuilder</code> to add the data to.
     */
    void toJson(JsonBuilder builder);
}
