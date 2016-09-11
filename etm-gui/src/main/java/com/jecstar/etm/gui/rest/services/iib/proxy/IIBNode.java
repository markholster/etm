package com.jecstar.etm.gui.rest.services.iib.proxy;

import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy.Node;

public class IIBNode {

	private Node node;

	protected IIBNode(MessageFlowProxy.Node node) {
		this.node = node;
	}
	
	public String getName() {
		return this.node.getName();
	}

	public String getType() {
		return this.node.getType();
	}

	public boolean isSupported() {
		return this.node.getType().startsWith("ComIbmMQ") && !"ComIbmMQHeaderNode".equals(this.node.getType());
	}

	public boolean isMonitoringSetInProfile(String profile) {
		if (profile == null) {
			return false;
		}
		return profile.indexOf("profile:eventSourceAddress=\"" + getName() + ".") >= 0;
	}


}
