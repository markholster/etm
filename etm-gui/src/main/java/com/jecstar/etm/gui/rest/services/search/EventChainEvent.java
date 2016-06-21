package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class EventChainEvent {
	
	private String eventId;
	private String correlationId;
	
	private String endpointName;
	private EventChainItem writer;
	private List<EventChainItem> readers = new ArrayList<>();
	
	private Comparator<EventChainItem> handlingTimeComparator = new Comparator<EventChainItem>(){
		@Override
		public int compare(EventChainItem o1, EventChainItem o2) {
			return new Long(o1.getHandlingTime()).compareTo(new Long(o2.getHandlingTime()));
		}};

	EventChainEvent(String eventId) {
		this.eventId = eventId;
	}

	public String getEventId() {
		return this.eventId;
	}
	

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
	
	public String getCorrelationId() {
		return this.correlationId;
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
	
	public void setEndpointName(String endpointName) {
		if (endpointName != null) {
			this.endpointName = endpointName;
		}
	}
	
	public String getEndpointName() {
		return this.endpointName;
	}
	
	public void sort() {
		this.readers.sort(this.handlingTimeComparator);
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EventChainEvent) {
			EventChainEvent other = (EventChainEvent) obj;
			return this.eventId.equals(other.eventId);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.eventId.hashCode();
	}
}
