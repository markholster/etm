package com.jecstar.etm.processor.ibmmq.configuration;

import java.net.InetAddress;

public class Configuration {

	private String clusterName = "Enterprise Telemetry Monitor";
	private String nodeName;
	private String masterAddresses;
	private int flushInterval = 30000;
	
	private QueueManager queueManager;
	
	public String getClusterName() {
		return this.clusterName;
	}
	
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}
	
	public String getNodeName() {
		return this.nodeName;
	}
	
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	public String getMasterAddresses() {
		return this.masterAddresses;
	}
	
	public void setMasterAddresses(String masterAddresses) {
		this.masterAddresses = masterAddresses;
	}
	
	public int getFlushInterval() {
		return this.flushInterval;
	}
	
	public void setFlushInterval(int flushInterval) {
		this.flushInterval = flushInterval;
	}
	
	public QueueManager getQueueManager() {
		return this.queueManager;
	}
	
	public void setQueueManager(QueueManager queueManager) {
		this.queueManager = queueManager;
	}
	
	public String getCalculatedNodeName() {
		if (this.nodeName != null) {
			return "ProcessorNode@" + this.nodeName;
		} else {
			return "ProcessorNode@" + getHostName();
		}
	}
	
	public int getTotalNumberOfListeners() {
		if (this.queueManager == null) {
			return 0;
		}
		if (this.queueManager.getDestinations() == null) {
			return 0;
		}
		return this.queueManager.getDestinations().stream().mapToInt(d -> d.getNrOfListeners()).sum();
	}
	
	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "local";
		}
	}
	
}
