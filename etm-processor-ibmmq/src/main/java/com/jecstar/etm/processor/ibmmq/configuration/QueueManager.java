package com.jecstar.etm.processor.ibmmq.configuration;

import java.util.List;

public class QueueManager {

	private String queueManagerName;
	private String queueManagerHost;
	private int queueManagerPort;
	private String queueManagerChannel;
	
	private List<Destination> destinations;
	
	public String getQueueManagerName() {
		return this.queueManagerName;
	}
	
	public void setQueueManagerName(String queueManagerName) {
		this.queueManagerName = queueManagerName;
	}
	
	public String getQueueManagerHost() {
		return this.queueManagerHost;
	}
	
	public void setQueueManagerHost(String queueManagerHost) {
		this.queueManagerHost = queueManagerHost;
	}
	
	public int getQueueManagerPort() {
		return this.queueManagerPort;
	}
	
	public void setQueueManagerPort(int queueManagerPort) {
		if (queueManagerPort < 1 || queueManagerPort > 65535) {
			throw new IllegalArgumentException(queueManagerPort + " is an invalid port number");
		}
		this.queueManagerPort = queueManagerPort;
	}
	
	public String getQueueManagerChannel() {
		return this.queueManagerChannel;
	}
	
	public void setQueueManagerChannel(String queueManagerChannel) {
		this.queueManagerChannel = queueManagerChannel;
	}
	
	public List<Destination> getDestinations() {
		return this.destinations;
	}
	
	public void setDestinations(List<Destination> destinations) {
		this.destinations = destinations;
	}
}
