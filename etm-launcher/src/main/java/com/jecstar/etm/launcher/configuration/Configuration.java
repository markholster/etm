package com.jecstar.etm.launcher.configuration;

public class Configuration {

	public String clusterName = "Enterprise Telemetry Monitor";
	public String instanceName = "Node_1";
	
	public Binding binding = new Binding();
	public Elasticsearch elasticSearch = new Elasticsearch();

	public boolean restProcessorEnabled = true;

	
	public boolean isHttpServerNecessary() {
		return this.restProcessorEnabled;
	}
	
	public boolean isProcessorNecessary() {
		return this.restProcessorEnabled;
	}
	
}
