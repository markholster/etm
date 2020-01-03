package com.jecstar.etm.server.core.domain;

import com.jecstar.etm.server.core.enhancers.TelemetryEventEnhancer;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.LruCache;

public class ImportProfile implements LruCache.LruCacheCallback {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(ImportProfile.class);

    public String name;
    public TelemetryEventEnhancer eventEnhancer;

    public ImportProfile initialize() {
        this.name = null;
        this.eventEnhancer = null;
        return this;
    }

    @Override
    public void removedFromCache() {
        if (this.eventEnhancer != null) {
            try {
                this.eventEnhancer.close();
            } catch (Exception e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage(e.getMessage(), e);
                }
            }
        }
    }
}
