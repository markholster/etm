package com.jecstar.etm.launcher.configuration;

import java.io.File;

public class Http {

	public boolean guiEnabled = true;
	public boolean restProcessorEnabled = true;
	public boolean restProcessorLoginRequired = false;
	
	public int httpPort = 8080;
	public int httpsPort = 8443;
	
	public int maxConcurrentRequests = 100;
	public int maxQueuedRequests = 100;
	
	public String sslProtocol = "TLSv1.2";
	public File sslKeystoreLocation;
	public String sslKeystoreType = "PKCS12";
	public String sslKeystorePassword;
	public File sslTruststoreLocation;
	public String sslTruststoreType = "JKS";
	public String sslTruststorePassword;
}
