package com.jecstar.etm.core.domain;

import java.net.InetAddress;

public class Application {
	
	/**
	 * The name of the application.
	 */
	public String name;
	
	/**
	 * The hostAddress of the application.
	 */
	public InetAddress hostAddress;
	
	/**
	 * The instance of the application. Useful if the application is clustered.
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
		this.hostAddress = null;
		this.instance = null;
		this.principal = null;
		this.version = null;
		return this;
	}

	public Application initialize(Application copy) {
		this.initialize();
		if (copy == null) {
			return this;
		}
		this.name = copy.name;
		this.hostAddress = copy.hostAddress;
		this.instance = copy.instance;
		this.principal = copy.principal;
		this.version = copy.version;
		return this;
	}

	public boolean isSet() {
		if (this.name != null || this.instance != null || this.principal != null || this.version != null || this.hostAddress != null) {
			return true;
		}
		return false; 
	} 

}
