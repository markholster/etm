package com.jecstar.etm.processor.ibmmq.configuration;

public class Destination {

	private String name;
	private String type;
	private int nrOfListeners;
	private int destinationReadOptions = 0;
	
	
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
		return this.nrOfListeners;
	}
	
	public void setNrOfListeners(int nrOfListeners) {
		if (nrOfListeners < 1 || nrOfListeners > 65535) {
			throw new IllegalArgumentException(nrOfListeners + " is an invalid number of listeners");
		}
		this.nrOfListeners = nrOfListeners;
	}
	
	public int getDestinationReadOptions() {
		return this.destinationReadOptions;
	}
	
	public void setDestinationReadOptions(int destinationReadOptions) {
		if (destinationReadOptions < 0) {
			throw new IllegalArgumentException(destinationReadOptions + " is an invalid destination read option number");
		}
		this.destinationReadOptions = destinationReadOptions;
	}
}
