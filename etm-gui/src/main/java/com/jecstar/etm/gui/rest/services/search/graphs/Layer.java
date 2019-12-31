package com.jecstar.etm.gui.rest.services.search.graphs;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The <code>Layer</code> class represents a horizontal row in a directed graph.
 */
public class Layer {

    /**
     * The layer index starting a zero for the top layer.
     */
    private final int index;

    /**
     * A list of <code>Vertex</code> instances.
     */
    private final List<Vertex> vertices = new ArrayList<>();

    /**
     * A map with patent <code>Vertex</code> keys and their corresponding child <code>Vertex</code> instances.
     */
    private final Map<Vertex, List<Vertex>> parentMap = new HashMap<>();

    /**
     * Creates a new <code>Layer</code> instance.
     *
     * @param index The index of the layer starting at zero for the top <code>Layer</code>.
     */
    public Layer(int index) {
        this.index = index;
    }

    /**
     * Gives the index of the layer. The top layer has an index equal to zero.
     *
     * @return The index of the layer.
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Code add a <code>Vertex</code> at the end of this <code>Layer</code>.
     *
     * @param vertex The <code>Vertex</code> to add to this <code>Layer</code>.
     * @return This <code>Layer</code> instance for chaining methods.
     */
    public Layer addVertex(Vertex vertex) {
        this.vertices.add(vertex);
        return this;
    }

    /**
     * Code add a <code>Vertex</code> and it's child vertices at the end of this <code>Layer</code>.
     *
     * @param vertex        The <code>Vertex</code> to add to this <code>Layer</code>.
     * @param childVertices The <code>Vertex</code> instances that have a reference to the given vertex as parent.
     * @return This <code>Layer</code> instance for chaining methods.
     */
    private Layer addVertex(Vertex vertex, List<Vertex> childVertices) {
        addVertex(vertex);
        this.parentMap.put(vertex, childVertices);
        return this;
    }


    /**
     * Gives the <code>Vertex</code> instances that should be plotted within this <code>Layer</code>.
     *
     * @return The <code>Vertex</code> instances of this <code>Layer</code>.
     */
    public List<Vertex> getVertices() {
        return this.vertices;
    }

    /**
     * Returns the child <code>Vertex</code> instances of the given <code>Vertex</code>.
     *
     * @param vertex The parent <code>Vertex</code>
     * @return The child <code>Vertext</code> instances, or <code>null</code> when the give <code>Vertex</code> isn't a parent <code>Vertex</code>.
     */
    public List<Vertex> getChildVertices(Vertex vertex) {
        return this.parentMap.get(vertex);
    }

