package com.holster.etm.core.configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.data.Stat;

import com.holster.etm.core.EtmException;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

abstract class AbstractConfiguration {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(AbstractConfiguration.class);
	
	public static final String NODE_CONFIGURATION_PATH = "/config";

	private List<ConfigurationChangeListener> configurationChangeListeners = new ArrayList<ConfigurationChangeListener>();
	
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
	
	/**
	 * Gives the configuration parameters of a given node.
	 * 
	 * @param nodeName
	 *            The name of the node.
	 * @return The configuration parameters of the node.
	 */
	Properties getNodeConfiguration(CuratorFramework client, String nodeName, String propertiesName) {
		Properties properties = new Properties();
		try {
			Stat stat = client.checkExists().forPath(NODE_CONFIGURATION_PATH + "/" + propertiesName);
			if (stat != null) {
				properties.putAll(loadPropertiesFromData(client.getData().forPath(NODE_CONFIGURATION_PATH + "/" + propertiesName)));
			}
			if (nodeName != null) {
				stat = client.checkExists().forPath(NODE_CONFIGURATION_PATH + "/" + nodeName + "/" + propertiesName);
				if (stat != null) {
					properties.putAll(loadPropertiesFromData(client.getData().forPath(NODE_CONFIGURATION_PATH + "/" + nodeName + "/" + propertiesName)));
				}
			}
        } catch (Exception e) {
        	throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
		return properties;
	}
	
	void checkDefaultValue(Properties properties, String key, String value) {
		if (!properties.containsKey(key) || properties.getProperty(key) == null) {
			properties.setProperty(key, value);
		}
	}
	
	void addConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		if (!this.configurationChangeListeners.contains(configurationChangeListener)) {
			this.configurationChangeListeners.add(configurationChangeListener);
		}
    }

	void removeConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
	    this.configurationChangeListeners.remove(configurationChangeListener);
    }
	
	List<ConfigurationChangeListener> getConfigurationChangeListeners() {
	    return this.configurationChangeListeners;
    }

}
