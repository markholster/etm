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

public class Http {

    public int httpPort = 8080;
    public int httpsPort = 8443;
    public boolean secureCookies = false;

    public int ioThreads = 2;
    public int workerThreads = 16;
    public String contextRoot = "/";

    public boolean guiEnabled = true;
    public int guiMaxConcurrentRequests = 50;
    public int guiMaxQueuedRequests = 50;

    public boolean restProcessorEnabled = true;
    public int restProcessorMaxConcurrentRequests = 50;
    public int restProcessorMaxQueuedRequests = 50;

    public String sslProtocol = "TLSv1.2";
    public File sslKeystoreLocation;
    public String sslKeystoreType = "PKCS12";
    public String sslKeystorePassword;
    public File sslTruststoreLocation;
    public String sslTruststoreType = "JKS";
    public String sslTruststorePassword;

    public String getContextRoot() {
        if (this.contextRoot == null) {
            this.contextRoot = "/";
        }
        if (!this.contextRoot.endsWith("/")) {
            this.contextRoot += "/";
        }
        return contextRoot;
    }

    public int calculateInstanceHash() {
        int hash = 0;
        hash += this.httpPort;
        hash += this.httpsPort;
        return hash;
    }
}
