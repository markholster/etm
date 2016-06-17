package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.List;

public class EventChainTransactionEvents {

	private final String transactionId;
	
	private EventChainKey reader;
	
	private List<EventChainKey> writers = new ArrayList<>();
	
	EventChainTransactionEvents(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public String getTransactionId() {
		return this.transactionId;
	}
	
	public void setReader(EventChainKey reader) {
		this.reader = reader;
	}
	
	public void addWriter(EventChainKey writer) {
		this.writers.add(writer);
	}
	
	public EventChainKey getReader() {
		return this.reader;
	}
	
	public List<EventChainKey> getWriters() {
		return this.writers;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EventChainTransactionEvents) {
			EventChainTransactionEvents other = (EventChainTransactionEvents) obj;
			return this.transactionId.equals(other.transactionId);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.transactionId.hashCode();
	}
}
