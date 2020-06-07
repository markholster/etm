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

package com.jecstar.etm.gui.rest.services.iib;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.JsonNamespace;
import com.jecstar.etm.server.core.converter.custom.PasswordConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

@JsonNamespace(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE)
public class Node {

    @JsonField("name")
    private String name;
    @JsonField("host")
    private String host;
    @JsonField("port")
    private int port;
    @JsonField("use_tls")
    private boolean useTls;
    @JsonField("username")
    private String username;
    @JsonField(value = "password", converterClass = PasswordConverter.class)
    private String password;
    @JsonField("queue_manager")
    private String queueManager;
    @JsonField("channel")
    private String channel;

    public String getName() {
        return this.name;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public boolean isUseTls() {
        return this.useTls;
    }

    public Node setUseTls(boolean useTls) {
        this.useTls = useTls;
        return this;
    }

    public String getUsername() {
        return this.username;
    }

    public Node setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return this.password;
    }

    public Node setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getQueueManager() {
        return this.queueManager;
    }

    public Node setQueueManager(String queueManager) {
        this.queueManager = queueManager;
        return this;
    }

    public String getChannel() {
        return this.channel;
    }

    public Node setChannel(String channel) {
        this.channel = channel;
        return this;
    }
}
