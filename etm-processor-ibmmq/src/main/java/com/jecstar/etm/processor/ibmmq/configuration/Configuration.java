package com.jecstar.etm.processor.ibmmq.configuration;

import java.net.InetAddress;
import java.util.List;

public class Configuration {

	private String clusterName = "Enterprise Telemetry Monitor";
	private String nodeName;
	private String masterAddresses;
	private int flushInterval = 30000;
	private boolean logMetrics;
	
	private List<QueueManager> queueManagers;
	
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
	
	public List<QueueManager> getQueueManagers() {
		return this.queueManagers;
	}
	
	public void setQueueManagers(List<QueueManager> queueManagers) {
		this.queueManagers = queueManagers;
	}
	
	public boolean isLogMetrics() {
		return this.logMetrics;
	}
	
	public void setLogMetrics(boolean logMetrics) {
		this.logMetrics = logMetrics;
	}
	
	public String getCalculatedNodeName() {
		if (this.nodeName != null) {
			return "ProcessorNode@" + this.nodeName;
		} else {
			return "ProcessorNode@" + getHostName();
		}
	}
	
	public int getTotalNumberOfListeners() {
		if (this.queueManagers == null) {
			return 0;
		}
		int total = 0;
		for (QueueManager queueManager : this.queueManagers) {
			if (queueManager.getDestinations() != null) {
				total += queueManager.getDestinations().stream().mapToInt(d -> d.getNrOfListeners()).sum(); 
			}
		}
		return total;
	}
	
	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "local";
		}
	}
	
}
