package com.holster.etm.core.configuration;

import java.util.Properties;

import com.holster.etm.core.util.ObjectUtils;

public class ConfigurationChangedEvent {

	private Properties oldProperties = new Properties();
	private Properties currentProperties = new Properties();
	private Properties changedProperties = new Properties();
	
	public ConfigurationChangedEvent(Properties oldProperties, Properties currentProperties) {
		if (oldProperties != null) {
			this.oldProperties.putAll(oldProperties);
		}
		if (currentProperties != null) {
			this.currentProperties.putAll(currentProperties);
			for (String key : currentProperties.stringPropertyNames()) {
				String currentValue = currentProperties.getProperty(key);
				String oldValue = null;
				if (oldProperties != null) {
					oldValue = oldProperties.getProperty(key);
				}
				if (!ObjectUtils.equalsNullProof(currentValue, oldValue)) {
					this.changedProperties.setProperty(key, currentValue);
				}
			}
		}
	}
	
	public boolean isChanged(String configurationKey) {
		return this.changedProperties.containsKey(configurationKey);
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
