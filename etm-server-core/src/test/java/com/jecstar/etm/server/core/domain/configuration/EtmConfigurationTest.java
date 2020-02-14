package com.jecstar.etm.server.core.domain.configuration;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the <code>EtmConfiguration</code> class.
 */
public class EtmConfigurationTest {

    @Test
    public void testMergeRemoteIndices() {
        EtmConfiguration etmConfiguration = new EtmConfiguration(getClass().getName());
        etmConfiguration.addRemoteCluster(new RemoteCluster().setName("cluster1"));
        etmConfiguration.addRemoteCluster(new RemoteCluster().setName("cluster2"));
        etmConfiguration.addRemoteCluster(new RemoteCluster().setName("cluster3"));

        String[] indices = etmConfiguration.mergeRemoteIndices("index1", "index2", "index3");
        assertEquals(12, indices.length);
        assertTrue(Arrays.asList(indices).contains("index1"));
        assertTrue(Arrays.asList(indices).contains("index2"));
        assertTrue(Arrays.asList(indices).contains("index3"));
        assertTrue(Arrays.asList(indices).contains("cluster1:index1"));
        assertTrue(Arrays.asList(indices).contains("cluster1:index2"));
        assertTrue(Arrays.asList(indices).contains("cluster1:index3"));
        assertTrue(Arrays.asList(indices).contains("cluster2:index1"));
        assertTrue(Arrays.asList(indices).contains("cluster2:index2"));
        assertTrue(Arrays.asList(indices).contains("cluster2:index3"));
        assertTrue(Arrays.asList(indices).contains("cluster3:index1"));
        assertTrue(Arrays.asList(indices).contains("cluster3:index2"));
        assertTrue(Arrays.asList(indices).contains("cluster3:index3"));

        etmConfiguration = new EtmConfiguration(getClass().getName());
        indices = etmConfiguration.mergeRemoteIndices("index1", "index2", "index3");
        assertEquals(3, indices.length);
        assertTrue(Arrays.asList(indices).contains("index1"));
        assertTrue(Arrays.asList(indices).contains("index2"));
        assertTrue(Arrays.asList(indices).contains("index3"));
    }
}
