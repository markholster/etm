package com.jecstar.etm.processor.elastic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

import com.codahale.metrics.Gauge;
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
		Gauge<Integer> blacklistGauge = new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				return BulkProcessorListener.this.blacklistedIds.size();
			}};
		metricRegistry.register("event-processor.blacklist-size", blacklistGauge);
	}
	
	@Override
	public void beforeBulk(long executionId, BulkRequest request) {
		this.metricContext.put(executionId, this.bulkTimer.time());
		cleanupBlacklist();
		Iterator<ActionRequest<?>> it = request.requests().iterator();
		while (it.hasNext()) {
			ActionRequest<?> action = it.next();
			if (isBlacklisted(action)) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Event with id '" + getEventId(action) + " is currently blacklisted and will not be persisted.");
				}
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
		if (response.hasFailures() && log.isErrorLevelEnabled()) {
			log.logErrorMessage(buildFailureMessage(response));
		}
		this.metricContext.remove(executionId).stop();
	}
	
	@Override
	public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
		if (log.isErrorLevelEnabled()) {
			log.logErrorMessage("Failure in bulk execution", failure);
		}
		this.metricContext.remove(executionId).stop();
	}

	private boolean isBlacklisted(ActionRequest<?> action) {
		return this.blacklistedIds.containsKey(getEventId(action));
	}
	
	private String getEventId(ActionRequest<?> action) {
        if (action instanceof IndexRequest) {
        	return ((IndexRequest) action).id();
        }  else if (action instanceof UpdateRequest) {
        	return ((UpdateRequest) action).id();
        } else if (action instanceof DeleteRequest) {
        	return ((DeleteRequest) action).id();
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
	
	private void cleanupBlacklist() {
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
	
	private String buildFailureMessage(BulkResponse bulkResponse) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("failure in bulk execution:");
        BulkItemResponse[] responses = bulkResponse.getItems();
        for (int i = 0; i < responses.length; i++) {
            BulkItemResponse response = responses[i];
            if (response.isFailed()) {
                buffer.append("\n[").append(i)
                        .append("]: index [").append(response.getIndex()).append("], type [").append(response.getType()).append("], id [").append(response.getId())
                        .append("], message [").append(getFailureMessage(response)).append("]");
                if (log.isDebugLevelEnabled() && response.getFailure() != null) {
                	log.logDebugMessage("Error persisting event with id '" + response.getId() + "'.", response.getFailure().getCause());
                }
             }
        }
        return buffer.toString();		
	}
	
	private String getFailureMessage(BulkItemResponse response) {
		if (response.getFailure() == null) {
			return null;
		}
		Throwable cause = response.getFailure().getCause();
		if (cause != null) {
			List<Throwable> causes = new ArrayList<>();
			Throwable rootCause = getRootCause(cause, causes);
			return rootCause.getMessage();
		}
		return response.getFailureMessage();
	}

	private Throwable getRootCause(Throwable cause, List<Throwable> causes) {
		if (causes.contains(cause)) {
			return causes.get(causes.size() - 1);
		}
		causes.add(cause);
		Throwable parent = cause.getCause();
		if (parent == null) {
			return cause;
		}
		return getRootCause(parent, causes);
	}


}
