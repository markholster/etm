package com.jecstar.etm.server.core;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.InetAddress;

/**
 * Super class for all integration tests. This class requires a running
 * Elasticsearch instance on localhost:9300. ETM templates and scripts should be
 * configured on that instance as well.
 *
 * @author Mark Holster
 */
public abstract class AbstractIntegrationTest {

    protected final EtmConfiguration etmConfiguration = new EtmConfiguration("integration-test");
    private Client client;
    protected BulkProcessor bulkProcessor;
    private final String elasticCredentials = "elastic:5+2vTgPUNPxJas*LBm9~";

    protected abstract BulkProcessor.Listener createBuilkListener();

    @BeforeEach
    public void setup() {
        this.etmConfiguration.setEventBufferSize(1);
        Settings.Builder builder = Settings.builder()
                .put("cluster.name", "Enterprise Telemetry Monitor")
                .put("client.transport.sniff", false);
        TransportClient transportClient = null;
        if (this.elasticCredentials != null) {
            builder.put("xpack.security.user", elasticCredentials);
            transportClient = new PreBuiltXPackTransportClient(builder.build());
        } else {
            transportClient = new PreBuiltTransportClient(builder.build());
        }
        transportClient.addTransportAddress(new TransportAddress(InetAddress.getLoopbackAddress(), 9300));
        this.client = transportClient;
        this.bulkProcessor = BulkProcessor.builder(this.client, createBuilkListener())
                .setBulkActions(1)
                .build();
    }

    @AfterEach
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
                if (version == null || getResponse.getVersion() == version) {
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
