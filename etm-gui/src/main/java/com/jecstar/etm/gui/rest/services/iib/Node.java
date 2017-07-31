package com.jecstar.etm.gui.rest.services.iib;

public class Node {
	
	private final String name;
	private final String host;
	private final int port; 
	private String username;
	private String password;
	private String queueManager;
	private String channel;

	
	Node(String name, String host, int port) {
		this.name = name;
		this.host = host;
		this.port = port;
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

	public String getUsername() {
		return this.username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getQueueManager() {
		return this.queueManager;
	}
	
	public void setQueueManager(String queueManager) {
		this.queueManager = queueManager;
	}
	
	public String getChannel() {
		return this.channel;
	}
	
	public void setChannel(String channel) {
		this.channel = channel;
	}
}
