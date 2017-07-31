package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class EventChainTransaction {

	private final String transactionId;
	
	private final List<EventChainItem> readers = new ArrayList<>();
	private final List<EventChainItem> writers = new ArrayList<>();
	
	private final Comparator<EventChainItem> handlingTimeComparator = (o1, o2) -> new Long(o1.getHandlingTime()).compareTo(o2.getHandlingTime());

	EventChainTransaction(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public String getTransactionId() {
		return this.transactionId;
	}
	
	public void addReader(EventChainItem item) {
		if (!this.readers.contains(item)) {
			this.readers.add(item);
		}
	}
	
	public List<EventChainItem> getReaders() {
		return this.readers;
	}
	
	public void addWriter(EventChainItem item) {
		if (!this.writers.contains(item)) {
			this.writers.add(item);
		}
	}
	
	public List<EventChainItem> getWriters() {
		return this.writers;
	}

	public void sort() {
		this.readers.sort(this.handlingTimeComparator);
		this.writers.sort(this.handlingTimeComparator);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EventChainTransaction) {
			EventChainTransaction other = (EventChainTransaction) obj;
			return this.transactionId.equals(other.transactionId);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.transactionId.hashCode();
	}
}
