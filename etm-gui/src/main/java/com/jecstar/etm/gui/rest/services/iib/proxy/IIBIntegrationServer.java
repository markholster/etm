package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.List;

public interface IIBIntegrationServer {

	String getName();
	List<IIBApplication> getApplications();
	IIBApplication getApplicationByName(String applicationName);

	List<IIBLibrary> getSharedLibraries();
	IIBLibrary getSharedLibraryByName(String libraryName);
	
	List<IIBMessageFlow> getMessageFlows();
	IIBMessageFlow getMessageFlowByName(String flowName);

}
