/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest.services.search.graphs;

import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class representing a Directed Graph.
 */
public class DirectedGraph {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(DirectedGraph.class);

    /**
     * The outdegree map of all <code>Vertex</code> instances present in this <code>DirectedGraph</code>.
     */
    private final Map<Vertex, LinkedList<Vertex>> outdegreeMap = new LinkedHashMap<>();
    /**
     * The indegree map that holds the connections to a certain <code>Vertex</code>.
     */
    private final Map<Vertex, LinkedList<Vertex>> indegreeMap = new LinkedHashMap<>();

    /**
     * The parent map that holds all <code>Vertex</code> instances per parent.
     */
    private final Map<Vertex, Set<Vertex>> parentMap = new HashMap<>();

    /**
     * The number of edges;
     */
    private int edgesCount;

    /**
     * The total duration of handling the first request in this DAG.
     */
    private Duration totalEventTime;

    /**
     * Add an <code>Vertex</code> to the <code>DirectedGraph</code>. If the <code>Vertex</code> already is present in the <code>DirectedGraph</code> this method will be ignored.
     *
     * @param vertex The <code>Vertex</code> to add.
     * @return The <code> DirectedGraph</code> used for chaining.
     */
    private DirectedGraph addVertex(Vertex vertex) {
        if (this.outdegreeMap.containsKey(vertex)) {
            return this;
        }
        this.outdegreeMap.put(vertex, new LinkedList<>());
        this.indegreeMap.put(vertex, new LinkedList<>());
        if (vertex.getParent() != null) {
            this.parentMap.computeIfAbsent(vertex.getParent(), k -> new HashSet<>()).add(vertex);
        }
        return this;
    }

    /**
     * Adds the directed edge tale → head to this directed graph.
     *
     * @param tail The tail <code>Vertex</code>.
     * @param head The head <code>Vertex</code>.
     * @return The <code> DirectedGraph</code> used for chaining.
     */
    public DirectedGraph addEdge(Vertex tail, Vertex head) {
        addVertex(tail).addVertex(head);
        if (tail instanceof Event && head instanceof Endpoint) {
            var eventStartTime = ((Event) tail).getEventStartTime();
            ((Endpoint) head).setWriteTime(eventStartTime);
        } else if (tail instanceof Endpoint && head instanceof Event) {
            var eventStartTime = ((Event) head).getEventStartTime();
            ((Endpoint) tail).addReadTime(eventStartTime);
        }
        this.outdegreeMap.get(tail).addFirst(head);
        this.indegreeMap.get(head).add(tail);
        this.edgesCount++;
        return this;
    }

    /**
     * Returns the <coder>Vertex</coder> instances of this <code>DirectedGraph</code>.
     *
     * @return The <coder>Vertex</coder> instances.
     */
    public Set<Vertex> getVertices() {
        return this.outdegreeMap.keySet();
    }

    /**
     * Returns the child <code>Vertex</code> instances of the given <code>Vertex</code>.
     *
     * @param vertex The parent <code>Vertex</code>
     * @return The child <code>Vertext</code> instances, or <code>null</code> when the give <code>Vertex</code> isn't a parent <code>Vertex</code>.
     */
    public Set<Vertex> getChildVertices(Vertex vertex) {
        return this.parentMap.get(vertex);
    }

    /**
     * Returns the number of directed edges incident from the given <code>Vertex</code>.
     * This is known as the <em>outdegree</em> of a vertex.
     *
     * @param vertex The <code>vertex</code>.
     * @return The outdegree of the given <code>Vertex</code>
     * @throws IllegalArgumentException when the given <code>Vertex</code> is not part of this <code>DirectedGraph</code>
     */
    public int outdegree(Vertex vertex) {
        var adjacencyList = this.outdegreeMap.get(vertex);
        if (adjacencyList == null) {
            throw new IllegalArgumentException("Vertex with id '" + vertex.getVertexId() + "' not part of this DirectedGraph");
        }
        return adjacencyList.size();
    }

    /**
     * Returns the number of directed edges incident to the given <code>Vertex</code>.
     * This is known as the <em>indegree</em> of a vertex.
     *
     * @param vertex The <code>vertex</code>.
     * @return The indegree of the given <code>Vertex</code>
     * @throws IllegalArgumentException when the given <code>Vertex</code> is not part of this <code>DirectedGraph</code>
     */
    public int indegree(Vertex vertex) {
        var indegreeList = this.indegreeMap.get(vertex);
        if (indegreeList == null) {
            throw new IllegalArgumentException("Vertex with id '" + vertex.getVertexId() + "' not part of this DirectedGraph");
        }
        return indegreeList.size();
    }

