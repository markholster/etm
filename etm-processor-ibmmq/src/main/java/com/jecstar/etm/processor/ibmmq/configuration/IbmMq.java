package com.jecstar.etm.processor.ibmmq.configuration;

import java.util.ArrayList;
import java.util.List;

public class IbmMq {

	public boolean enabled = false;
	public final List<QueueManager> queueManagers = new ArrayList<>();
	
	public int getTotalNumberOfListeners() {
		if (this.queueManagers == null) {
			return 0;
		}
		int total = 0;
		for (QueueManager queueManager : this.queueManagers) {
			if (queueManager.getDestinations() != null) {
				total += queueManager.getDestinations().stream().mapToInt(Destination::getNrOfListeners).sum();
			}
		}
		return total;
	}
	
	public List<QueueManager> getQueueManagers() {
		return this.queueManagers;
	}
}
