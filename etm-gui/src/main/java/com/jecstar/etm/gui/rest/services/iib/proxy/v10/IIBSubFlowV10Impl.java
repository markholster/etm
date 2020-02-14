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

import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.MessageFlowProxy.Node;
import com.ibm.broker.config.proxy.SubFlowProxy;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBNode;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBSubFlow;
import com.jecstar.etm.server.core.EtmException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
