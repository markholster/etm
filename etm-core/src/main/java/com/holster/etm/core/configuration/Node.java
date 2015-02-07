package com.holster.etm.core.configuration;

public class Node {

	private String name;
	private boolean active;

	public Node(String name, boolean active) {
		this.name = name;
		this.active = active;
	}
	
	public String getName() {
	    return this.name;
    }
	
	public boolean isActive() {
	    return this.active;
    }
}
