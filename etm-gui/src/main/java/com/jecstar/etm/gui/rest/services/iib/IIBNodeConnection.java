package com.jecstar.etm.gui.rest.services.iib;

import java.io.Closeable;
import java.util.Enumeration;

import com.ibm.broker.config.proxy.BrokerProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyException;
import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.ConfigurableService;
import com.ibm.broker.config.proxy.ExecutionGroupProxy;
import com.ibm.broker.config.proxy.MQBrokerConnectionParameters;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class IIBNodeConnection implements Closeable {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(IIBNodeConnection.class);


	private BrokerProxy brokerProxy;
	private Node node;
	
	protected IIBNodeConnection(Node node) {
		this.node = node;
	}
	
	protected Node getNode() {
		return node;
	}

	protected void connect() {
		MQBrokerConnectionParameters bcp = new MQBrokerConnectionParameters(this.node.getHost(), this.node.getPort(), this.node.getQueueManager());
		if (this.node.getChannel()!= null) {
			bcp.setAdvancedConnectionParameters(this.node.getChannel(), null, null, -1, -1, null);
		}
		try {
			String message = "Connecting to the integration node running at " + this.node.getHost() + ":" + this.node.getPort() + " with queuemanager '" + this.node.getQueueManager() + "'";
			if (this.node.getChannel() != null) {
				message += " and channel '" + this.node.getChannel() + "'.";
			} else {
				message += ".";
			}
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage(message);
			}
			this.brokerProxy = BrokerProxy.getInstance(bcp);

			if (!this.brokerProxy.hasBeenPopulatedByBroker(true)) {
				if (log.isWarningLevelEnabled()) { 
					log.logWarningMessage("Integration node '" + this.node.getHost() + ":" + this.node.getPort() + "' is not responding.");
				}
				throw new EtmException(EtmException.IIB_CONNECTION_ERROR);
			}
		} catch (ConfigManagerProxyException e) {
			if (log.isErrorLevelEnabled()) { 
				log.logErrorMessage("Unable to connect to integration node '" + this.node.getHost() + ":" + this.node.getPort() + "'.", e);
			}
			throw new EtmException(EtmException.IIB_CONNECTION_ERROR, e);
		}
	}
	
	protected ExecutionGroupProxy getServerByName(String serverName) {
		try {
			return this.brokerProxy.getExecutionGroupByName(serverName);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	protected Enumeration<ExecutionGroupProxy> getServers() {
		try {
			return this.brokerProxy.getExecutionGroups(null);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	protected void setSynchronous(int timeout) {
		this.brokerProxy.setSynchronous(timeout);
	}

	protected ConfigurableService getConfigurableService(String type, String name) {
		try {
			return this.brokerProxy.getConfigurableService(type, name);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	protected void createConfigurableService(String type, String name) {
		try {
			this.brokerProxy.createConfigurableService(type, name);
		} catch (ConfigManagerProxyLoggedException | IllegalArgumentException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	protected void deleteConfigurableService(String type, String name) {
		try {
			this.brokerProxy.deleteConfigurableService(type, name);
		} catch (ConfigManagerProxyLoggedException | IllegalArgumentException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	@Override
	public void close() {
		if (this.brokerProxy != null) {
			this.brokerProxy.disconnect();
		}
	}

}
