package com.jecstar.etm.launcher.configuration;

import java.io.File;

public class Http {

    public int httpPort = 8080;
    public int httpsPort = 8443;

    public int ioThreads = 2;
    public int workerThreads = 16;

    public boolean guiEnabled = true;
    public int guiMaxConcurrentRequests = 50;
    public int guiMaxQueuedRequests = 50;

    public boolean restProcessorEnabled = true;
    public boolean restProcessorLoginRequired = false;
    public int restProcessorMaxConcurrentRequests = 50;
    public int restProcessorMaxQueuedRequests = 50;

    public String sslProtocol = "TLSv1.2";
    public File sslKeystoreLocation;
    public String sslKeystoreType = "PKCS12";
    public String sslKeystorePassword;
    public File sslTruststoreLocation;
    public String sslTruststoreType = "JKS";
    public String sslTruststorePassword;
}
