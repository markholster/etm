package com.jecstar.etm.gui.rest.services.search.eventchain;

import com.jecstar.etm.server.core.util.ObjectUtils;

public class EventChainApplication {

    private final String name;
    private final String instance;

    public EventChainApplication(String name, String instance) {
        this.name = name;
        this.instance = instance;
    }

    public String getName() {
        return this.name;
    }

    public String getInstance() {
        return this.instance;
    }

    public String getId() {
        if (getInstance() == null) {
            return getName();
        }
        return getName() + "_" + getInstance();
    }

    public String getDisplayName() {
        if (getInstance() == null) {
            return getName();
        }
        return getName() + " (" + getInstance() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EventChainApplication) {
            EventChainApplication other = (EventChainApplication) obj;
            return ObjectUtils.equalsNullProof(this.getId(), other.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (getName() + getInstance()).hashCode();
    }
}
