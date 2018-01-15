package com.jecstar.etm.gui.rest.services.iib.proxy;

import com.ibm.broker.config.proxy.ConfigurableService;
import com.jecstar.etm.gui.rest.services.iib.Node;

import java.util.List;

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
