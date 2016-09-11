package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.List;

public interface IIBFlow {
	
	static final String RUNTIME_PROPERTY_MONITORING = "This/monitoring";
	static final String RUNTIME_PROPERTY_MONITORING_PROFILE = "This/monitoringProfile";

	String getName();
	String getVersion();
	List<IIBNode> getNodes();
	boolean isMonitoringActivated();
	String getMonitoringProfileName();
	void activateMonitoringProfile(String monitoringProfileName);
	String deactivateMonitoringProfile();
}