    /**
     * Returns the adjacent vertices that are outgoing seen from a given <code>Vertex</code>.
     *
     * @param vertex The <code>Vertex</code> to return the adjacent vertices from.
     * @return A <code>List</code> with adjacent <code>Vertex</code> instances, or <code>null</code> if the given <code>Vertex</code> is not part of this <code>DirectedGraph</code>.
     */
    public List<Vertex> getAdjacentOutVertices(Vertex vertex) {
        return this.outdegreeMap.get(vertex);
    }

    /**
     * Returns the adjacent vertices that are incomming seen from a given <code>Vertex</code>.
     *
     * @param vertex The <code>Vertex</code> to return the adjacent vertices from.
     * @return A <code>List</code> with adjacent <code>Vertex</code> instances, or <code>null</code> if the given <code>Vertex</code> is not part of this <code>DirectedGraph</code>.
     */
    public List<Vertex> getAdjacentInVertices(Vertex vertex) {
        return this.indegreeMap.get(vertex);
    }

    /**
     * Returns a <code>List</code> of <code>Vertex</code> instances in such and order that all its edges point from a vertex earlier in the order to a vertex later in the order.
     *
     * @return The code>List</code> of ordered <code>Vertex</code> instances.
     */
    public List<Vertex> getDirectedAcyclicOrder() {
        var cycleFinder = new CycleFinder(this);
        if (cycleFinder.hasCycle()) {
            // TODO dit moet nog opgelost worden. Op zoek naar de kortste cycle en deze negeren o.i.d.
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Found a cycle in the event order: \n" + toString());
            }
            throw new IllegalStateException("Found a cycle in the event order. It is impossible to display the endpoints overview.");
        }
        var depthFirstOrder = new DepthFirstOrder(this);
        return depthFirstOrder.reversePostOrder();
    }

    /**
     * Determines if the given <em>source</em> <code>Vertex</code> has a path to the <em>target</em> <code>Vertex</code>.
     *
     * @param source The source <code>Vertex</code>.
     * @param target The target <code>Vertex</code>.
     * @return <code>true</code> if there is a path from the <em>source</em> <code>Vertex</code> to the <em>target</em> <code>Vertex</code>, <code>false</code> otherwise.
     */
    public boolean hasPathTo(Vertex source, Vertex target) {
        return new BreadthFirstDirectedPath(this, source).hasPathTo(target);
    }

    /**
     * Determines if an <code>Event</code> with the given predicate is present.
     *
     * @param predicate The predicate to test for.
     * @return <code>true</code> if an <code>Event</code> with the given predicate is present in the graph, <code>false</code> otherwise.
     */
    public boolean containsEvent(Predicate<? super Event> predicate) {
        return getVertices().stream().filter(p -> Event.class.equals(p.getClass())).map(v -> (Event) v).anyMatch(predicate);
    }

    /**
     * Find <code>Event</code> instances that match the given predicate.
     *
     * @param predicate The predicate to test for.
     * @return A <code>List</code> with <code>Event</code> instances.
     */
    public List<Event> findEvents(Predicate<? super Event> predicate) {
        return getVertices().stream().filter(p -> Event.class.equals(p.getClass())).map(v -> (Event) v).filter(predicate).collect(Collectors.toList());
    }

    /**
     * Gives the <code>Layer</code>s in such an order that all layers can be draw in a graph from top to order.
     *
     * @return The <code>Layer</code> instances of this <code>DirectedGraph</code>.
     */
    public List<Layer> getLayers() {
        var layers = new ArrayList<Layer>();
        var layer = new Layer(0);
        layers.add(layer);
        layer.addAndOptimize(layers, getDirectedAcyclicOrder());
        return layers;
    }

    /**
     * Gives the total handling time of the first request in this DAG. The method @calculateAbsoluteMetrics should be called before calling this method.
     *
     * @return The total handling time of all events in this DAG.
     */
    public Duration getTotalEventTime() {
        return this.totalEventTime;
    }

    /**
     * Calculates the absolute event time for all events present in this DAG.
     *
     * @return This <code>DirectedGraph</code> for chaining.
     */
    public DirectedGraph calculateAbsoluteMetrics() {
        List<Vertex> orderedVertices = getDirectedAcyclicOrder();
        if (orderedVertices.size() == 0) {
            return this;
        }
        Instant firstTime = null;
        Instant lastTime = null;
        for (int i = 0; i < orderedVertices.size() && firstTime == null; i++) {
            var vertex = orderedVertices.get(i);
            if (vertex instanceof Event) {
                firstTime = ((Event) vertex).getEventStartTime();
            } else if (vertex instanceof Endpoint) {
                var endpoint = (Endpoint) vertex;
                if (endpoint.getWriteTime() != null) {
                    firstTime = endpoint.getWriteTime();
                } else if (endpoint.getFirstReadTime() != null) {
                    firstTime = endpoint.getFirstReadTime();
                }
            }
        }
        for (int i = orderedVertices.size() - 1; i >= 0 && lastTime == null; i--) {
            var vertex = orderedVertices.get(i);
            if (vertex instanceof Event) {
                lastTime = ((Event) vertex).getEventStartTime();
            } else if (vertex instanceof Endpoint) {
                var endpoint = (Endpoint) vertex;
                if (endpoint.getFirstReadTime() != null) {
                    lastTime = endpoint.getFirstReadTime();
                } else if (endpoint.getWriteTime() != null) {
                    lastTime = endpoint.getWriteTime();
                }
            }
        }
        List<Event> events = findEvents(x -> !x.isResponse());
        if (events.size() == 0) {
            return this;
        }
        if (firstTime == null || lastTime == null) {
            // Not possible to calculate times. Reset everything and return.
            events.forEach(Event::resetAbsoluteTransactionMetrics);
            return this;
        }
        this.totalEventTime = Duration.between(firstTime, lastTime);

        events.sort(Comparator.comparing(Event::getEventStartTime).thenComparing(Event::getOrder).thenComparing(Event::getEventEndTime, Comparator.reverseOrder()));
        if (this.totalEventTime == null) {
            return this;
        }
        for (var i = 0; i < events.size(); i++) {
            var event = events.get(i);
            if (event.isResponse()) {
                continue;
            }
            var eventTime = event.getTotalEventTime();
            if (eventTime == null) {
                continue;
            }
            if (event.isSent()) {
                // A writer -> the percentage is calculated over the max latency of all readers of the same event.
                if (event.isAsync()) {
//                    event.setAbsoluteTransactionPercentage(0);
                } else {
                    var lowestStartTime = event.getEventStartTime().toEpochMilli();
                    var highestEndTime = event.getEventEndTime().toEpochMilli();
                    var minEvent = events.stream()
                            .skip(i)
                            .filter(p -> p.isReceived() && Objects.equals(p.getEventId(), event.getEventId()) && p.getEventStartTime() != null && !p.getEventStartTime().isBefore(event.getEventStartTime()))
                            .min(Comparator.comparing(e -> e.getEventStartTime().toEpochMilli()));
                    if (minEvent.isPresent()) {
                        lowestStartTime = minEvent.get().getEventStartTime().toEpochMilli();
                    }

                    var maxEvent = events.stream()
                            .skip(i)
                            .filter(p -> p.isReceived() && Objects.equals(p.getEventId(), event.getEventId()) && p.getEventEndTime() != null && !p.getEventEndTime().isAfter(event.getEventEndTime()))
                            .max(Comparator.comparing(e -> e.getEventEndTime().toEpochMilli()));
                    if (maxEvent.isPresent()) {
                        highestEndTime = maxEvent.get().getEventEndTime().toEpochMilli();
                    }
                    final var latency = eventTime.minus((highestEndTime - lowestStartTime), ChronoUnit.MILLIS);
                    event.calculateAbsoluteTransactionMetrics(latency, this.totalEventTime);
                }
            } else {
                // A reader -> search for event from the same application that are written after the current event.
                var backendTime = events.stream()
                        .skip(i)
                        .filter(p -> p.isSent()
                                && Objects.equals(p.getTransactionId(), event.getTransactionId())
                                && Objects.equals(p.getParent(), event.getParent())
                                && p.getTotalEventTime() != null
                                && !p.isAsync()
                                && !p.isResponse())
                        .mapToLong(e -> e.getTotalEventTime().toMillis())
                        .sum();
                var absoluteTime = eventTime.minusMillis(backendTime);
                event.calculateAbsoluteTransactionMetrics(absoluteTime, this.totalEventTime);
            }
        }
        return this;
    }

    /**
     * Method that finished the graph by connecting the unconnected <code>Vertex</code> instances where possible.
     * <p>
     * This method has nothing to do with the mathematical principal behind a DAG. It has ETM specific knowledge.
     *
     * @return This <code>DirectedGraph</code> for chaining.
     */
    public DirectedGraph finishGraph() {
        // Add edges within a transaction.
        var vertices = getVertices();
        for (var vertex : vertices) {
            if (!(vertex instanceof Event)) {
                continue;
            }
            if (!getAdjacentOutVertices(vertex).isEmpty()) {
                continue;
            }
            // We've found an event without a connection to a next vertex. Let's see if we can make some connections.
            var event = (Event) vertex;
            if (event.getTransactionId() != null) {
                var transactionEvents = findEvents(e -> event.getTransactionId().equals(e.getTransactionId()));
                transactionEvents.sort(Comparator.comparing(Event::getEventStartTime));
                for (var transactionEvent : transactionEvents) {
                    if (!transactionEvent.getEventStartTime().isBefore(event.getEventStartTime()) && !event.getEventId().equals(transactionEvent.getEventId())) {
                        addEdge(event, transactionEvent);
                        break;
                    }
                }
            }
        }
        // See if all requests are connected to responses. If not, try to add the connection.
        for (var vertex : vertices) {
            if (!(vertex instanceof Event)) {
                continue;
            }
            var responseEvent = (Event) vertex;
            // Check if this responseEvent is a response and has a correlation to a request.
            if (!responseEvent.isResponse() || responseEvent.getCorrelationEventId() == null) {
                continue;
            }
            // Find the corresponding request of the response.
            Optional<Event> first = vertices.stream().filter(v -> v instanceof Event).map(e -> (Event) e).filter(e -> Objects.equals(e.getEventId(), responseEvent.getCorrelationEventId())).findFirst();
            if (first.isEmpty()) {
                continue;
            }
            var requestEvent = first.get();
            if (hasPathTo(requestEvent, responseEvent)) {
                // There's a path from the request to the response. Nothing to do...
                continue;
            }
            createPath(requestEvent, responseEvent);
        }
        return this;
    }

    /**
     * Method that tries to create a path from a request <ccode>Event</ccode> to a corresponding response <code>Event</code>.
     *
     * @param requestEvent  The request <code>Event</code> that needs to have a path to the corresponding response <code>Event</code>.
     * @param responseEvent The response <code>Event</code> that needs to have a path to the corresponding request <code>Event</code>.
     */
    private void createPath(Event requestEvent, Event responseEvent) {
        if (!Objects.equals(requestEvent.getEventId(), responseEvent.getCorrelationEventId())) {
            // Objects not related.
            return;
        }
        if (hasPathTo(requestEvent, responseEvent)) {
            // Path already present.
            return;
        }
        // Let's see if we can find the end of the path seen from the request.
        Vertex lastAfterRequest = requestEvent;
        while (getAdjacentOutVertices(lastAfterRequest) != null && !getAdjacentOutVertices(lastAfterRequest).isEmpty()) {
            var next = getAdjacentOutVertices(lastAfterRequest).get(0);
            if (next.equals(requestEvent)) {
                // Somehow a cycle has introduced into the DAG. This should not be possible.
                break;
            }
            if (next instanceof Event) {
                var nextEvent = (Event) next;
                if (!nextEvent.isResponse()) {
                    // We've found another request. Let's make sure there is a path from this request to the response otherwise the wrong dots will be connected.
                    Optional<Event> optionalResponse = getVertices().stream()
                            .filter(v -> v instanceof Event)
                            .map(e -> (Event) e)
                            .filter(e -> Objects.equals(nextEvent.getEventId(), e.getCorrelationEventId()) && e.isResponse())
                            .findFirst();
                    if (!optionalResponse.isEmpty()) {
                        createPath(nextEvent, optionalResponse.get());
                        var cycleFinder = new CycleFinder(this);
                        if (cycleFinder.hasCycle()) {
                            // Should be impossible, but we need to stop here otherwise we might create a stack overflow.
                            if (log.isErrorLevelEnabled()) {
                                log.logErrorMessage("Found a cycle in the event order: \n" + toString());
                            }
                            return;
                        }
                        if (hasPathTo(requestEvent, responseEvent)) {
                            // Check again if the path is created now. This might happen because of a request down the chain is connected now.
                            return;
                        }
                    }
                }
            }
            lastAfterRequest = next;
        }
        if (lastAfterRequest.equals(requestEvent)) {
            // Nothing found...
            return;
        }
        Vertex firstBeforeResponse = responseEvent;
        while (getAdjacentInVertices(firstBeforeResponse) != null && !getAdjacentInVertices(firstBeforeResponse).isEmpty()) {
            var next = getAdjacentInVertices(firstBeforeResponse).get(0);
            if (next.equals(responseEvent)) {
                // Somehow a cycle has introduced into the DAG. This should not be possible.
                break;
            }
            firstBeforeResponse = next;
        }
        addEdge(lastAfterRequest, firstBeforeResponse);
    }

    @Override
    public String toString() {
        StringBuilder stack = new StringBuilder();
        for (var entry : this.outdegreeMap.entrySet()) {
            if (stack.length() > 0) {
                stack.append("\n");
            }
            stack.append(entry.getKey().toString() + " references " + entry.getValue().stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        return stack.toString();
    }


    /**
     * Class that can find a cyclic order of the edges in a <code>DirectedGraph</code> instance.
     */
    private static class CycleFinder {
        private boolean[] marked;
        private int[] edgeTo;
        private boolean[] onStack;
        private LinkedList<Vertex> cycle;

        private CycleFinder(DirectedGraph directedGraph) {
            var verticesCount = directedGraph.getVertices().size();
            this.marked = new boolean[verticesCount];
            this.edgeTo = new int[verticesCount];
            this.onStack = new boolean[verticesCount];
            var vertices = new ArrayList<>(directedGraph.outdegreeMap.keySet());
            for (int vertexIx = 0; vertexIx < vertices.size(); vertexIx++) {
                if (!this.marked[vertexIx] && this.cycle == null) {
                    checkCycle(vertices, directedGraph.outdegreeMap, vertices.get(vertexIx));
                }
            }
        }

        private void checkCycle(List<Vertex> vertices, Map<Vertex, LinkedList<Vertex>> adjacencyMap, Vertex vertex) {
            var vertexIx = vertices.indexOf(vertex);
            this.onStack[vertexIx] = true;
            this.marked[vertexIx] = true;
            for (var adjacentVertex : adjacencyMap.get(vertex)) {
                final var adjacentVertexIx = vertices.indexOf(adjacentVertex);
                if (this.cycle != null) {
                    return;
                } else if (!this.marked[adjacentVertexIx]) {
                    this.edgeTo[adjacentVertexIx] = vertexIx;
                    checkCycle(vertices, adjacencyMap, adjacentVertex);
                } else if (onStack[adjacentVertexIx]) {
                    this.cycle = new LinkedList<>();
                    for (int x = vertexIx; x != adjacentVertexIx; x = this.edgeTo[x]) {
                        this.cycle.addFirst(vertices.get(x));
                    }
                    this.cycle.addFirst(vertices.get(adjacentVertexIx));
                    this.cycle.addFirst(vertices.get(vertexIx));
                }
            }
            this.onStack[vertexIx] = false;
        }

        /**
         * Boolean indicating the given <code>DirectedGraph</code> has a cycle.
         *
         * @return <code>true<code> when the <code>DirectedGraph</code> has one or more cycles, <code>false</code> otherwise.
         */
        boolean hasCycle() {
            return this.cycle != null;
        }
    }

    /**
     * Class that determines the <em>preorder</em>, <em>postorder</em> and <code>reverse postorder</code> of a given <code>DirectedGraph</code>.
     */
    private static class DepthFirstOrder {
        private boolean[] marked;
        private List<Vertex> preOrder = new ArrayList<>();
        private List<Vertex> postOrder = new ArrayList<>();

        DepthFirstOrder(DirectedGraph directedGraph) {
            var vertices = new ArrayList<>(directedGraph.getVertices());
            this.marked = new boolean[vertices.size()];
            for (int vertexIx = 0; vertexIx < vertices.size(); vertexIx++) {
                if (!this.marked[vertexIx]) {
                    setOrder(vertices, directedGraph.outdegreeMap, vertices.get(vertexIx));
                }
            }
        }

        private void setOrder(List<Vertex> vertices, Map<Vertex, LinkedList<Vertex>> adjacencyMap, Vertex vertex) {
            var vertexIx = vertices.indexOf(vertex);
            this.marked[vertexIx] = true;
            this.preOrder.add(vertex);
            for (var adjacentVertex : adjacencyMap.get(vertex)) {
                final var adjacentVertexIx = vertices.indexOf(adjacentVertex);
                if (!this.marked[adjacentVertexIx]) {
                    setOrder(vertices, adjacencyMap, adjacentVertex);
                }
            }
            this.postOrder.add(vertex);
        }

        /**
         * Returns the vertices in post order.
         *
         * @return The vertices in post order.
         */
        public List<Vertex> postOrder() {
            return this.postOrder;
        }

        /**
         * Returns the vertices in pre order.
         *
         * @return The vertices in pre order.
         */
        public List<Vertex> preOrder() {
            return this.preOrder;
        }

        /**
         * Returns the vertices in reverse post order.
         *
         * @return the vertices in reverse post order.
         */
        public List<Vertex> reversePostOrder() {
            var reverse = new LinkedList<Vertex>();
            for (var vertex : this.postOrder) {
                reverse.addFirst(vertex);
            }
            return reverse;
        }
    }

    /**
     * The <code>BreadthDirectedFirstPaths</code> class represents a data type for finding shortest paths (number of edges) from a source vertex <em>s</em> to every other vertex in the digraph.
     */
    private static class BreadthFirstDirectedPath {

        private List<Vertex> marked;
        private Map<Vertex, Vertex> edgeTo;
        private Map<Vertex, Integer> distanceTo;

        BreadthFirstDirectedPath(DirectedGraph directedGraph, Vertex sourceVertex) {
            this.marked = new ArrayList<>();
            this.edgeTo = new HashMap<>();
            this.distanceTo = new HashMap<>();
            setPaths(directedGraph.outdegreeMap, sourceVertex);
        }

        private void setPaths(Map<Vertex, LinkedList<Vertex>> adjacencyMap, Vertex sourceVertex) {
            var queue = new ArrayList<Vertex>();
            this.marked.add(sourceVertex);
            this.distanceTo.put(sourceVertex, 0);
            queue.add(sourceVertex);
            while (!queue.isEmpty()) {
                var vertex = queue.remove(0);
                for (var adjacentVertex : adjacencyMap.get(vertex)) {
                    if (!this.marked.contains(adjacentVertex)) {
                        this.edgeTo.put(adjacentVertex, vertex);
                        this.distanceTo.put(adjacentVertex, this.distanceTo.get(vertex) + 1);
                        this.marked.add(adjacentVertex);
                        queue.add(adjacentVertex);
                    }
                }
            }
        }

        /**
         * Is there a directed path from the source vertex to the given <code>Vertex</code>?
         *
         * @param vertex The <code>vertex</code>
         * @return <code>true</code> if there is a directed path,<code>false</code>> otherwise.
         */
        public boolean hasPathTo(Vertex vertex) {
            return this.marked.contains(vertex);
        }

        /**
         * Gives the number of edges in a shortest path from the source vertex to the given <code>Vertex</code>
         *
         * @param vertex The <code>vertex</code>
         * @return The number of edges in a shortest path, or -1 if there's no path.
         */
        public int distanceTo(Vertex vertex) {
            if (!hasPathTo(vertex)) {
                return -1;
            }
            return this.distanceTo.get(vertex);
        }

        /**
         * Returns a shortest path from the source vertex to the given <code>Vertex</code>.
         *
         * @param vertex The <code>>vertex</code.
         * @return A <code>List</code> of <code>Vertex</code> instances on a shortest path, or <code>null</code> if there's no path.
         */
        public List<Vertex> pathTo(Vertex vertex) {
            if (!hasPathTo(vertex)) {
                return null;
            }
            var path = new ArrayList<Vertex>();
            Vertex v;
            for (v = vertex; distanceTo(v) > 0; v = this.edgeTo.get(v)) {
                path.add(v);
            }
            path.add(v);
            return path;
        }
    }

}
