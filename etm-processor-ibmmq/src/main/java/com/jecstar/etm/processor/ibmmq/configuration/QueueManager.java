package com.jecstar.etm.processor.ibmmq.configuration;

import java.util.List;

public class QueueManager {

	private String name = "QM1";
	private String host = "127.0.0.1";
	private int port = 1414;
	private String channel;
	
	private List<Destination> destinations;
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public void setPort(int port) {
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException(port + " is an invalid port number");
		}
		this.port = port;
	}
	
	public String getChannel() {
		return this.channel;
	}
	
	public void setChannel(String channel) {
		this.channel = channel;
	}
	
	public List<Destination> getDestinations() {
		return this.destinations;
	}
	
	public void setDestinations(List<Destination> destinations) {
		this.destinations = destinations;
	}
}
