package com.jecstar.etm.gui.rest;

public class ClusterStatus {

	public String clusterName;
	
	public StatusCode statusCode;

	public int numberOfNodes;

	public int numberOfDataNodes;

	public int numberOfActivePrimaryShards;

	public int numberOfActiveShards;

	public int numberOfRelocatingShards;

	public int numberOfInitializingShards;

	public int numberOfUnassignedShards;

	public int numberOfDelayedUnassignedShards;

	public int numberOfPendingTasks;

	public int numberOfInFlightFethch;
}
