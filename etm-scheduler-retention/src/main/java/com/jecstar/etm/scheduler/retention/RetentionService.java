package com.jecstar.etm.scheduler.retention;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

import com.datastax.driver.core.Session;
import com.jecstar.etm.core.cassandra.PartitionKeySuffixCreator;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.core.util.DateUtils;
import com.jecstar.etm.jee.configurator.core.SchedulerConfiguration;

@ManagedBean
@Singleton
@Startup
public class RetentionService extends LeaderSelectorListenerAdapter {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(RetentionService.class);

	@Inject
	@SchedulerConfiguration
	private EtmConfiguration etmConfiguration;

	@Inject
	@SchedulerConfiguration
	private SolrClient solrClient;
	
	@Inject
	@SchedulerConfiguration
	private Session session;
	
	private DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private LeaderSelector leaderSelector;
	private EtmDataCleaner etmDataCleaner;
	private Date lastCleanupTime = new Date();

	@PostConstruct
	public void registerRetentionService() {
		this.solrDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.etmDataCleaner = new EtmDataCleaner(this.session);
		this.leaderSelector = this.etmConfiguration.createLeaderSelector("/data-retention", this);
		this.leaderSelector.autoRequeue();
		this.leaderSelector.start();
	}

	@Override
    public void takeLeadership(CuratorFramework client) {
		boolean stopProcessing = false;
	    while(!stopProcessing && this.leaderSelector.hasLeadership()) {
	    	try {
		    	if (log.isDebugLevelEnabled()) {
		    		log.logDebugMessage("Removing events with expired retention.");
		    	}
		    	try {
		    		Date dateTill = new Date();
		    		this.solrClient.deleteByQuery("retention:[* TO " + this.solrDateFormat.format(dateTill) + "]");
		    		this.solrClient.commit(false, false, true);
			    	Date cleanupTime = new Date(DateUtils.normalizeTime(dateTill.getTime(), PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1)));
			    	if (!this.lastCleanupTime.equals(cleanupTime)) {
			    		this.lastCleanupTime = cleanupTime;
						this.etmDataCleaner.cleanup(cleanupTime, this.etmConfiguration.isDataRetentionPreserveEventCounts(),
						        this.etmConfiguration.isDataRetentionPreserveEventPerformances(),
						        this.etmConfiguration.isDataRetentionPreserveEventSlas());
			    	}
	            } catch (SolrServerException | IOException e) {
	            	if (log.isErrorLevelEnabled()) {
	            		log.logErrorMessage("Error removing expired events", e);
	            	}
	            }
		    	long timeOut = this.etmConfiguration.getDataRetentionCheckInterval();
		    	if (timeOut < 30000) {
		    		Thread.sleep(this.etmConfiguration.getDataRetentionCheckInterval());
		    	} else {
		    		long endSleep = System.currentTimeMillis() + timeOut;
		    		while (System.currentTimeMillis() < endSleep) {
		    			long sleepTime = endSleep - System.currentTimeMillis();
		    			if (sleepTime > 0) {
		    				if (sleepTime > 30000) {
		    					Thread.sleep(30000);
		    				} else {
		    					Thread.sleep(sleepTime);
		    				}
		    			}
		    		}
		    	}
            } catch (InterruptedException e) {
            	if (log.isWarningLevelEnabled()) {
            		log.logWarningMessage("Retention service interrupted. Giving back leadership.");
            	}
            	Thread.currentThread().interrupt();
            	stopProcessing = true;
            } catch (Exception e) {
            	if (log.isErrorLevelEnabled()) {
            		log.logErrorMessage("Retention service threw an exception. Giving back leadership.", e);
            	}
            	stopProcessing = true;
            }
	    }
    }
	
	@PreDestroy
	public void preDestroy() {
		if (this.leaderSelector != null) {
			this.leaderSelector.close();
		}
		this.leaderSelector = null;
	}

}
