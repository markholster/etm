package com.jecstar.etm.processor.elastic;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.core.CommandResources;
import com.jecstar.etm.processor.core.PersistenceEnvironment;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.io.IOException;

public class PersistenceEnvironmentElasticImpl implements PersistenceEnvironment {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(PersistenceEnvironmentElasticImpl.class);

    private final EtmConfiguration etmConfiguration;
    private final DataRepository dataRepository;
    private CommandResources commandResources;

    public PersistenceEnvironmentElasticImpl(final EtmConfiguration etmConfiguration, final DataRepository dataRepository) {
        this.etmConfiguration = etmConfiguration;
        this.dataRepository = dataRepository;
    }

    @Override
    public CommandResources getCommandResources(final MetricRegistry metricRegistry) {
        synchronized (this) {
            if (this.commandResources == null) {
                this.commandResources = new CommandResourcesElasticImpl(this.dataRepository, this.etmConfiguration, metricRegistry);
            }
        }
        return this.commandResources;
    }

    @Override
    public void close() {
        if (this.commandResources != null) {
            try {
                this.commandResources.close();
            } catch (IOException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Failed to close CommandResources", e);
                }
            }
        }
    }

}
