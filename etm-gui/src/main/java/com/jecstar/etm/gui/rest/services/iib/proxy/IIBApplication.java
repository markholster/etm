package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ibm.broker.config.proxy.ApplicationProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.LibraryProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.jecstar.etm.gui.rest.IIBApi;
import com.jecstar.etm.server.core.EtmException;

public class IIBApplication {

	private ApplicationProxy application;

	protected IIBApplication(ApplicationProxy applicationProxy) {
		this.application = applicationProxy;
	}

	public String getName() {
		try {
			return this.application.getName();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBLibrary> getLibraries() {
		try {
			List<IIBLibrary> libraries = new ArrayList<>();
			Enumeration<LibraryProxy> libraryProxies = this.application.getLibraries(null);
			while (libraryProxies.hasMoreElements()) {
				libraries.add(new IIBLibrary(libraryProxies.nextElement()));
			}
			return libraries;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}		
	}
	
	public IIBLibrary getLibraryByName(String libraryName) {
		try {
			LibraryProxy libraryByProxy = this.application.getLibraryByName(libraryName);
			if (libraryByProxy == null) {
				return null;
			}
			return new IIBLibrary(libraryByProxy);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) { 
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}	
	}
	
	public List<IIBMessageFlow> getMessageFlows() {
		try {
			List<IIBMessageFlow> messageFlows = new ArrayList<>();
			Enumeration<MessageFlowProxy> messageFlowProxies = this.application.getMessageFlows(null);
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
			MessageFlowProxy messageFlowProxy = this.application.getMessageFlowByName(flowName);
			if (messageFlowProxy == null) {
				return null;
			}
			return new IIBMessageFlow(messageFlowProxy);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBSubFlow> getSubFlows() {
		if (!IIBApi.IIB_SUBFLOW_PROXY_AVAILABLE) {
			return new ArrayList<>();
		}
		// Actual call to subflow retrieval is done in a separate method to prevent runtime errors when IIB 9 api is on the classpath.
		return getSubFlowsWhenSubflowApiPresent();
	}
	
	private List<IIBSubFlow> getSubFlowsWhenSubflowApiPresent() {
		try {
			List<IIBSubFlow> subFlows = new ArrayList<>();
			Enumeration<com.ibm.broker.config.proxy.SubFlowProxy> subFlowProxies = this.application.getSubFlows(null);
			while (subFlowProxies.hasMoreElements()) {
				subFlows.add(new IIBSubFlow(subFlowProxies.nextElement()));
			}
			return subFlows;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}				
	}
	
	public IIBSubFlow getSubFlowByName(String subFlowName) {
		if (!IIBApi.IIB_SUBFLOW_PROXY_AVAILABLE) {
			return null;
		}
		// Actual call to subflow retrieval is done in a separate method to prevent runtime errors when IIB 9 api is on the classpath.
		return getSubflowByNameWhenSubflowApiPresent(subFlowName);
	}
	
	private IIBSubFlow getSubflowByNameWhenSubflowApiPresent(String subflowName) {
		try {
			com.ibm.broker.config.proxy.SubFlowProxy subFlowProxy = this.application.getSubFlowByName(subflowName);
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
			return this.application.getVersion();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
}
