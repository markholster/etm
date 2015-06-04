package com.jecstar.etm.core.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class DateUtils {
	
	private static DateFormat GMT_DAY_FORMAT = new SimpleDateFormat("yyyyMMdd");

	static {
		GMT_DAY_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public static long normalizeTime(long timeInMillis, long factor) {
		return (timeInMillis / factor) * factor;
    }
	
	public static String toGMTDay(Date date) {
		return GMT_DAY_FORMAT.format(date);
	}

}
