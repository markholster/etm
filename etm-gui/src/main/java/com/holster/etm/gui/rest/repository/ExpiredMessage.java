package com.holster.etm.gui.rest.repository;

import java.util.Date;

public class ExpiredMessage {
	
	private String name;
	
	private String application;

	private Date startTime;
	
	private Date expirationTime;

	public ExpiredMessage(String name, Date startTime, Date expirationTime, String application) {
	    this.name = name;
	    this.startTime = startTime;
	    this.expirationTime = expirationTime;
	    this.application = application;
    }

	public String getName() {
		return this.name;
	}

	public String getApplication() {
		return this.application;
	}

	public Date getStartTime() {
		return this.startTime;
	}

	public Date getExpirationTime() {
		return this.expirationTime;
	}

}
