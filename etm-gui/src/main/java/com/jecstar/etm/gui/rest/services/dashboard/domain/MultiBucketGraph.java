package com.jecstar.etm.gui.rest.services.dashboard.domain;

/**
 * Superclass for all <code>Graph</code> instances that have multiple series/buckets in a single visual representation.
 */
public abstract class MultiBucketGraph extends Graph {

    /**
     * Gives the <code>Axes</code> of the <code>Graph</code>
     *
     * @return The <code>Axes</code> of the <code>Graph</code>
     */
    public abstract Axes getAxes();
}
