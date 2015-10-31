package com.jecstar.etm.launcher;

public class Configuration {

	public String clusterName = "Enterprise Telemetry Monitor";
	public String nodeName = "Node_1";
	
	public String bindingAddress = "0.0.0.0";
	public int bindingPortOffset = 0;
	public int httpPort = 8080;
	
	public boolean nodeData = true;
	public String dataPath = "data";
	
	public boolean restProcessorEnabled = true;
	
	public boolean isHttpServerNecessary() {
		return this.restProcessorEnabled;
	}
	
	public boolean isProcessorNecessary() {
		return this.restProcessorEnabled;
	}
	
}
