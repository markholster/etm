package com.jecstar.etm.processor.ibmmq.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QueueManager {

	private String name = "QMGR";
	private String host = "127.0.0.1";
	private int port = 1414;
	private String channel;
	private String userId;
	private String password;
	private String sslCipherSuite;
	private String sslProtocol = "TLSv1.2";
	private File sslKeystoreLocation;
	private String sslKeystoreType = "PKCS12";
	private String sslKeystorePassword;
	private File sslTruststoreLocation;
	private String sslTruststoreType = "JKS";
	private String sslTruststorePassword;
	
	
	private List<Destination> destinations = new ArrayList<>();
	
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
	
	public String getUserId() {
		return this.userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getSslCipherSuite() {
		return this.sslCipherSuite;
	}
	
	public void setSslCipherSuite(String sslCipherSuite) {
		this.sslCipherSuite = sslCipherSuite;
	}

	public String getSslProtocol() {
		return sslProtocol;
	}

	public void setSslProtocol(String sslProtocol) {
		this.sslProtocol = sslProtocol;
	}

	public File getSslKeystoreLocation() {
		return this.sslKeystoreLocation;
	}

	public void setSslKeystoreLocation(File sslKeystoreLocation) {
		this.sslKeystoreLocation = sslKeystoreLocation;
	}

	public String getSslKeystoreType() {
		return this.sslKeystoreType;
	}

	public void setSslKeystoreType(String sslKeystoreType) {
		this.sslKeystoreType = sslKeystoreType;
	}

	public String getSslKeystorePassword() {
		return this.sslKeystorePassword;
	}

	public void setSslKeystorePassword(String sslKeystorePassword) {
		this.sslKeystorePassword = sslKeystorePassword;
	}

	public File getSslTruststoreLocation() {
		return this.sslTruststoreLocation;
	}

	public void setSslTruststoreLocation(File sslTruststoreLocation) { this.sslTruststoreLocation = sslTruststoreLocation; }
	
	public String getSslTruststoreType() {
		return this.sslTruststoreType;
	}
	
	public void setSslTruststoreType(String sslTruststoreType) {
		this.sslTruststoreType = sslTruststoreType;
	}

	public String getSslTruststorePassword() {
		return this.sslTruststorePassword;
	}

	public void setSslTruststorePassword(String sslTruststorePassword) {
		this.sslTruststorePassword = sslTruststorePassword;
	}
	
}
