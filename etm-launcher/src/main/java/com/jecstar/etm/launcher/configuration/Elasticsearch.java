package com.jecstar.etm.launcher.configuration;

public class Elasticsearch {

	// Determines the connection mode: Node or Client  
	public boolean connectAsNode = false;
	// Comma separated list of hosts to connect to.
	public String connectAddresses = "127.0.0.1:9300";

	// Configuration for elasticsearch node mode.
	public int transportPort = 9300;
	public boolean nodeData = true;
	public String nodeHomePath = "./";
	public String nodeDataPath = "./data";
	public String nodeLogPath = "./log";
	public boolean nodeMulticast = true;

	
}
