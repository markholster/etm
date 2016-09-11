package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy.Node;
import com.jecstar.etm.server.core.EtmException;

public class IIBMessageFlow {

	public static final String RUNTIME_PROPERTY_MONITORING = "This/monitoring";
	public static final String RUNTIME_PROPERTY_MONITORING_PROFILE = "This/monitoringProfile";
	
	private MessageFlowProxy messageFlow;

	protected IIBMessageFlow(MessageFlowProxy messageFlowProxy) {
		this.messageFlow = messageFlowProxy;
	}
	
	public String getName() {
		try {
			return this.messageFlow.getName();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public boolean isMonitoringActivated() {
		try {
			if (this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING) != null
					&& this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING)
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
			return this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
		} catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBNode> getNodes() {
		try {
			List<IIBNode> nodes = new ArrayList<>();
			Enumeration<Node> nodeProxy = this.messageFlow.getNodes();
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
			return this.messageFlow.getVersion();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	public void activateMonitoringProfile(String monitoringProfileName) {
		try {
			String currenProfileName = this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
			if (currenProfileName == null || !currenProfileName.equals(monitoringProfileName)) {
				this.messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE, monitoringProfileName);
			}
			String status = this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING);
			if (status == null || !status.equals("active")) {
				this.messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING, "active");
			}	
		} catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException | ConfigManagerProxyLoggedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	public String deactivateMonitoringProfile() {
		try {
			String status = messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING);
			if ("active".equals(status)) {
				this.messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING, "inactive");
			}
			String currentProfile = messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
			if (currentProfile != null) {
				this.messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE, "");
			}
			return currentProfile;
		} catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException | ConfigManagerProxyLoggedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
}
