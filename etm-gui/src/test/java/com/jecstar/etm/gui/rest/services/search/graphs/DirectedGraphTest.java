package com.jecstar.etm.gui.rest.services.search.graphs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the <code>DirectedGraph</code> class.
 */
public class DirectedGraphTest {

    /**
     * Test the DAG ordering.
     */
    @Test
    public void testGetDirectedAcyclicOrder() {
        var directedGraph = new DirectedGraph();
        var frontendApp = new Application("app1", "Frontend app");
        var backendApp = new Application("app2", "Backend app");
        directedGraph.addEdge(new Event("6", "getStockRequest", frontendApp), new Endpoint("7", "https://www.backend.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP));
        directedGraph.addEdge(new Event("2", "getCustomerRequest", frontendApp), new Endpoint("3", "REQ.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ));
        directedGraph.addEdge(new Event("5", "getCustomerResponse", frontendApp), new Event("6", "getStock", frontendApp));
        directedGraph.addEdge(new Endpoint("4", "RSP.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ), new Event("5", "getCustomerResponse", frontendApp));
        directedGraph.addEdge(new Event("12", "responseMessage", frontendApp), new Endpoint("13", "https://www.jecstar.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP));
        directedGraph.addEdge(new Event("9", "getStockResponse", backendApp), new Endpoint("10", "https://www.backend.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP));
        directedGraph.addEdge(new Endpoint("0", "https://www.jecstar.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP), new Event("1", "getMessage", frontendApp));
        directedGraph.addEdge(new Endpoint("7", "https://www.backend.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP), new Event("8", "getStockRequest", backendApp));
        directedGraph.addEdge(new Event("1", "getMessage", frontendApp), new Event("2", "getCustomerRequest", frontendApp));
        directedGraph.addEdge(new Event("8", "getStockRequest", backendApp), new Event("9", "getStockResponse", backendApp));
        directedGraph.addEdge(new Endpoint("3", "REQ.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ), new Endpoint("3B", "RSP.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ));
        directedGraph.addEdge(new Endpoint("3B", "REQ.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ), new Endpoint("4B", "RSP.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ));
        directedGraph.addEdge(new Endpoint("4B", "REQ.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ), new Endpoint("4", "RSP.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ));
        directedGraph.addEdge(new Endpoint("10", "https://www.backend.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP), new Event("11", "getStockResponse", frontendApp));
        directedGraph.addEdge(new Event("11", "getStockResponse", frontendApp), new Event("12", "responseMessage", frontendApp));
        var order = directedGraph.getDirectedAcyclicOrder();
        for (var vertex : order) {
            int vertexIx = order.indexOf(vertex);
            var adjacentVertices = directedGraph.getAdjacentOutVertices(vertex);
            for (var adjacentVertex : adjacentVertices) {
                assertTrue(vertexIx < order.indexOf(adjacentVertex));
            }
        }
        var layers = directedGraph.getLayers();
        // We expect 4 layers.
        //0 -> 0, 13
        //1 -> app1
        //2 -> 3, 4, 7, 10
        //3 -> 3B, 4B, app2
        assertEquals(4, layers.size());

        // At layer 0 we expect to see 0 and 13
        var vertices = layers.get(0).getVertices();
        assertEquals(2, vertices.size());
        assertEquals("0", vertices.get(0).getVertexId());
        assertEquals("13", vertices.get(1).getVertexId());

        // At layer 1 we expect to see app1
        vertices = layers.get(1).getVertices();
        assertEquals(1, vertices.size());
        assertEquals("app1", vertices.get(0).getVertexId());

        // At layer 2 we expect to see 3, 4, 7 and 10
        vertices = layers.get(2).getVertices();
        assertEquals(4, vertices.size());
        assertEquals("3", vertices.get(0).getVertexId());
        assertEquals("4", vertices.get(1).getVertexId());
        assertEquals("7", vertices.get(2).getVertexId());
        assertEquals("10", vertices.get(3).getVertexId());

        // At layer 3 we expect to see 3B, 4B and app2
        vertices = layers.get(3).getVertices();
        assertEquals(3, vertices.size());
        assertEquals("3B", vertices.get(0).getVertexId());
        assertEquals("4B", vertices.get(1).getVertexId());
        assertEquals("app2", vertices.get(2).getVertexId());
    }
}
