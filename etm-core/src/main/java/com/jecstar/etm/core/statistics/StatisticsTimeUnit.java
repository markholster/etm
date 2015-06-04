package com.jecstar.etm.core.statistics;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public enum StatisticsTimeUnit {

	MINUTE(60), HOUR(60 * 60), DAY(60 * 60 * 24);

	private long seconds;
	private StatisticsTimeUnit(long seconds) {
		this.seconds = seconds;
	}
	 
	public Date toDate(LocalDateTime timestamp) {
	    long epochSeconds = timestamp.atZone(ZoneOffset.systemDefault()).toEpochSecond();
	    long milliseconds = ((epochSeconds / this.seconds) * this.seconds) * 1000; 
	    return new Date(milliseconds);
    }
}
