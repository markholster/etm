package com.jecstar.etm.core.domain;

public class Application {
	
	/**
	 * The name of the application.
	 */
	public String name;
	
	/**
	 * The instance of the application. Useful if the application is clustered, or to store the IP address of the browser in case of a http request.
	 */
	public String instance;
	
	/**
	 * The principal that executed the action that triggers the event.
	 */
	public String principal;

	/**
	 * The version of the application.
	 */
	public String version;
	
	public Application initialize() {
		this.name = null;
		this.instance = null;
		this.principal = null;
		this.version = null;
		return this;
	}

	public Application initialize(Application copy) {
		this.initialize();
		this.name = copy.name;
		this.instance = copy.instance;
		this.principal = copy.instance;
		this.version = copy.version;
		return this;
	}

	public boolean isSet() {
		if (this.name != null || this.instance != null || this.principal != null || this.version != null) {
			return false;
		}
		return true; 
	} 

}
