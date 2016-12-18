package com.jecstar.etm.server.core.util;

/**
 * Utility class for <code>Object</code> manipulations.
 * 
 * @author Mark Holster
 */
public final class ObjectUtils {

    /**
     * Equals method that is <code>null</code> proof.
     * 
     * @param object1
     *            The object to compare with the other object.
     * @param object2
     *            The object to compare with the other object.
     * @return <code>true</code> when both objects are <code>null</code> or equal, <code>false</code>
     *         otherwise.
     */
    public static boolean equalsNullProof(Object object1, Object object2) {
    	return equalsNullProof(object1, object2, true);
    }
    
    public static boolean equalsNullProof(Object object1, Object object2, boolean trueWhenBothNull) {
        if (object1 == null ^ object2 == null) {
            return false;
        } else if (object1 == null && object2 == null) {
            return trueWhenBothNull;
        }
        return object1.equals(object2);
    }
}
