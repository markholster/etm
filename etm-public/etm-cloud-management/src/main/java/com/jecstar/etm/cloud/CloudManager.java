package com.jecstar.etm.cloud;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;

/**
 * Main entry point that handles all actions on any of the cloud providers.
 */
public interface CloudManager {

    /**
     * Creates a new namespace. Each customer should be created in a separate namespace.
     *
     * @param namespace The namespace to user.
     * @return The created <code>Namespace</code> instance.
     */
    Namespace createNamespace(String namespace);

    /**
     * Returns all <code>Namespace</code> instances.
     *
     * @return The <code>NamespaceList</code> instance with all namespaces.
     */
    NamespaceList getNamespaces();
}
