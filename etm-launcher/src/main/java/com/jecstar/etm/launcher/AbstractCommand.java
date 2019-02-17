package com.jecstar.etm.launcher;

import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;


public abstract class AbstractCommand {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private final LogWrapper log = LogFactory.getLogger(getClass());

    protected DataRepository createElasticsearchClient(Configuration configuration) {
        HttpHost[] hosts = configuration.elasticsearch.connectAddresses.stream().map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder restClientBuilder = RestClient.builder(hosts);

        restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
            if (configuration.elasticsearch.username != null && configuration.elasticsearch.password != null) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(configuration.elasticsearch.username, configuration.elasticsearch.password));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            if (configuration.elasticsearch.sslTrustStoreLocation != null) {
                try {
                    KeyStore truststore = KeyStore.getInstance("jks");
                    try (InputStream is = Files.newInputStream(configuration.elasticsearch.sslTrustStoreLocation.toPath())) {
                        truststore.load(is, configuration.elasticsearch.sslTrustStorePassword == null ? null : configuration.elasticsearch.sslTrustStorePassword.toCharArray());
                    }
                    SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(truststore, null);
                    httpClientBuilder.setSSLContext(sslBuilder.build());
                } catch (Exception e) {
                    e.printStackTrace();
                    if (log.isErrorLevelEnabled()) {
                        log.logErrorMessage(e.getMessage(), e);
                    }
                }
            }
            return httpClientBuilder;
        });

        RestHighLevelClient client = new RestHighLevelClient(restClientBuilder);


        if (configuration.elasticsearch.waitForConnectionOnStartup) {
            waitForActiveConnection(client);
        }
        return new DataRepository(client);
    }

    private void waitForActiveConnection(RestHighLevelClient client) {
        boolean esClusterInitialized = false;
        while (!esClusterInitialized) {
            try {
                ClusterHealthResponse healthResponse = client.cluster().health(
                        new ClusterHealthRequest()
                                .waitForNoInitializingShards(true)
                                .waitForYellowStatus(),
                        RequestOptions.DEFAULT
                );
                if (healthResponse.getInitializingShards() == 0
                        && healthResponse.getNumberOfPendingTasks() == 0
                        && healthResponse.getNumberOfDataNodes() > 0) {
                    esClusterInitialized = true;
                }
            } catch (IOException e) {
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
