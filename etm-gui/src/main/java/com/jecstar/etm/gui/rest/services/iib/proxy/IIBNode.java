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

package com.jecstar.etm.gui.rest.services.iib.proxy;

import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy.Node;

public class IIBNode {

    private final Node node;

    public IIBNode(MessageFlowProxy.Node node) {
        this.node = node;
    }

    public String getName() {
        return this.node.getName();
    }

    public String getType() {
        return this.node.getType();
    }

    public boolean isSupported() {
        return
                this.node.getType().equals("SubFlowNode")
                        || (this.node.getType().startsWith("ComIbmMQ") && !this.node.getType().equals("ComIbmMQHeaderNode"))
                        || this.node.getType().equals("ComIbmPublication")
//			|| this.node.getType().startsWith("ComIbmREST")
                        || (this.node.getType().startsWith("ComIbmHTTP") && !this.node.getType().equals("ComIbmHTTPHeader"))
                        || this.node.getType().startsWith("ComIbmWS")
                        || (this.node.getType().startsWith("ComIbmSOAP") && !this.node.getType().equals("ComIbmSOAPWrapperNode") && !this.node.getType().equals("ComIbmSOAPExtractNode"))
                ;
    }

    public String getProperty(String key) {
        return this.node.getProperties().getProperty(key);
    }


}
