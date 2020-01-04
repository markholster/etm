package com.jecstar.etm.gui.rest.services.search.graphs;

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
    protected void doWriteToJson(StringBuilder buffer) {
        jsonWriter.addStringElementToJsonBuffer("name", getName(), buffer, false);
        jsonWriter.addStringElementToJsonBuffer("instance", getInstance(), buffer, false);
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
