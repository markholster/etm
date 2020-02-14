/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
