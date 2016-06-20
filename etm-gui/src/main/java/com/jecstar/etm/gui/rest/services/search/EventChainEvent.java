package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class EventChainEvent {
	
	private String eventId;
	
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
		if (this.writer != null) {
			return this.writer.isRequest();
		}
		if (!this.readers.isEmpty()) {
			return this.readers.get(0).isRequest();
		}
		return false;
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

	public void calculateResponseTimePercentages(float transactionTime) {
		if (this.writer != null) {
			calculateResponseTimePercentages(this.writer, transactionTime);
		}
		for (EventChainItem item : this.readers) {
			calculateResponseTimePercentages(item, transactionTime);
		}
	}
	
	private void calculateResponseTimePercentages(EventChainItem item, float transactionTime) {
		if (item.isRequest()) {
			if (item.getAbsoluteResponseTime() != null) {
				float percentage = ((float)item.getAbsoluteResponseTime()) / transactionTime;
				if (percentage > 1) {
					percentage = 1;
				}
				item.setAbsoluteResponseTimePercentage(percentage);
			}
			Long responseTime = item.getResponseTime();
			if (responseTime == null) {
				responseTime = item.getExpiry() - item.getHandlingTime();
			}
			if (responseTime != null) {
				float percentage = ((float)responseTime) / transactionTime;
				if (percentage > 1) {
					percentage = 1;
				}
				item.setResponseTimePercentage(percentage);
			}			
		}
	}
}
