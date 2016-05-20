package com.jecstar.etm.processor;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.After;
import org.junit.Before;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;

public abstract class AbstractIntegrationTest {

	protected final EtmConfiguration etmConfiguration = new EtmConfiguration("integration-test");
	protected Client client; 
	protected BulkProcessor bulkProcessor;
	
	protected abstract BulkProcessor.Listener createBuilkListener();
	
	@Before
	public void setup() throws UnknownHostException {
		this.etmConfiguration.setEventBufferSize(1);
		TransportClient transportClient = TransportClient.builder().settings(Settings.builder()
				.put("cluster.name", "Enterprise Telemetry Monitor")
				.put("client.transport.sniff", false)).build();
		transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getLocalHost(), 9300));
		this.client = transportClient;
		this.bulkProcessor = BulkProcessor.builder(this.client,  createBuilkListener())
			.setBulkActions(1)
			.build();
	}
	
	@After
	public void tearDown() {
		if (this.bulkProcessor != null) {
			this.bulkProcessor.close();
		}
		
	}
}
