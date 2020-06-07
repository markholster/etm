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

package com.jecstar.etm.launcher.configuration;

import com.jecstar.etm.processor.ibmmq.configuration.IbmMq;
import com.jecstar.etm.processor.jms.configuration.Jms;
import com.jecstar.etm.processor.kafka.configuration.Kafka;
import com.jecstar.etm.signaler.configuration.Signaler;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Configuration {

    public String clusterName = "Enterprise Telemetry Monitor";
    public String instanceName = "Node_1";
    public String bindingAddress = "127.0.0.1";
    // In an ideal world this would be a char[], but the snakeyml doesn't support that type. The data will always
    // be read into a ScalarNode which contains a value of String type.
    public String secret;

    public String licenseUpdateUrl;

    public final Elasticsearch elasticsearch = new Elasticsearch();
    public final Http http = new Http();
    public final Logging logging = new Logging();
    public final Signaler signaler = new Signaler();

    public final IbmMq ibmMq = new IbmMq();
    public final Jms jms = new Jms();
    public final Kafka kafka = new Kafka();

    public boolean isHttpServerNecessary() {
        return this.http.restProcessorEnabled || this.http.guiEnabled;
    }

    public int calculateInstanceHash() {
        int hash = 0;
        hash += this.clusterName == null ? 0 : this.clusterName.hashCode();
        hash += this.instanceName == null ? 0 : this.instanceName.hashCode();
        try {
            hash += this.bindingAddress == null ? 0 : InetAddress.getByName(this.bindingAddress).hashCode();
        } catch (UnknownHostException e) {
            hash += this.bindingAddress == null ? 0 : this.bindingAddress.hashCode();
        }
        hash += this.elasticsearch.calculateInstanceHash();
        hash += this.http.calculateInstanceHash();
        return hash;
    }
}
