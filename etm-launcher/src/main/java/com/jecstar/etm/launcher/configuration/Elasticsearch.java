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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Elasticsearch {

    public String clusterName = "elasticsearch";

    public List<String> connectAddresses = new ArrayList<>();

    public boolean waitForConnectionOnStartup = false;

    public String username;
    public String password;

    public File sslTrustStoreLocation;
    public String sslTrustStorePassword;

    public Elasticsearch() {
        this.connectAddresses.add("127.0.0.1:9200");
    }

    public int calculateInstanceHash() {
        int hash = 0;
        hash += this.clusterName == null ? 0 : this.clusterName.hashCode();
        return hash;
    }
}
