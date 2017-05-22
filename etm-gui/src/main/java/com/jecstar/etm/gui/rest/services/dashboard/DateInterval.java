package com.jecstar.etm.gui.rest.services.dashboard;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;

public enum DateInterval {

	SECONDS("yyyy-MM-dd'T'HH:mm:ss", DateHistogramInterval.SECOND, 1000l), 
	MINUTES("yyyy-MM-dd'T'HH:mm", DateHistogramInterval.MINUTE, 60 * 1000l), 
	HOURS("yyyy-MM-dd'T'HH:mm", DateHistogramInterval.HOUR, 60 * 60 * 1000l), 
	DAYS("yyyy-MM-dd", DateHistogramInterval.DAY , 24 * 60 * 60 * 1000l), 
	WEEKS("yyyy-ww", DateHistogramInterval.WEEK, 7 * 24 * 60 * 60 * 1000l), 
	MONTHS("yyyy-MM", DateHistogramInterval.MONTH, 30 * 24 * 60 * 60 * 1000l), 
	QUARTERS(null, DateHistogramInterval.QUARTER, 13 * 7 * 24 * 60 * 60 * 1000l), 
	YEARS("yyyy", DateHistogramInterval.YEAR, 365 * 24 * 60 * 60 * 1000l);
	
	private final DateHistogramInterval dateHistogramInterval;
	private final String simpleDateFormat;
	private final long milliseconds;

	private DateInterval(String simpleDateFormat, DateHistogramInterval dateHistogramInterval, long milliseconds) {
		this.simpleDateFormat = simpleDateFormat;
		this.dateHistogramInterval = dateHistogramInterval;
		this.milliseconds = milliseconds;
	}
	
	public DateHistogramInterval getDateHistogramInterval() {
		return this.dateHistogramInterval;
	}
	
	public DateFormat getDateFormat(Locale locale, TimeZone timeZone) {
		if (DateInterval.QUARTERS.equals(this)) {
			return new QuarterDateFormat(locale, timeZone);
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat(this.simpleDateFormat, locale);
		dateFormat.setTimeZone(timeZone);
		return dateFormat;
	}

	public static DateInterval safeValueOf(String interval) {
		if ("seconds".equals(interval)) {
			return DateInterval.SECONDS;
		} else if ("minutes".equals(interval)) {
			return DateInterval.MINUTES;
		} else if ("hours".equals(interval)) {
			return DateInterval.HOURS;
		} else if ("days".equals(interval)) {
			return DateInterval.DAYS;
		} else if ("weeks".equals(interval)) {
			return DateInterval.WEEKS;
		} else if ("months".equals(interval)) {
			return DateInterval.MONTHS;
		} else if ("quarters".equals(interval)) {
			return DateInterval.QUARTERS;
		} else if ("years".equals(interval)) {
			return DateInterval.YEARS;
		}
		return DateInterval.DAYS;
	}

	/**
	 * Calculates a <code>DateInterval</code> based on a number of given
	 * milliseconds.
	 * 
	 * @param milliseconds
	 *            The number of milliseconds to calculate the best
	 *            <code>DateInterval</code> for.
	 * @return The appropriate <code>DateInterval</code>.
	 */
	public static DateInterval ofRange(long milliseconds) {
		final long flipFactor = 100;
		if (milliseconds > DateInterval.QUARTERS.milliseconds * flipFactor) {
			return DateInterval.YEARS;
		} else if (milliseconds > DateInterval.MONTHS.milliseconds * flipFactor) {
			return DateInterval.QUARTERS;
		} else if (milliseconds > DateInterval.WEEKS.milliseconds * flipFactor) {
			return DateInterval.MONTHS;
		} else if (milliseconds > DateInterval.DAYS.milliseconds * flipFactor) {
			return DateInterval.WEEKS;
		} else if (milliseconds > DateInterval.HOURS.milliseconds * flipFactor) {
			return DateInterval.DAYS;
		} else if (milliseconds > DateInterval.MINUTES.milliseconds * 120) {
			return DateInterval.HOURS;
		} else if (milliseconds > DateInterval.SECONDS.milliseconds * 120) {
			return DateInterval.MINUTES;
		}
		return DateInterval.SECONDS;
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