    /**
     * Adds the given <code>Vertex</code> instances to this layer. This method will determine which exact <code>Vertex</code> instances belong to this <code>Layer</code>, and pushes all remaining vertices to the next <code>Layer</code>.
     *
     * @param layers          The current available <code>Layer</code> instances.
     * @param orderedVertices The <code>Vertex</code> instances that should be added to this or a lower <code>Layer</code>.
     */
    public void addAndOptimize(ArrayList<Layer> layers, List<Vertex> orderedVertices) {
        if (orderedVertices == null || orderedVertices.isEmpty()) {
            return;
        }
        if (orderedVertices.size() <= 2 && orderedVertices.stream().allMatch(p -> p.getParent() == null)) {
            // 2 vertices may always be on the same layer.
            this.vertices.addAll(orderedVertices);
            orderedVertices.clear();
            return;
        }
        if (orderedVertices.stream().allMatch(p -> p.getParent() == null)) {
            // No parents/containers, or first and last vertices are not in an container.
            // Add the first and last vertex to this layer and pass rest of the vertices to the next layer.
            var childLayer = getOrCreateLayer(getIndex() + 1, layers);
            // Add the first and last vertex to this layer.
            addVertex(orderedVertices.remove(0));
            addVertex(orderedVertices.remove(orderedVertices.size() - 1));
            // Pass through the rest to the next layer.
            childLayer.addAndOptimize(layers, orderedVertices);
        } else if (orderedVertices.get(0).getParent() == null && orderedVertices.get(orderedVertices.size() - 1).getParent() == null) {
            // No container at start and end of chain:
            // 1. Add the start of the chain to this layer.
            // 2. Then handle all events from first event with parent until last event with parent.
            // 3. Finally add the end of the chain to this layer.
            List<Vertex> withParent = orderedVertices.stream().filter(p -> p.getParent() != null).collect(Collectors.toList());
            var head = new ArrayList<>(orderedVertices.subList(0, orderedVertices.indexOf(withParent.get(0))));
            var tail = new ArrayList<>(orderedVertices.subList(orderedVertices.indexOf(withParent.get(withParent.size() - 1)) + 1, orderedVertices.size()));
            orderedVertices.removeAll(head);
            orderedVertices.removeAll(tail);
            // Step 1.
            addAndOptimize(layers, head);
            // Step 2.
            addAndOptimize(layers, orderedVertices);
            // Step 3.
            addAndOptimize(layers, tail);
        } else if (orderedVertices.get(0).getParent() != null) {
            // Container at start of chain.
            var parent = orderedVertices.get(0).getParent();
            var childVertices = orderedVertices.stream().filter(p -> p.getParent() != null && p.getParent().equals(parent)).collect(Collectors.toList());
            var lastVertexWithParent = childVertices.get(childVertices.size() - 1);
            // Find all vertices within the first and last vertex of the parent.
            var parentScopedVertices = new ArrayList<>(orderedVertices.subList(0, orderedVertices.indexOf(lastVertexWithParent) + 1));
            orderedVertices.removeAll(parentScopedVertices);
            // subVertices contains all vertices that are not within the parent, but called directly or indirectly from the parent.
            var subVertices = new ArrayList<List<Vertex>>();
            var currentSubs = new ArrayList<Vertex>();
            for (var parentScopedVertex : parentScopedVertices) {
                if (parentScopedVertex.getParent() != null && parentScopedVertex.getParent().equals(parent)) {
                    if (currentSubs.size() > 0) {
                        subVertices.add(currentSubs);
                        currentSubs = new ArrayList<>();
                    }
                    continue;
                }
                currentSubs.add(parentScopedVertex);
            }
            Optional<Layer> optionalLayer = layers.stream().filter(p -> p.getVertices().contains(parent)).findFirst();
            // Find the layer that has the parent, or create a new one.
            if (optionalLayer.isPresent()) {
                // Container is already known on a layar.
                if (subVertices.size() > 0) {
                    var subLayer = getOrCreateLayer(optionalLayer.get().getIndex() + 1, layers);
                    for (var subs : subVertices) {
                        subLayer.addAndOptimize(layers, subs);
                    }
                }
            } else if (getIndex() == 0 && getVertices().size() == 0) {
                // Container not yet present and container is first item in chain
                addVertex(parent, childVertices);
                if (subVertices.size() > 0) {
                    var subLayer = getOrCreateLayer(getIndex() + 1, layers);
                    for (var subs : subVertices) {
                        subLayer.addAndOptimize(layers, subs);
                    }
                }
            } else {
                var childLayer = getOrCreateLayer(getIndex() + 1, layers);
                childLayer.addVertex(parent, childVertices);
                if (subVertices.size() > 0) {
                    var subLayer = getOrCreateLayer(childLayer.getIndex() + 1, layers);
                    for (var subs : subVertices) {
                        subLayer.addAndOptimize(layers, subs);
                    }
                }
            }
            // Continue after the parent.
            addAndOptimize(layers, orderedVertices);
        }
    }

    /**
     * Get or create a new <code>Layer</code> instance with a given index.
     *
     * @param index         The index at which the layer should be positioned.
     * @param currentLayers The current available <code>Layer</code> instances.
     * @return The <code>Layer</code> instance with the given index.
     */
    private Layer getOrCreateLayer(int index, List<Layer> currentLayers) {
        if (index >= currentLayers.size()) {
            // Create the next layer if not present.
            var childLayer = new Layer(index);
            currentLayers.add(childLayer);
        }
        return currentLayers.get(index);

    }


}
