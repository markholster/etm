package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventChainEndpoint {

    private final String name;
    private final String eventId;

    private EventChainItem writer;
    private final List<EventChainItem> readers = new ArrayList<>();

    private final Comparator<EventChainItem> handlingTimeComparator = (o1, o2) -> new Long(o1.getHandlingTime()).compareTo(o2.getHandlingTime());

    EventChainEndpoint(String name, String eventId) {
        this.name = name;
        this.eventId = eventId;
    }

    public String getName() {
        return this.name;
    }

    public void setWriter(EventChainItem item) {
        this.writer = item;
    }

    public EventChainItem getWriter() {
        return this.writer;
    }

    public void addReader(EventChainItem item) {
        if (!this.readers.contains(item)) {
            this.readers.add(item);
        }
    }

    public List<EventChainItem> getReaders() {
        return this.readers;
    }

    public void sort() {
        this.readers.sort(this.handlingTimeComparator);
    }

    public boolean isMissing() {
        if (this.writer != null) {
            if (!this.writer.isMissing()) {
                return false;
            }
        }
        for (EventChainItem reader : this.readers) {
            if (!reader.isMissing()) {
                return false;
            }
        }
        return true;
    }

    public boolean isRequest() {
        return getFirstEventChainItem().isRequest();
    }

    public boolean isResponse() {
        return getFirstEventChainItem().isResponse();
    }

    public boolean isAsync() {
        return getFirstEventChainItem().isAsync();
    }


    /**
     * Gives the first item that occurred in this event. This is the writer, or
     * the first reader if no writer is present. Make sure the {@link #sort()}
     * method is called before calling this method if you want to retrieve the
     * first element in time.
     *
     * @return The first item.
     */
    public EventChainItem getFirstEventChainItem() {
        if (this.writer != null) {
            return this.writer;
        }
        return this.readers.get(0);
    }

    public String getKey() {
        return this.name + "_" + this.eventId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EventChainEndpoint) {
            EventChainEndpoint other = (EventChainEndpoint) obj;
            return getKey().equals(other.getKey());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

}
