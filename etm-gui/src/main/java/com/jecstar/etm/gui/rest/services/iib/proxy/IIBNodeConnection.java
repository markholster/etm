package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.List;

import com.ibm.broker.config.proxy.ConfigurableService;
import com.jecstar.etm.gui.rest.services.iib.Node;

public interface IIBNodeConnection extends AutoCloseable {
	
	Node getNode();
	void connect();
	
	
	IIBIntegrationServer getServerByName(String serverName);
	List<IIBIntegrationServer> getServers();
	void setSynchronous(int timeout);
	ConfigurableService getConfigurableService(String type, String name);
	void createConfigurableService(String type, String name);
	void deleteConfigurableService(String type, String name);
	
	@Override
	void close();

}
