package com.jecstar.etm.gui.rest.services.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class EventChainTransaction {

	private String transactionId;
	
	private List<EventChainItem> readers = new ArrayList<>();
	private List<EventChainItem> writers = new ArrayList<>();
	
	private Comparator<EventChainItem> handlingTimeComparator = new Comparator<EventChainItem>(){
		@Override
		public int compare(EventChainItem o1, EventChainItem o2) {
			return new Long(o1.getHandlingTime()).compareTo(new Long(o2.getHandlingTime()));
		}};

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

	/**
	 * Method calculating the absolute response time for the first reader. If
	 * the first reader is not a request, this method will do nothing.
	 * 
	 * The absolute response time is calculated by subtracting the response time
	 * of the reader with the response time of all writers combined.
	 */
	public void calculateAbsoluteResponseTimes() {
		if (this.readers.isEmpty() || !this.readers.get(0).isRequest()) {
			// First reading item in a transaction is not a request -> could be a response or datagram. Not calculating absolute response time for those items.
			return;
		}
		long totalWriterResponseTimes = 0;
		for (EventChainItem item : this.writers) {
			if (item.isRequest()) {
				if (item.getResponseTime() != null) {
					totalWriterResponseTimes += item.getResponseTime();
				} else if (item.getExpiry() != null) {
					totalWriterResponseTimes += (item.getExpiry() - item.getHandlingTime());
				}
			}
		}
		EventChainItem reader = this.readers.get(0);
		if (reader.getResponseTime() == null) {
			// A request without a response time must be expired
			reader.setAbsoluteResponseTime(reader.getExpiry());
		} else {
			reader.setAbsoluteResponseTime(reader.getResponseTime() - totalWriterResponseTimes); 
		}
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
