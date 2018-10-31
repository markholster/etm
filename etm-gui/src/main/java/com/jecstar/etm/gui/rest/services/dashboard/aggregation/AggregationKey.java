package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import com.jecstar.etm.domain.writer.json.JsonWriter;

/**
 * Interface for all classes that represent an aggregation key.
 */
public interface AggregationKey extends Comparable<AggregationKey> {

    /**
     * Gives a <code>String</code> representation of the key.
     *
     * @return The key in a human readable form.
     */
    String getKeyAsString();

    /**
     * Convert the key to an escaped json value.
     *
     * @param jsonWriter The <code>JsonWriter</code> used for json escaping.
     * @return The key as json string.
     */
    String toJsonValue(JsonWriter jsonWriter);

}
