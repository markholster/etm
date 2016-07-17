package com.jecstar.etm.launcher.retention;

import org.elasticsearch.client.Client;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public class IndexCleaner implements Runnable {

	private final EtmConfiguration etmConfiguration;
	private final Client client;

	public IndexCleaner(EtmConfiguration etmConfiguration, Client client) {
		this.etmConfiguration = etmConfiguration;
		this.client = client;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
	}

}
