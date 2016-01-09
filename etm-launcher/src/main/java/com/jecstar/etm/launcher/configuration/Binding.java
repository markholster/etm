package com.jecstar.etm.launcher.configuration;

import java.io.File;

public class Binding {

	public String bindingAddress = "0.0.0.0";
	public int bindingPortOffset = 0;
	public int httpPort = 8080;
	
	public int httpsPort = 8443;
	public String sslProtocol = "TLS";
	public File sslKeystoreLocation;
	public String sslKeystoreType = "PKCS12";
	public String sslKeystorePassword;
	public File sslTruststoreLocation;
	public String sslTruststoreType = "JKS";
	public String sslTruststorePassword;

	
	public int getHttpPort() {
		if (this.httpPort > 0) {
			return this.httpPort + this.bindingPortOffset; 
		}
		return this.httpPort;
	}
	
	public int getHttpsPort() {
		if (this.httpsPort > 0) {
			return this.httpsPort + this.bindingPortOffset; 
		}
		return this.httpsPort;
	}
}
