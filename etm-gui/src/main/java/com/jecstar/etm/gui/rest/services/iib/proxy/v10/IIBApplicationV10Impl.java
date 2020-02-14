/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest.services.iib.proxy.v10;

import com.ibm.broker.config.proxy.*;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBApplication;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBLibrary;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBMessageFlow;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBSubFlow;
import com.jecstar.etm.server.core.EtmException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IIBApplicationV10Impl implements IIBApplication {

    private final ApplicationProxy application;

    IIBApplicationV10Impl(ApplicationProxy applicationProxy) {
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
            Enumeration<StaticLibraryProxy> libraryProxies = this.application.getStaticLibraries(null);
            while (libraryProxies.hasMoreElements()) {
                libraries.add(new IIBLibraryV10Impl(libraryProxies.nextElement()));
            }
            return libraries;
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public IIBLibrary getLibraryByName(String libraryName) {
        try {
            StaticLibraryProxy libraryByProxy = this.application.getStaticLibraryByName(libraryName);
            if (libraryByProxy == null) {
                return null;
            }
            return new IIBLibraryV10Impl(libraryByProxy);
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
        try {
            List<IIBSubFlow> subFlows = new ArrayList<>();
            Enumeration<SubFlowProxy> subFlowProxies = this.application.getSubFlows(null);
            while (subFlowProxies.hasMoreElements()) {
                subFlows.add(new IIBSubFlowV10Impl(subFlowProxies.nextElement()));
            }
            return subFlows;
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public IIBSubFlow getSubFlowByName(String subFlowName) {
        try {
            SubFlowProxy subFlowProxy = this.application.getSubFlowByName(subFlowName);
            if (subFlowProxy == null) {
                return null;
            }
            return new IIBSubFlowV10Impl(subFlowProxy);
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
