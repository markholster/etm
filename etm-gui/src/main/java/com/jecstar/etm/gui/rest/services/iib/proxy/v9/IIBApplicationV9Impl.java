package com.jecstar.etm.gui.rest.services.iib.proxy.v9;

import com.ibm.broker.config.proxy.ApplicationProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.LibraryProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBApplication;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBLibrary;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBMessageFlow;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBSubFlow;
import com.jecstar.etm.server.core.EtmException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class IIBApplicationV9Impl implements IIBApplication {

    private final ApplicationProxy application;

    IIBApplicationV9Impl(ApplicationProxy applicationProxy) {
        this.application = applicationProxy;
    }

    public String getName() {
        try {
            return this.application.getName();
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    @SuppressWarnings("deprecation")
    public List<IIBLibrary> getLibraries() {
        try {
            List<IIBLibrary> libraries = new ArrayList<>();
            Enumeration<LibraryProxy> libraryProxies = this.application.getLibraries(null);
            while (libraryProxies.hasMoreElements()) {
                libraries.add(new IIBLibraryV9Impl(libraryProxies.nextElement()));
            }
            return libraries;
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    @SuppressWarnings("deprecation")
    public IIBLibrary getLibraryByName(String libraryName) {
        try {
            LibraryProxy libraryByProxy = this.application.getLibraryByName(libraryName);
            if (libraryByProxy == null) {
                return null;
            }
            return new IIBLibraryV9Impl(libraryByProxy);
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
        return Collections.emptyList();
    }


    public IIBSubFlow getSubFlowByName(String subFlowName) {
        return null;
    }

    public String getVersion() {
        try {
            return this.application.getVersion();
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }
}
