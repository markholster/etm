package com.jecstar.etm.gui.rest.services.dashboard;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;

public enum Interval {

	SECONDS("yyyy-MM-dd'T'HH:mm:ssXXX", DateHistogramInterval.SECOND), 
	MINUTES("yyyy-MM-dd'T'HH:mmXXX", DateHistogramInterval.MINUTE), 
	HOURS("yyyy-MM-dd'T'HH:mmXXX", DateHistogramInterval.HOUR), 
	DAYS("yyyy-MM-dd", DateHistogramInterval.DAY), 
	WEEKS("yyyy-YY", DateHistogramInterval.WEEK), 
	MONTHS("yyyy-MM", DateHistogramInterval.MONTH), 
	QUARTERS(null, DateHistogramInterval.QUARTER), 
	YEARS("yyyy", DateHistogramInterval.YEAR);
	
	private final DateHistogramInterval dateHistogramInterval;
	private final String simpleDateFormat;

	private Interval(String simpleDateFormat, DateHistogramInterval dateHistogramInterval) {
		this.simpleDateFormat = simpleDateFormat;
		this.dateHistogramInterval = dateHistogramInterval;
	}
	
	public DateHistogramInterval getDateHistogramInterval() {
		return this.dateHistogramInterval;
	}
	
	public DateFormat getDateFormat(Locale locale, TimeZone timeZone) {
		if (Interval.QUARTERS.equals(this)) {
			return new QuarterDateFormat(locale, timeZone);
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat(this.simpleDateFormat, locale);
		dateFormat.setTimeZone(timeZone);
		return dateFormat;
	}

	public static Interval safeValueOf(String interval) {
		if ("seconds".equals(interval)) {
			return Interval.SECONDS;
		} else if ("minutes".equals(interval)) {
			return Interval.MINUTES;
		} else if ("hours".equals(interval)) {
			return Interval.HOURS;
		} else if ("days".equals(interval)) {
			return Interval.DAYS;
		} else if ("weeks".equals(interval)) {
			return Interval.WEEKS;
		} else if ("months".equals(interval)) {
			return Interval.MONTHS;
		} else if ("quarters".equals(interval)) {
			return Interval.QUARTERS;
		} else if ("years".equals(interval)) {
			return Interval.YEARS;
		}
		return Interval.DAYS;
	}
	
	private class QuarterDateFormat extends SimpleDateFormat {
		
		private static final long serialVersionUID = 7698888710576662511L;

		private final Calendar calendar;
		
		private QuarterDateFormat(Locale locale, TimeZone timeZone) {
			super("yyyy");
			this.calendar = Calendar.getInstance(locale);
			this.calendar.setTimeZone(timeZone);
		}
		
		@Override
		public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
			this.calendar.setTime(date);
			return super.format(date, toAppendTo, pos).append(" Q" + ((this.calendar.get(Calendar.MONTH) / 3) + 1));
		}
	}
	
}
