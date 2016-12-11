package com.jecstar.etm.gui.rest.services.dashboard.aggregation;

import java.text.Format;

import com.jecstar.etm.domain.writers.json.JsonWriter;

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
	 *         missingValue is returned when that is a valid value.
	 *         <code>null</code> is returned when both values are not valid.
	 */
	T getValue();

	/**
	 * Set the value to be used when the instance has an invalid value.
	 * 
	 * @param t
	 *            The value to be used when the instance has an invalid value.
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
	 * @param format
	 *            The format that needs to be used to markup the value.
	 * @return The formatter value.
	 */
	String getValueAsString(Format format);

	/**
	 * Add the value to a json buffer.
	 * 
	 * @param jsonWriter
	 *            The writer to use to write json stings.
	 * @param buffer
	 *            The buffer to add the value to.
	 * @param firstElement
	 *            boolean indicating this is the first element in a json object.
	 */
	void appendValueToJsonBuffer(JsonWriter jsonWriter, StringBuilder buffer, boolean firstElement);

	/**
	 * Append the entiry object to a json buffer.
	 * 
	 * @param jsonWriter
	 *            The writer to use to write json stings.
	 * @param format
	 *            The format that needs to be used to markup the value.
	 * @param buffer
	 *            The buffer to add the value to.
	 * @param firstElement
	 *            boolean indicating this is the first element in a json object.
	 */
	void appendToJsonBuffer(JsonWriter jsonWriter, Format format, StringBuilder buffer, boolean firstElement);

	/**
	 * Indication this value is a percentage.
	 * 
	 * @param percentage
	 *            <code>true</code> if this value represents a percentage,
	 *            <code>false</code> otherwise.
	 * 
	 * @return This instance for method chaining.
	 */
	AggregationValue<T> setPercentage(boolean percentage);

	/**
	 * Boolean indicating this value is a percentage.
	 * 
	 * @return <code>true</code> if this value represents a percentage,
	 *         <code>false</code> otherwise.
	 */
	boolean isPercentage();

	/**
	 * Boolean indicating this instance has a correct value.
	 * 
	 * @return <code>true</code> if this instance has a correct value,
	 *         <code>false</code> otherwise.
	 */
	boolean hasValidValue();
}
