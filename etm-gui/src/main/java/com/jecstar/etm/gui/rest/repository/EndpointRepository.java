package com.jecstar.etm.gui.rest.repository;

import java.util.List;

public interface EndpointRepository {

	public List<String> getEndpointNames();

	public EndpointConfiguration getEndpointConfiguration(String endpointName);
	
	public void deleteEndpointConfiguration(String endpointName);
	
	public void updateEnpointConfiguration(EndpointConfiguration endpointConfiguration);
	
}
