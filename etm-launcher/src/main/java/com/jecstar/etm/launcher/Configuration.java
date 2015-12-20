package com.jecstar.etm.launcher;

import java.io.File;

public class Configuration {

	@YamlConfiguration(key = "cluster.name")
	public String clusterName = "Enterprise Telemetry Monitor";
	@YamlConfiguration(key = "instance.name")
	public String instanceName = "Node_1";
	
	@YamlConfiguration(key = "binding.address")
	public String bindingAddress = "0.0.0.0";
	@YamlConfiguration(key = "binding.port.offset")
	public int bindingPortOffset = 0;
	@YamlConfiguration(key = "http.port")
	public int httpPort = 8080;
	
	@YamlConfiguration(key = "https.port")
	public int httpsPort = 8443;
	@YamlConfiguration(key = "ssl.protocol")
	public String sslProtocol = "TLS";
	@YamlConfiguration(key = "ssl.keystore.location")
	public File sslKeystoreLocation;
	@YamlConfiguration(key = "ssl.keystore.type")
	public String sslKeystoreType = "PKCS12";
	@YamlConfiguration(key = "ssl.keystore.password")
	public String sslKeystorePassword;
	@YamlConfiguration(key = "ssl.trusttore.location")
	public File sslTruststoreLocation;
	@YamlConfiguration(key = "ssl.trusttore.type")
	public String sslTruststoreType = "JKS";
	@YamlConfiguration(key = "ssl.trusttore.password")
	public String sslTruststorePassword;
	
	
	// Determines the connection mode: Node or Client  
	@YamlConfiguration(key = "connect.node")
	public boolean connectAsElasticsearchNode = true;
	// Comma separated list of hosts to connect to.
	@YamlConfiguration(key = "connect.addresses")
	public String connectAddresses = "127.0.0.1:9300";
	
	// Configuration for elasticsearch node mode.
	@YamlConfiguration(key = "node.data")
	public boolean nodeData = true;
	@YamlConfiguration(key = "node.path.home")
	public String nodeHomePath = "./";
	@YamlConfiguration(key = "node.path.data")
	public String nodeDataPath = "./data";
	@YamlConfiguration(key = "node.path.data")
	public String nodeLogPath = "./log";
	@YamlConfiguration(key = "node.multicast")
	public boolean nodeMulticast = true;

	@YamlConfiguration(key = "processor.rest.enabled")
	public boolean restProcessorEnabled = true;

	public int getHttpPort() {
		if (this.httpPort > 0) {
			return this.httpPort + this.bindingPortOffset; 
		}
		return this.httpPort;
	}
	
	public int getHttpsPort() {
		if (this.httpsPort > 0) {
			return this.httpsPort + this.bindingPortOffset; 
		}
		return this.httpsPort;
	}
	
	public boolean isHttpServerNecessary() {
		return this.restProcessorEnabled;
	}
	
	public boolean isProcessorNecessary() {
		return this.restProcessorEnabled;
	}
	
}
