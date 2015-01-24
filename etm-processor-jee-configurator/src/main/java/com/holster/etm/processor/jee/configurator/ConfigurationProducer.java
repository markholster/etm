package com.holster.etm.processor.jee.configurator;

import java.io.IOException;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.jee.configurator.core.ProcessorConfiguration;

@ManagedBean
@Singleton
public class ConfigurationProducer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ConfigurationProducer.class);

	private EtmConfiguration configuration;

	@Produces
	@ProcessorConfiguration
	public EtmConfiguration getEtmConfiguration() {
		synchronized (this) {
			if (this.configuration == null) {
				this.configuration = new EtmConfiguration();
				try {
	                this.configuration.load();
                } catch (InterruptedException e) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Thread interrupted while loading etm configuration.", e);
                	}
                }
			}
		}
		return this.configuration;
	}
	
	@PreDestroy
	public void preDestroy() {
		if (this.configuration != null) {
			try {
	            this.configuration.close();
            } catch (IOException e) {
            	if (log.isWarningLevelEnabled()) {
            		log.logWarningMessage("Unable to close etm configuration.", e);
            	}
            }
		}
	}
}
