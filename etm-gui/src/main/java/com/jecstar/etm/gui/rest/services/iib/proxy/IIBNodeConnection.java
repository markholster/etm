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

import com.ibm.broker.config.proxy.ConfigurableService;
import com.jecstar.etm.gui.rest.services.iib.Node;

import java.util.List;

public interface IIBNodeConnection extends AutoCloseable {

    Node getNode();

    void connect();


    IIBIntegrationServer getServerByName(String serverName);

    List<IIBIntegrationServer> getServers();

    void setSynchronous(int timeout);

    ConfigurableService getConfigurableService(String type, String name);

    void createConfigurableService(String type, String name);

    void deleteConfigurableService(String type, String name);

    @Override
    void close();

}
