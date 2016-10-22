package com.jecstar.etm.processor.ibmmq.configuration;

import com.ibm.mq.constants.CMQC;

public class Destination {

	private String name;
	private String type = "queue";
	private int nrOfListeners = 1;
	private String messagesType = "auto"; // iibevent, etmevent, clone  
	
	private int maxMessageSize = 1024 * 1024 * 4;
	private int commitSize = 500;
	private int commitInterval = 10000;
	private int destinationGetOptions = CMQC.MQGMO_WAIT + CMQC.MQGMO_FAIL_IF_QUIESCING + CMQC.MQGMO_CONVERT + CMQC.MQGMO_SYNCPOINT + CMQC.MQGMO_LOGICAL_ORDER + CMQC.MQGMO_ALL_SEGMENTS_AVAILABLE + CMQC.MQGMO_COMPLETE_MSG;
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
		if (!"queue".equalsIgnoreCase(type) && !"topic".equalsIgnoreCase(type)) {
			throw new IllegalArgumentException("'" + type + "' is an invalid destination type.");
		}
		this.type = type;
	}
	
	public int getNrOfListeners() {
		if ("topic".equalsIgnoreCase(type)) {
			return 1;
		}
		return this.nrOfListeners;
	}
	
	public void setNrOfListeners(int nrOfListeners) {
		if (nrOfListeners < 1 || nrOfListeners > 65535) {
			throw new IllegalArgumentException(nrOfListeners + " is an invalid number of listeners");
		}
		this.nrOfListeners = nrOfListeners;
	}
	
	public String getMessagesType() {
		return this.messagesType;
	}
	
	public void setMessagesType(String messagesType) {
		if (!"auto".equalsIgnoreCase(messagesType) 
				&& !"iibevent".equalsIgnoreCase(messagesType)
				&& !"etmevent".equalsIgnoreCase(messagesType)
				&& !"clone".equalsIgnoreCase(messagesType)) {
			throw new IllegalArgumentException("'" + messagesType + "' is an invalid messages type.");
		}
		this.messagesType = messagesType;
	}
	
	public int getMaxMessageSize() {
		return this.maxMessageSize;
	}
	
	public void setMaxMessageSize(int maxMessageSize) {
		if (maxMessageSize < 1) {
			throw new IllegalArgumentException(maxMessageSize + " is an invalid max message size");
		}
		this.maxMessageSize = maxMessageSize;
	}
	
	
	public int getCommitSize() {
		return this.commitSize;
	}
	
	public void setCommitSize(int commitSize) {
		if (commitSize < 0) {
			throw new IllegalArgumentException(commitSize + " is an invalid commit size");
		}
		this.commitSize = commitSize;
	}
	
	public int getDestinationGetOptions() {
		return this.destinationGetOptions;
	}
	
	public int getCommitInterval() {
		return this.commitInterval;
	}
	
	public void setCommitInterval(int commitInterval) {
		if (commitInterval < 0) {
			throw new IllegalArgumentException(commitInterval + " is an invalid commit interval");
		}
		this.commitInterval = commitInterval;
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
