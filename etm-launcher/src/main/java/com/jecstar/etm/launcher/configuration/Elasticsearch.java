package com.jecstar.etm.launcher.configuration;

import java.io.File;

public class Elasticsearch {

	public String clusterName = "elasticsearch";
	
	// Comma separated list of hosts to connect to.
	public String connectAddresses = "127.0.0.1:9300";

	public boolean waitForConnectionOnStartup = false;
	
	public String username;
	public String password;
	
	public boolean sslEnabled = false;
	public File sslKeyLocation;
	public File sslCertificateLocation;
	public File sslCertificateAuthoritiesLocation;
	
}
