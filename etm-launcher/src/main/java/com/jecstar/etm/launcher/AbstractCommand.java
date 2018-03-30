package com.jecstar.etm.launcher;

import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.MasterNotDiscoveredException;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public abstract class AbstractCommand {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private final LogWrapper log = LogFactory.getLogger(getClass());

    protected Client createElasticsearchClient(Configuration configuration) {
        Settings.Builder settingsBuilder = Settings.builder()
                .put("cluster.name", configuration.elasticsearch.clusterName)
                .put("client.transport.sniff", true);
        TransportClient transportClient;
        if (configuration.elasticsearch.username != null && configuration.elasticsearch.password != null) {
            settingsBuilder.put("xpack.security.user", configuration.elasticsearch.username + ":" + configuration.elasticsearch.password);
            if (configuration.elasticsearch.sslKeyLocation != null) {
                settingsBuilder.put("xpack.ssl.key", configuration.elasticsearch.sslKeyLocation.getAbsolutePath());
            }
            if (configuration.elasticsearch.sslCertificateLocation != null) {
                settingsBuilder.put("xpack.ssl.certificate", configuration.elasticsearch.sslCertificateLocation.getAbsolutePath());
            }
            if (configuration.elasticsearch.sslCertificateAuthoritiesLocation != null) {
                settingsBuilder.put("xpack.ssl.certificate_authorities", configuration.elasticsearch.sslCertificateAuthoritiesLocation.getAbsolutePath());
            }
            if (configuration.elasticsearch.sslEnabled) {
                settingsBuilder.put("xpack.security.transport.ssl.enabled", "true");
            }
            transportClient = new PreBuiltXPackTransportClient(settingsBuilder.build());
        } else {
            transportClient = new PreBuiltTransportClient(settingsBuilder.build());
        }
        int hostsAdded = addElasticsearchHostsToTransportClient(configuration.elasticsearch.connectAddresses, transportClient);
        if (configuration.elasticsearch.waitForConnectionOnStartup) {
            while (hostsAdded == 0) {
                // Currently this can only happen in docker swarm installations where the elasticsearch service isn't fully started when ETM starts. This will result in a
                // UnknownHostException so that leaves with a transportclient without any hosts. Also this may happen when the end users misspells the hostname in the configuration.
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                hostsAdded = addElasticsearchHostsToTransportClient(configuration.elasticsearch.connectAddresses, transportClient);
            }
            waitForActiveConnection(transportClient);
        }
        return transportClient;
    }

    private int addElasticsearchHostsToTransportClient(List<String> hosts, TransportClient transportClient) {
        int added = 0;
        for (String host : hosts) {
            TransportAddress transportAddress = createTransportAddress(host);
            if (transportAddress != null) {
                transportClient.addTransportAddress(transportAddress);
                added++;
            }
        }
        return added;
    }

    private TransportAddress createTransportAddress(String host) {
        int ix = host.lastIndexOf(":");
        if (ix != -1) {
            try {
                InetAddress inetAddress = InetAddress.getByName(host.substring(0, ix));
                int port = Integer.parseInt(host.substring(ix + 1));
                return new TransportAddress(inetAddress, port);
            } catch (UnknownHostException e) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("Unable to connect to '" + host + "'", e);
                }
            }
        }
        return null;
    }

    private void waitForActiveConnection(TransportClient transportClient) {
        while (transportClient.connectedNodes().isEmpty()) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            // Wait for any elasticsearch node to become active.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        boolean esClusterInitialized = false;
        while (!esClusterInitialized) {
            try {
                ClusterHealthResponse clusterHealthResponse = transportClient.admin().cluster().prepareHealth().get();
                if (clusterHealthResponse.getInitializingShards() == 0
                        && clusterHealthResponse.getNumberOfPendingTasks() == 0
                        && clusterHealthResponse.getNumberOfDataNodes() > 0) {
                    esClusterInitialized = true;
                }
            } catch (MasterNotDiscoveredException | ClusterBlockException e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Waiting for cluster to te available", e);
                }
            }
            if (!esClusterInitialized) {
                // Wait for all shards to be initialized and no more tasks pending and at least 1 data node to be available.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
        }
    }

}
