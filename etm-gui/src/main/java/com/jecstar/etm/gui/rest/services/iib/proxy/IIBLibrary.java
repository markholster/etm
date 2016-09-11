package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.LibraryProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.jecstar.etm.gui.rest.IIBApi;
import com.jecstar.etm.server.core.EtmException;

public class IIBLibrary {

	private LibraryProxy library;

	protected IIBLibrary(LibraryProxy libraryProxy) {
		this.library = libraryProxy;
	}
	
	public String getName() {
		try {
			return this.library.getName();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBMessageFlow> getMessageFlows() {
		try {
			List<IIBMessageFlow> messageFlows = new ArrayList<>();
			Enumeration<MessageFlowProxy> messageFlowProxies = this.library.getMessageFlows(null);
			while (messageFlowProxies.hasMoreElements()) {
				messageFlows.add(new IIBMessageFlow(messageFlowProxies.nextElement()));
			}
			return messageFlows;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}		
	}
	
	public IIBMessageFlow getMessageFlowByName(String flowName) {
		try {
			MessageFlowProxy messageFlowProxy = this.library.getMessageFlowByName(flowName);
			if (messageFlowProxy == null) {
				return null;
			}
			return new IIBMessageFlow(messageFlowProxy);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBSubFlow> getSubFlows() {
		try {
			List<IIBSubFlow> subFlows = new ArrayList<>();
			if (IIBApi.IIB_SUBFLOW_PROXY_AVAILABLE) {
				Enumeration<com.ibm.broker.config.proxy.SubFlowProxy> subFlowProxies = this.library.getSubFlows(null);
				while (subFlowProxies.hasMoreElements()) {
					subFlows.add(new IIBSubFlow(subFlowProxies.nextElement()));
				}
			}
			return subFlows;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}		
	}
	
	public IIBSubFlow getSubFlowByName(String flowName) {
		if (!IIBApi.IIB_SUBFLOW_PROXY_AVAILABLE) {
			return null;
		}
		try {
			com.ibm.broker.config.proxy.SubFlowProxy subFlowProxy = this.library.getSubFlowByName(flowName);
			if (subFlowProxy == null) {
				return null;
			}
			return new IIBSubFlow(subFlowProxy);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	public String getVersion() {
		try {
			return this.library.getVersion();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
}
