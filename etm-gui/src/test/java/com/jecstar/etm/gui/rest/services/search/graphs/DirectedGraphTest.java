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

        var event1 = new Event("1", "getMessage", frontendApp);
        var event2 = new Event("2", "getCustomerRequest", frontendApp);
        var event5 = new Event("5", "getCustomerResponse", frontendApp);
        var event6 = new Event("6", "getStockRequest", frontendApp);
        var event8 = new Event("8", "getStockRequest", backendApp);
        var event9 = new Event("9", "getStockResponse", backendApp);
        var event11 = new Event("11", "getStockResponse", frontendApp);
        var event12 = new Event("12", "responseMessage", frontendApp);

        directedGraph.addEdge(event6, new Endpoint("7", "https://www.backend.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP));
        directedGraph.addEdge(event2, new Endpoint("3", "REQ.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ));
        directedGraph.addEdge(event5, event6);
        directedGraph.addEdge(new Endpoint("4", "RSP.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ), event5);
        directedGraph.addEdge(event12, new Endpoint("13", "https://www.jecstar.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP));
        directedGraph.addEdge(event9, new Endpoint("10", "https://www.backend.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP));
        directedGraph.addEdge(new Endpoint("0", "https://www.jecstar.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP), event1);
        directedGraph.addEdge(new Endpoint("7", "https://www.backend.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP), event8);
        directedGraph.addEdge(event1, event2);
        directedGraph.addEdge(event8, event9);
        directedGraph.addEdge(new Endpoint("3", "REQ.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ), new Endpoint("3B", "REQ.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ));
        directedGraph.addEdge(new Endpoint("3B", "REQ.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ), new Endpoint("4B", "RSP.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ));
        directedGraph.addEdge(new Endpoint("4B", "RSP.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ), new Endpoint("4", "RSP.CUSTOMER", com.jecstar.etm.domain.Endpoint.ProtocolType.MQ));
        directedGraph.addEdge(new Endpoint("10", "https://www.backend.com", com.jecstar.etm.domain.Endpoint.ProtocolType.HTTP), event11);
        directedGraph.addEdge(event11, event12);
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
