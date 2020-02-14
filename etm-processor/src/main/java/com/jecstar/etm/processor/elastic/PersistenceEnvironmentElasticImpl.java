/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
