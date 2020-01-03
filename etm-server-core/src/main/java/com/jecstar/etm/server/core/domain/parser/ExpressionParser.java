package com.jecstar.etm.server.core.domain.parser;

/**
 * Interface for all <code>ExpressionParser</code> instances.
 * <p>
 * An <code>ExpressionParser</code> is capable of extracting data from an <code>TelemetryEvent</code> before it is stored
 * to Elasticsearch. Some <code>ExpressionParser</code>s are capable of replacing content as well.
 */
public interface ExpressionParser extends AutoCloseable {

    /**
     * Gives the name of the <code>ExpressionParser</code>.
     *
     * @return The name of the <code>ExpressionParser</code>/
     */
    String getName();

    /**
     * Evaluates this <code>ExpressionParser</code> against the given content.
     * @param content The content to evaluate.
     * @return The extracted value or <code>null</code> when no value could be extracted.
     */
    String evaluate(String content);

    /**
     * Boolean indicating this <code>ExpressionParser</code> is capable of replacing content.
     *
     * @return <code>true</code> when this <code>ExpressionParser</code> is capable of replacing content.
     */
    boolean isCapableOfReplacing();

    /**
     * Replaces some (or all) parts of the given content.
     *
     * @param content     The content to evaluate and replace.
     * @param replacement The replacement.
     * @param allValues   <code>true</code> when all occurrences should be replaced. <code>false</code> when only the first
     *                    occurrence should be replaced.
     * @return The content with the replaced values.
     * @throws UnsupportedOperationException when {@link #isCapableOfReplacing()} returns <code>false</code>.
     */
    String replace(String content, String replacement, boolean allValues);
}
