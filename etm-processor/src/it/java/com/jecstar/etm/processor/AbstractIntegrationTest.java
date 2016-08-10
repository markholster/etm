package com.jecstar.etm.processor;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.Before;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;

/**
 * Super class for all integration tests. This class requires a running
 * Elasticsearch instance on localhost:9300. ETM templates and scripts should be
 * configured on that instance as well.
 * 
 * @author Mark Holster
 */
public abstract class AbstractIntegrationTest {

	protected final EtmConfiguration etmConfiguration = new EtmConfiguration("integration-test");
	protected Client client; 
	protected BulkProcessor bulkProcessor;
	
	protected abstract BulkProcessor.Listener createBuilkListener();
	
	@Before
	public void setup() throws UnknownHostException {
		this.etmConfiguration.setEventBufferSize(1);
		TransportClient transportClient = new PreBuiltTransportClient(Settings.builder()
				.put("cluster.name", "Enterprise Telemetry Monitor")
				.put("client.transport.sniff", false).build());
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
	
	protected GetResponse waitFor(String index, String type, String id) throws InterruptedException {
		return waitFor(index, type, id, null);
	}
	
	protected GetResponse waitFor(String index, String type, String id, Long version) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		do {
			GetResponse getResponse = this.client.prepareGet(index, type, id).get();
			if (getResponse.isExists()) {
				if (version == null || getResponse.getVersion() == version.longValue()) {
					return getResponse;
				}
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedException();
			}
		} while (System.currentTimeMillis() - startTime < 10_000);
		throw new NoSuchEventException(index, type, id, version);
	}
}
