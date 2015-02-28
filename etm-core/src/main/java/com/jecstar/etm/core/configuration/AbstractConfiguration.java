package com.jecstar.etm.core.configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransaction;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.data.Stat;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.core.util.ObjectUtils;

abstract class AbstractConfiguration {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(AbstractConfiguration.class);
	
	static final String NODE_CONFIGURATION_PATH = "/config";

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
	 * 
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
	
	void updateNodeConfiguration(CuratorFramework client, String nodeName, String propertiesName, String[] configurationKeys, Properties defaultValues, Properties valuesToUpdate) {
		// TODO controleren wat er gebeurt met null waardes (bijv cassandra.username).
		List<String> configKeys = Arrays.stream(configurationKeys).collect(Collectors.toList());
		Properties clusterProperties = new Properties();
		try {
			if (nodeName == null) {
				Stat stat = client.checkExists().forPath(NODE_CONFIGURATION_PATH + "/" + propertiesName);
				if (stat != null) {
					clusterProperties.putAll(loadPropertiesFromData(client.getData()
					        .forPath(NODE_CONFIGURATION_PATH + "/" + propertiesName)));
				}
				Map<String, Properties> propertiesToUpdate = new LinkedHashMap<String, Properties>();
				for (Object object : valuesToUpdate.keySet()) {
					String key = object.toString();
					if (!configKeys.contains(key)) {
						continue;
					}
					String clusterValue = clusterProperties.getProperty(key);
					String defaultValue = defaultValues.getProperty(key);
					String updatedValue = valuesToUpdate.getProperty(key);
					if (ObjectUtils.equalsNullProof(updatedValue, defaultValue)) {
						// New value equal to default value.
						if (!ObjectUtils.equalsNullProof(updatedValue, clusterValue) && clusterValue != null) {
							if (clusterProperties.containsKey(key)) {
								clusterProperties.remove(key);
								propertiesToUpdate.put(NODE_CONFIGURATION_PATH + "/" + propertiesName, clusterProperties);
								// Remove the value from the child nodes if the value is the same as the default value.
								List<String> children = client.getChildren().forPath(NODE_CONFIGURATION_PATH);
								for (String childNodeName : children) {
									String childNodePath = NODE_CONFIGURATION_PATH + "/" + childNodeName + "/" + propertiesName;
									Properties childProperties = new Properties();
									if (propertiesToUpdate.containsKey(childNodePath)) {
										childProperties = propertiesToUpdate.get(childNodePath);
									} else {
										stat = client.checkExists().forPath(childNodePath);
										if (stat != null) {
											childProperties = loadPropertiesFromData(client.getData().forPath(childNodePath));
										}
									}
									if (ObjectUtils.equalsNullProof(defaultValue, childProperties.get(key))) {
										childProperties.remove(key);
										propertiesToUpdate.put(childNodePath, childProperties);
									}
								}
							}
						}
					} else if (!ObjectUtils.equalsNullProof(updatedValue, clusterValue)) {
						// New value not equal to default value and cluster value, so we have to set the new cluster value. 
						clusterProperties.setProperty(key, updatedValue);
						propertiesToUpdate.put(NODE_CONFIGURATION_PATH + "/" + propertiesName, clusterProperties);
						// Remove the value from the child nodes if the value is the same as the new value.
						List<String> children = client.getChildren().forPath(NODE_CONFIGURATION_PATH);
						for (String childNodeName : children) {
							String childNodePath = NODE_CONFIGURATION_PATH + "/" + childNodeName + "/" + propertiesName;
							Properties childProperties = new Properties();
							if (propertiesToUpdate.containsKey(childNodePath)) {
								childProperties = propertiesToUpdate.get(childNodePath);
							} else {
								stat = client.checkExists().forPath(childNodePath);
								if (stat != null) {
									childProperties = loadPropertiesFromData(client.getData().forPath(childNodePath));
								}
							}
							if (ObjectUtils.equalsNullProof(updatedValue, childProperties.get(key))) {
								childProperties.remove(key);
								propertiesToUpdate.put(childNodePath, childProperties);
							}
						}
					}
				}
				if (propertiesToUpdate.size() != 0) {
					CuratorTransaction transaction = client.inTransaction();
					for (String key : propertiesToUpdate.keySet()) {
						Properties properties = propertiesToUpdate.get(key);
						stat = client.checkExists().forPath(key);
						if (stat != null) {
							if (properties.size() == 0) {
								transaction = transaction.delete().forPath(key).and();
							} else {
								StringWriter writer = new StringWriter();
								properties.store(writer, null);
								transaction = transaction.setData().forPath(key, writer.toString().getBytes()).and();
								writer.close();
							}
						} else {
							StringWriter writer = new StringWriter();
							properties.store(writer, null);
							transaction = transaction.create().forPath(key, writer.toString().getBytes()).and();
							writer.close();
						}					
					}
					((CuratorTransactionFinal)transaction).commit();
				}
			} else {
				Properties nodeProperties = new Properties();
				Stat stat = client.checkExists().forPath(NODE_CONFIGURATION_PATH + "/" + propertiesName);
				if (stat != null) {
					clusterProperties.putAll(loadPropertiesFromData(client.getData()
					        .forPath(NODE_CONFIGURATION_PATH + "/" + propertiesName)));
				}
				stat = client.checkExists().forPath(NODE_CONFIGURATION_PATH + "/" + nodeName + "/" + propertiesName);
				if (stat != null) {
					nodeProperties.putAll(loadPropertiesFromData(client.getData().forPath(
					        NODE_CONFIGURATION_PATH + "/" + nodeName + "/" + propertiesName)));
				}
				boolean updateNodeProperties = false;
				for (Object object : valuesToUpdate.keySet()) {
					String key = object.toString();
					if (!configKeys.contains(key)) {
						continue;
					}
					String clusterValue = clusterProperties.getProperty(key);
					String nodeValue = nodeProperties.getProperty(key);
					String defaultValue = defaultValues.getProperty(key);
					String updatedValue = valuesToUpdate.getProperty(key);
					if (ObjectUtils.equalsNullProof(updatedValue, defaultValue)) {
						// Updated value equal to default value.
						if (!ObjectUtils.equalsNullProof(updatedValue, clusterValue) && clusterValue != null) {
							// Updated value should be the default value. So if the cluster value is different, then the node value should be equal to the default value.
							if (!ObjectUtils.equalsNullProof(updatedValue, nodeValue)) {
								nodeProperties.setProperty(key, updatedValue);
								updateNodeProperties = true;
							}
						} else if (nodeProperties.containsKey(key)) {
							nodeProperties.remove(key);
							updateNodeProperties = true;
						} 
					} else if (ObjectUtils.equalsNullProof(updatedValue, clusterValue)) {
						// New value equal to cluster value. Remove from node specific properties if present.
						if (nodeProperties.containsKey(key)) {
							nodeProperties.remove(key);
							updateNodeProperties = true;
						}
					} else if (!ObjectUtils.equalsNullProof(updatedValue, nodeValue)) {
						// New value not equal to default value, cluster value and node value, so we have to set the new specific node value. 
						nodeProperties.setProperty(key, updatedValue);
						updateNodeProperties = true;
					}
				}
				if (updateNodeProperties) {
					stat = client.checkExists().forPath(NODE_CONFIGURATION_PATH + "/" + nodeName + "/" + propertiesName);
					if (stat != null) {
						if (nodeProperties.size() == 0) {
							client.delete().forPath(NODE_CONFIGURATION_PATH + "/" + nodeName + "/" + propertiesName);
						} else {
							StringWriter writer = new StringWriter();
							nodeProperties.store(writer, null);
							client.setData().forPath(NODE_CONFIGURATION_PATH + "/" + nodeName + "/" + propertiesName, writer.toString().getBytes());
							writer.close();
						}
					} else {
						StringWriter writer = new StringWriter();
						nodeProperties.store(writer, null);
						client.create().forPath(NODE_CONFIGURATION_PATH + "/" + nodeName + "/" + propertiesName, writer.toString().getBytes());
						writer.close();
					}
				}
			}
		} catch (Exception e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
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
