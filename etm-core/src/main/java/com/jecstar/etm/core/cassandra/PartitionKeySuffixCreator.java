package com.jecstar.etm.core.cassandra;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class PartitionKeySuffixCreator extends SimpleDateFormat {

	/**
	 * The serialVersionUID for this class.
	 */
    private static final long serialVersionUID = 8202775484176243127L;
    
    // TODO this properties should be stored in EtmConfiguration
    public static final int SMALLEST_CALENDAR_UNIT = Calendar.HOUR_OF_DAY;
    public static final TimeUnit SMALLEST_TIMUNIT_UNIT = TimeUnit.HOURS;

	public PartitionKeySuffixCreator() {
		super("yyyyMMddHH");
		setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
		toAppendTo.append("-");
	    return super.format(date, toAppendTo, pos);
	}
	
}
