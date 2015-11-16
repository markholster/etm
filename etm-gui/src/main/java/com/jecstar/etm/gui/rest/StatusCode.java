package com.jecstar.etm.gui.rest;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;

public enum StatusCode {
	OK, 
	WARNING, 
	ERROR;
	
	public static StatusCode fromClusterHealthStatus(ClusterHealthStatus clusterHealthStatus) {
		switch (clusterHealthStatus) {
		case GREEN:
			return StatusCode.OK;
		case YELLOW:
			return StatusCode.WARNING;
		case RED:
			return StatusCode.ERROR;
		default:
			throw new IllegalArgumentException("Unknowns ClusterHealthStatus '" + clusterHealthStatus.name() + "'");
		}

	}
};
