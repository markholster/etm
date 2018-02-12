package com.jecstar.etm.gui.rest.services.iib.proxy.v10;

import com.ibm.broker.config.proxy.*;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBApplication;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBIntegrationServer;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBLibrary;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBMessageFlow;
import com.jecstar.etm.server.core.EtmException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IIBIntegrationServerV10Impl implements IIBIntegrationServer {

    private final ExecutionGroupProxy integrationServer;

    IIBIntegrationServerV10Impl(ExecutionGroupProxy executionGroupProxy) {
        this.integrationServer = executionGroupProxy;
    }

    public String getName() {
        try {
            return this.integrationServer.getName();
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public List<IIBApplication> getApplications() {
        try {
            List<IIBApplication> applications = new ArrayList<>();
            Enumeration<ApplicationProxy> applicationProxies = this.integrationServer.getApplications(null);
            while (applicationProxies.hasMoreElements()) {
                applications.add(new IIBApplicationV10Impl(applicationProxies.nextElement()));
            }
            return applications;
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public IIBApplication getApplicationByName(String applicationName) {
        try {
            ApplicationProxy applicationProxy = this.integrationServer.getApplicationByName(applicationName);
            if (applicationProxy == null) {
                return null;
            }
            return new IIBApplicationV10Impl(applicationProxy);
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public List<IIBLibrary> getSharedLibraries() {
        try {
            List<IIBLibrary> libraries = new ArrayList<>();
            Enumeration<SharedLibraryProxy> libraryProxies = this.integrationServer.getSharedLibraries(null);
            while (libraryProxies.hasMoreElements()) {
                libraries.add(new IIBLibraryV10Impl(libraryProxies.nextElement()));
            }
            return libraries;
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }


    public IIBLibrary getSharedLibraryByName(String libraryName) {
        try {
            SharedLibraryProxy libraryProxy = this.integrationServer.getSharedLibraryByName(libraryName);
            if (libraryProxy == null) {
                return null;
            }
            return new IIBLibraryV10Impl(libraryProxy);
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }


    public List<IIBMessageFlow> getMessageFlows() {
        try {
            List<IIBMessageFlow> messageFlows = new ArrayList<>();
            Enumeration<MessageFlowProxy> messageFlowProxies = this.integrationServer.getMessageFlows(null);
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
            MessageFlowProxy messageFlowProxy = this.integrationServer.getMessageFlowByName(flowName);
            if (messageFlowProxy == null) {
                return null;
            }
            return new IIBMessageFlow(messageFlowProxy);
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

}
