package com.jecstar.etm.domain;

import java.net.InetAddress;
import java.util.Objects;

public class Application {

    /**
     * The name of the application.
     */
    public String name;

    /**
     * The hostAddress of the application.
     */
    public InetAddress hostAddress;

    /**
     * The instance of the application. Useful if the application is clustered.
     */
    public String instance;

    /**
     * The principal that executed the action that triggers the event.
     */
    public String principal;

    /**
     * The version of the application.
     */
    public String version;

    public Application initialize() {
        this.name = null;
        this.hostAddress = null;
        this.instance = null;
        this.principal = null;
        this.version = null;
        return this;
    }

    public Application initialize(Application copy) {
        this.initialize();
        if (copy == null) {
            return this;
        }
        this.name = copy.name;
        this.hostAddress = copy.hostAddress;
        this.instance = copy.instance;
        this.principal = copy.principal;
        this.version = copy.version;
        return this;
    }

    public boolean isSet() {
        return this.name != null || this.instance != null || this.principal != null || this.version != null || this.hostAddress != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.instance, that.instance) &&
                Objects.equals(this.version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.instance, this.version);
    }
}
