package com.holster.etm.core.configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;

import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

abstract class AbstractConfiguration {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(AbstractConfiguration.class);

	Properties loadProperties(NodeCache nodeCache) {
		Properties properties = new Properties();
		ChildData currentData = nodeCache.getCurrentData();
		if (currentData != null) {
			properties.putAll(loadPropertiesFromData(currentData.getData()));
		}
		return properties;
	}

	private Properties loadPropertiesFromData(byte[] data) {
		Properties properties = new Properties();
		ByteArrayInputStream bis = null;
		try {
			bis = new ByteArrayInputStream(data);
			properties.load(bis);
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Could not load configuration", e);
			}
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					if (log.isDebugLevelEnabled()) {
						log.logDebugMessage("Could not close inputstream to configuration", e);
					}
				}
			}
		}
		return properties;
	}
	
	void checkDefaultValue(Properties properties, String key, String value) {
		if (!properties.containsKey(key) || properties.getProperty(key) == null) {
			properties.setProperty(key, value);
		}
	}

}
