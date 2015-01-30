package com.holster.etm.scheduler.retention;

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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;

import com.datastax.driver.core.Session;
import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.jee.configurator.core.SchedulerConfiguration;

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
	private SolrServer solrServer;
	
	@Inject
	@SchedulerConfiguration
	private Session session;
	
	private RemovingCallbackHandler callbackHandler;

	
	private DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private LeaderSelector leaderSelector;

	@PostConstruct
	public void registerRetentionService() {
		this.solrDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.callbackHandler = new RemovingCallbackHandler(this.solrServer, this.session, this.etmConfiguration);
		this.leaderSelector = this.etmConfiguration.createLeaderSelector("/leader-election-retention", this);
		this.leaderSelector.autoRequeue();
		this.leaderSelector.start();
	}

	@Override
    public void takeLeadership(CuratorFramework client) {
		boolean stopProcessing = false;
	    while(!stopProcessing && this.leaderSelector.hasLeadership()) {
	    	try {
		    	long dataRetentionTime = this.etmConfiguration.getDataRetentionTime();
		    	if (log.isInfoLevelEnabled()) {
		    		log.logInfoMessage("Removing events older than " + dataRetentionTime + " ms.");
		    	}
		    	if (dataRetentionTime > 0) {
			    	Date dateTill = new Date(System.currentTimeMillis() - dataRetentionTime + 1);
			    	try {
			    		SolrQuery query = new SolrQuery("creationTime:[* TO " + solrDateFormat.format(dateTill) + "]");
			    		query.setStart(0);
			    		// Max 50000 rows per iteration, otherwise the http response will be too big.
			    		query.setRows(50000);
			            this.solrServer.queryAndStreamResponse(query, this.callbackHandler);
		            } catch (SolrServerException | IOException e) {
		            	if (log.isErrorLevelEnabled()) {
		            		log.logErrorMessage("Error removing events added before " + solrDateFormat.format(dateTill), e);
		            	}
		            }
		    	}
	            Thread.sleep(this.etmConfiguration.getDataRetentionCheckInterval());
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
		if (this.callbackHandler != null) {
			this.callbackHandler.close();
		}
		this.leaderSelector = null;
		this.callbackHandler = null;
	}

}
