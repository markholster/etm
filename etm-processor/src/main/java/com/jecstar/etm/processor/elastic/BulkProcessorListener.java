package com.jecstar.etm.processor.elastic;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class BulkProcessorListener implements BulkProcessor.Listener {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(PersistenceEnvironmentElasticImpl.class);

	private static final int MAX_MODIFICATIONS_BEFORE_BLACKLISTING = 100;
	private static final int BLACKLIST_TIME = 60 * 60 * 1000; // Blacklist potential dangerous events for an hour.
	
	private final Timer bulkTimer;
	private Map<Long, Context> metricContext = new ConcurrentHashMap<Long, Context>();
	private Map<String, Long> blacklistedIds = new ConcurrentHashMap<String, Long>();
	
	public BulkProcessorListener(final MetricRegistry metricRegistry) {
		this.bulkTimer = metricRegistry.timer("event-processor.persisting-repository-bulk-update");
	}
	
	@Override
	public void beforeBulk(long executionId, BulkRequest request) {
		this.metricContext.put(executionId, this.bulkTimer.time());
		clealupBlacklist();
		Iterator<ActionRequest<?>> it = request.requests().iterator();
		while (it.hasNext()) {
			ActionRequest<?> action = it.next();
			if (isBlacklisted(action)) {
				it.remove();
			}
		}
	}

	@Override
	public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
		for (BulkItemResponse itemResponse : response.getItems()) {
			if (itemResponse.getVersion() > MAX_MODIFICATIONS_BEFORE_BLACKLISTING) {
				addToBlacklist(itemResponse.getId());
			}
		}
		this.metricContext.remove(executionId).stop();
	}

	@Override
	public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
		this.metricContext.remove(executionId).stop();
	}


	private boolean isBlacklisted(ActionRequest<?> action) {
        if (action instanceof IndexRequest) {
        	return this.blacklistedIds.containsKey(((IndexRequest) action).id());
        }  else if (action instanceof UpdateRequest) {
        	return this.blacklistedIds.containsKey(((UpdateRequest) action).id());
        } else if (action instanceof DeleteRequest) {
        	return this.blacklistedIds.containsKey(((DeleteRequest) action).id());
        } else {
            throw new IllegalArgumentException("No support for request [" + action + "]");
        }
	}

	
	private void addToBlacklist(String id) {
		if (!this.blacklistedIds.containsKey(id)) {
			this.blacklistedIds.put(id, System.currentTimeMillis());
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Event id '" + id + "' blacklisted for " + (BLACKLIST_TIME / 1000) + " seconds.");
			}
		}
	}
	
	private void clealupBlacklist() {
		final long startTime = System.currentTimeMillis();
		Iterator<Entry<String, Long>> iterator = this.blacklistedIds.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Long> entry = iterator.next();
			if (startTime > (entry.getValue() + BLACKLIST_TIME)) {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Event id '" + entry.getKey() + "' no longer blacklisted.");
				}				
				iterator.remove();
			}
		}
	}


}
