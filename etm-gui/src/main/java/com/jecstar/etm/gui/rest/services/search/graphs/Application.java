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

import java.util.Objects;

public class Application extends AbstractVertex {

    private final String name;
    private String instance;

    public Application(String vertexId, String name) {
        super(vertexId);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getInstance() {
        return this.instance;
    }

    public Application setInstance(String instance) {
        this.instance = instance;
        return this;
    }

    @Override
    protected String getType() {
        return "application";
    }

    @Override
    protected void doWriteToJson(JsonBuilder builder) {
        builder.field("name", getName());
        builder.field("instance", getInstance());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Application that = (Application) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.name, this.instance);
    }
}
