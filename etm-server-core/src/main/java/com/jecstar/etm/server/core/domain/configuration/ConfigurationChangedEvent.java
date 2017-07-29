package com.jecstar.etm.server.core.domain.configuration;

import java.util.List;

public class ConfigurationChangedEvent {

	private final List<String> changedConfigurationKeys;

	public ConfigurationChangedEvent(List<String> changedConfigurationKeys) {
		this.changedConfigurationKeys = changedConfigurationKeys;
	}
	
	public boolean isChanged(String configurationKey) {
		return this.changedConfigurationKeys.contains(configurationKey);
	}
	
	public boolean isAnyChanged(String ... configurationKeys) {
		for (String key : configurationKeys) {
			if (isChanged(key)) {
				return true;
			}
		}
		return false;
	}
}
