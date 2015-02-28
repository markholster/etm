package com.jecstar.etm.core.util;

public final class DateUtils {

	public static long normalizeTime(long timeInMillis, long factor) {
		return (timeInMillis / factor) * factor;
    }

}
