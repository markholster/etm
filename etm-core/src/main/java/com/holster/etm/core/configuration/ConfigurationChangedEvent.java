package com.holster.etm.core.configuration;

import java.util.Properties;

public class ConfigurationChangedEvent {

	private Properties oldProperties;
	private Properties currentProperties;
	private Properties addedProperties;
	private Properties removedProperties;
	private Properties changedProperties;
}
