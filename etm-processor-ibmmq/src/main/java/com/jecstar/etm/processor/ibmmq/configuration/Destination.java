package com.jecstar.etm.processor.ibmmq.configuration;

import com.ibm.mq.constants.CMQC;

public class Destination {

	private String name;
	private String type = "queue";
	private int nrOfListeners = 5;
	
	private int commitCount = 1000;
	private int commitTime = 30000;
	private int destinationGetOptions = CMQC.MQGMO_WAIT + CMQC.MQGMO_FAIL_IF_QUIESCING + CMQC.MQGMO_CONVERT + CMQC.MQGMO_SYNCPOINT;
	private int destinationOpenOptions = CMQC.MQOO_INQUIRE + CMQC.MQOO_FAIL_IF_QUIESCING + CMQC.MQOO_INPUT_SHARED;
	
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getType() {
		return this.type;
	}
	
	public void setType(String type) {
		if (!"queue".equalsIgnoreCase(type)) {
			throw new IllegalArgumentException("'" + type + "' is an invalid destination type.");
		}
		this.type = type;
	}
	
	public int getNrOfListeners() {
		return this.nrOfListeners;
	}
	
	public void setNrOfListeners(int nrOfListeners) {
		if (nrOfListeners < 1 || nrOfListeners > 65535) {
			throw new IllegalArgumentException(nrOfListeners + " is an invalid number of listeners");
		}
		this.nrOfListeners = nrOfListeners;
	}
	
	public int getCommitCount() {
		return this.commitCount;
	}
	
	public void setCommitCount(int commitCount) {
		if (commitCount < 0) {
			throw new IllegalArgumentException(commitCount + " is an invalid commit count");
		}
		this.commitCount = commitCount;
	}
	
	public int getDestinationGetOptions() {
		return this.destinationGetOptions;
	}
	
	public int getCommitTime() {
		return this.commitTime;
	}
	
	public void setCommitTime(int commitTime) {
		if (commitTime < 0) {
			throw new IllegalArgumentException(commitTime + " is an invalid commit time");
		}
		this.commitTime = commitTime;
	}
	
	public void setDestinationGetOptions(int destinationGetOptions) {
		if (destinationGetOptions < 0) {
			throw new IllegalArgumentException(destinationGetOptions + " is an invalid destination get option number");
		}
		this.destinationGetOptions = destinationGetOptions;
	}
	
	public int getDestinationOpenOptions() {
		return this.destinationOpenOptions;
	}
	
	public void setDestinationOpenOptions(int destinationOpenOptions) {
		if (destinationOpenOptions < 0) {
			throw new IllegalArgumentException(destinationOpenOptions + " is an invalid destination open option number");
		}
		this.destinationOpenOptions = destinationOpenOptions;
	}
}
