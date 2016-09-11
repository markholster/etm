package com.jecstar.etm.gui.rest.services.iib;

public class Node {
	
	private final String name;
	private final String host;
	private final int port; 
	private final String queueManager;
	private String channel;

	
	Node(String name, String host, int port, String queueManager) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.queueManager = queueManager;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String getQueueManager() {
		return this.queueManager;
	}
	
	public String getChannel() {
		return this.channel;
	}
	
	public void setChannel(String channel) {
		this.channel = channel;
	}
}
