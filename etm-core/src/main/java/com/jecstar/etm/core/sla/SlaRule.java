package com.jecstar.etm.core.sla;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

public class SlaRule {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(SlaRule.class);

	
	private int threshold;
	private long thresholdValidity;
	private long slaExpiryTime;
	private long notificationTimeout;
	private String notifcations;
	
	private SlaRule(int threshold, long thresholdValidity, long slaExpiryTime, long notificationTimeout, String notifications) {
		this.threshold = threshold;
		this.thresholdValidity = thresholdValidity;
		this.slaExpiryTime = slaExpiryTime;
		this.notificationTimeout = notificationTimeout;
		this.notifcations = notifications;
	}
 	
	public static SlaRule fromConfiguration(String configuration) {
		if (configuration == null) {
			return null;
		}
		String[] split = configuration.split(",");
		if (split.length != 5) {
			return null;
		}
		try {
			 return new SlaRule(Integer.valueOf(split[1]), Long.valueOf(split[2]), Long.valueOf(split[0]), Long.valueOf(split[3]), split[4]);
		} catch (Exception e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Could not create sla rule.", e);
			}
		}
		return null;
	}
	
	public long getSlaExpiryTime() {
	    return this.slaExpiryTime;
    }
	
	public String toConfiguration() {
		return this.slaExpiryTime + "," + this.threshold + "," + this.thresholdValidity + "," + this.notificationTimeout + "," + this.notifcations;
	}
	
	public boolean compliesToSla(long responseTime) {
		return responseTime <= this.slaExpiryTime;
	}
}
