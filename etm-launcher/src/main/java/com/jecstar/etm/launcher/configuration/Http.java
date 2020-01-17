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
