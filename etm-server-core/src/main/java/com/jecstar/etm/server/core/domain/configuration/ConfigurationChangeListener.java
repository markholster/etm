package com.jecstar.etm.server.core.domain.configuration;


public interface ConfigurationChangeListener {

	void configurationChanged(ConfigurationChangedEvent event);
}
