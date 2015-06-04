package com.jecstar.etm.core.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Date;

public final class DateUtils {
	
	public static String toUTCDay(Date date) {
		return toUTCDay(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
	}
	
	public static String toUTCDay(LocalDateTime localDateTime) {
		return localDateTime.get(ChronoField.YEAR) + addLeadingZeroes(localDateTime.get(ChronoField.MONTH_OF_YEAR)) + addLeadingZeroes(localDateTime.get(ChronoField.DAY_OF_MONTH));
	}
	
	public static Date toDate(LocalDateTime localDateTime) {
	    long epochMilliSeconds = localDateTime.atZone(ZoneOffset.systemDefault()).toEpochSecond() * 1000;
		return new Date(epochMilliSeconds);
	}
	
	private static String addLeadingZeroes(int timeUnit) {
		if (timeUnit < 10) {
			return "0" + timeUnit;
		}
		return "" + timeUnit;
	}
	


}
