package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import com.jecstar.etm.domain.writer.json.JsonWriter;

import java.text.Format;

/**
 * Interface for all values of an aggregation. Together with an <code>AggregationKey</code> these classes form a point (x,y) on a graph.
 *
 * @param <T> The value type.
 */
public interface AggregationValue<T> {

    /**
     * Gives the label of the value.
     *
     * @return The label.
     */
    String getLabel();

    /**
     * Gives the value.
     *
     * @return The original value is that is a valid value. Otherwise the
     * missingValue is returned when that is a valid value.
     * <code>null</code> is returned when both values are not valid.
     */
    T getValue();

    /**
     * Set the value to be used when the instance has an invalid value.
     *
     * @param t The value to be used when the instance has an invalid value.
     */
    void setMissingValue(T t);

    /**
     * Give the value to be used when the instance has an invalid value
     *
     * @return The value to be used when the instance has an invalid value
     */
    T getMissingValue();

    /**
     * Gives a string representation of the value.
     *
     * @param format The format that needs to be used to markup the value.
     * @return The formatter value.
     */
    String getValueAsString(Format format);

    /**
     * Add the value to a json buffer.
     *
     * @param jsonWriter   The writer to use to write json stings.
     * @param buffer       The buffer to add the value to.
     * @param firstElement boolean indicating this is the first element in a json object.
     */
    void appendValueToJsonBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement);

    /**
     * Append the entiry object to a json buffer.
     *
     * @param jsonWriter   The writer to use to write json stings.
     * @param format       The format that needs to be used to markup the value.
     * @param buffer       The buffer to add the value to.
     * @param firstElement boolean indicating this is the first element in a json object.
     */
    void appendToJsonBuffer(JsonWriter jsonWriter, Format format, StringBuilder buffer, boolean firstElement);

    /**
     * Indication this value is a percentage.
     *
     * @param percentage <code>true</code> if this value represents a percentage,
     *                   <code>false</code> otherwise.
     * @return This instance for method chaining.
     */
    AggregationValue<T> setPercentage(boolean percentage);

    /**
     * Boolean indicating this value is a percentage.
     *
     * @return <code>true</code> if this value represents a percentage,
     * <code>false</code> otherwise.
     */
    boolean isPercentage();

    /**
     * Boolean indicating this instance has a correct value.
     *
     * @return <code>true</code> if this instance has a correct value,
     * <code>false</code> otherwise.
     */
    boolean hasValidValue();

    /**
     * Checks if the value is equal to a given value. When the value is not set, the default missing value will be used for comparison.
     *
     * @param value The value to compare with.
     * @return <code>true</code> when the values are equal, <code>false</code> otherwise.
     */
    boolean isEqualTo(int value);

    /**
     * Checks if the value is greater than a given value. When the value is not set, the default missing value will be used for comparison.
     *
     * @param value The value to compare with.
     * @return <code>true</code> when the value is greater than the given value, <code>false</code> otherwise.
     */
    boolean isGreaterThan(int value);

    /**
     * Checks if the value is less than a given value. When the value is not set, the default missing value will be used for comparison.
     *
     * @param value The value to compare with.
     * @return <code>true</code> when the value is less than the given value, <code>false</code> otherwise.
     */
    boolean isLessThan(int value);


}
