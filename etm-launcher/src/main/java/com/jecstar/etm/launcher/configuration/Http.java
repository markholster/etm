package com.jecstar.etm.launcher.configuration;

import java.io.File;

public class Http {

	public int httpPort = 8080;
	public int httpsPort = 8443;
	
	public String sslProtocol = "TLS";
	public File sslKeystoreLocation;
	public String sslKeystoreType = "PKCS12";
	public String sslKeystorePassword;
	public File sslTruststoreLocation;
	public String sslTruststoreType = "JKS";
	public String sslTruststorePassword;
}
