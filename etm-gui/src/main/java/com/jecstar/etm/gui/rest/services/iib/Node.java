package com.jecstar.etm.gui.rest.services.iib;

import com.ibm.broker.config.proxy.BrokerProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyException;
import com.ibm.broker.config.proxy.MQBrokerConnectionParameters;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class Node {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Node.class);
	
	private final String name;
	private final String host;
	private final int port; 
	private final String queueManager;
	private String channel;

	
	Node(String name, String host, int port, String queueManager) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.queueManager = queueManager;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String getQueueManager() {
		return this.queueManager;
	}
	
	public String getChannel() {
		return this.channel;
	}
	
	public void setChannel(String channel) {
		this.channel = channel;
	}
	
	protected BrokerProxy connect() {
		MQBrokerConnectionParameters bcp = new MQBrokerConnectionParameters(getHost(), getPort(), getQueueManager());
		if (getChannel()!= null) {
			bcp.setAdvancedConnectionParameters(getChannel(), null, null, -1, -1, null);
		}
		BrokerProxy brokerProxy = null;
		try {
			String message = "Connecting to the integration node running at " + getHost() + ":" + getPort() + " with queuemanager '" + getQueueManager() + "'";
			if (getChannel() != null) {
				message += " and channel '" + getChannel() + "'.";
			} else {
				message += ".";
			}
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage(message);
			}
			brokerProxy = BrokerProxy.getInstance(bcp);

			if (!brokerProxy.hasBeenPopulatedByBroker(true)) {
				if (log.isWarningLevelEnabled()) { 
					log.logWarningMessage("Integration node '" + getHost() + ":" + getPort() + "' is not responding.");
				}
				throw new EtmException(EtmException.IIB_CONNECTION_ERROR);
			}
			return brokerProxy;
		} catch (ConfigManagerProxyException e) {
			if (log.isErrorLevelEnabled()) { 
				log.logErrorMessage("Unable to connect to integration node '" + getHost() + ":" + getPort() + "'.", e);
			}
			throw new EtmException(EtmException.IIB_CONNECTION_ERROR, e);
		}
	}
}
