package com.jecstar.etm.gui.rest.services.iib.proxy.v10;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.MessageFlowProxy.Node;
import com.ibm.broker.config.proxy.SubFlowProxy;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBNode;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBSubFlow;
import com.jecstar.etm.server.core.EtmException;

public class IIBSubFlowV10Impl implements IIBSubFlow {

	private final SubFlowProxy subFlow;

	IIBSubFlowV10Impl(SubFlowProxy subFlowProxy) {
		this.subFlow = subFlowProxy;
	}
	
	public String getName() {
		try {
			return this.subFlow.getName();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBNode> getNodes() {
		try {
			List<IIBNode> nodes = new ArrayList<>();
			Enumeration<Node> nodeProxy = this.subFlow.getNodes();
			while (nodeProxy.hasMoreElements()) {
				nodes.add(new IIBNode(nodeProxy.nextElement()));
			}
			return nodes;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}		
	}

	public String getVersion() {
		try {
			return this.subFlow.getVersion();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
}
