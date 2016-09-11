package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.MessageFlowProxy.Node;
import com.jecstar.etm.server.core.EtmException;

public class IIBSubFlow implements IIBFlow {

	// Fully qualified name because otherwise the class loading will fail when the IIB 9 configmanagerproxy.jar is provided.
	private com.ibm.broker.config.proxy.SubFlowProxy subFlow;

	protected IIBSubFlow(com.ibm.broker.config.proxy.SubFlowProxy subFlowProxy) {
		this.subFlow = subFlowProxy;
	}
	
	public String getName() {
		try {
			return this.subFlow.getName();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public boolean isMonitoringActivated() {
		try {
			if (this.subFlow.getRuntimeProperty(IIBFlow.RUNTIME_PROPERTY_MONITORING) != null
					&& this.subFlow.getRuntimeProperty(IIBFlow.RUNTIME_PROPERTY_MONITORING)
							.equals("active")) {
				return true;
			}
		} catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}				
		return false;
	}
	
	public String getMonitoringProfileName() {
		try {
			return this.subFlow.getRuntimeProperty(IIBFlow.RUNTIME_PROPERTY_MONITORING_PROFILE);
		} catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException e) {
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

	public void activateMonitoringProfile(String monitoringProfileName) {
		try {
			String currenProfileName = this.subFlow.getRuntimeProperty(IIBFlow.RUNTIME_PROPERTY_MONITORING_PROFILE);
			if (currenProfileName == null || !currenProfileName.equals(monitoringProfileName)) {
				this.subFlow.setRuntimeProperty(IIBFlow.RUNTIME_PROPERTY_MONITORING_PROFILE, monitoringProfileName);
			}
			String status = this.subFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING);
			if (status == null || !status.equals("active")) {
				this.subFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING, "active");
			}	
		} catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException | ConfigManagerProxyLoggedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	public String deactivateMonitoringProfile() {
		try {
			String status = subFlow.getRuntimeProperty(IIBFlow.RUNTIME_PROPERTY_MONITORING);
			if ("active".equals(status)) {
				this.subFlow.setRuntimeProperty(IIBFlow.RUNTIME_PROPERTY_MONITORING, "inactive");
			}
			String currentProfile = subFlow.getRuntimeProperty(IIBFlow.RUNTIME_PROPERTY_MONITORING_PROFILE);
			if (currentProfile != null) {
				this.subFlow.setRuntimeProperty(IIBFlow.RUNTIME_PROPERTY_MONITORING_PROFILE, "");
			}
			return currentProfile;
		} catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException | ConfigManagerProxyLoggedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
}
