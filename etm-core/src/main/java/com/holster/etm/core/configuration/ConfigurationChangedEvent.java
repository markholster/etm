package com.holster.etm.core.configuration;

import java.util.Properties;

public class ConfigurationChangedEvent {

	Properties oldProperties;
	Properties currentProperties;
	Properties addedProperties;
	Properties removedProperties;
	Properties changedProperties;
}
