package com.jecstar.etm.core.configuration;

import java.util.ArrayList;
import java.util.List;

import com.jecstar.etm.core.util.ObjectUtils;

//TODO document this class and the different properties. 
public class EtmConfiguration {
	
	public static final String CONFIG_KEY_LICENSE = "license";
	public static final String CONFIG_KEY_ENHANCING_HANDLER_COUNT = "enhancingHandlerCount";
	public static final String CONFIG_KEY_PERSISTING_HANDLER_COUNT = "persistingHandlerCount";
	public static final String CONFIG_KEY_EVENT_BUFFER_SIZE = "eventBufferSize";
	public static final String CONFIG_KEY_PERSISTING_BULK_SIZE = "persistingBulkSize";
	public static final String CONFIG_KEY_SHARDS_PER_INDEX = "shardsPerIndex";
	public static final String CONFIG_KEY_REPLICAS_PER_INDEX = "replicasPerIndex";

	// Disruptor configuration properties.
	private int enhancingHandlerCount = 5;
	private int persistingHandlerCount = 5;
	private int eventBufferSize = 4096;
	
	// Persisting configuration properties;
	private int persistingBulkSize = 50;
	private int shardsPerIndex = 2;
	private int replicasPerIndex = 1;

	private final String nodeName;

	private License license;

	private String component;
	
	private List<ConfigurationChangeListener> changeListeners = new ArrayList<ConfigurationChangeListener>();

	public EtmConfiguration(String nodeName, String component) {
		this.nodeName = nodeName;
		this.component = component;
	}

	// Etm license configuration

	public License getLicense() {
		return this.license;
	}

	public void setLicenseKey(String licenseKey) {
		this.license = new License(licenseKey);
		// TODO Controleren of de huidige versie een trial is en de nieuwe ook.
		// Als dat zo is, dan de key afwijzen?
	}
	
	// Etm processor configuration

	public int getEnhancingHandlerCount() {
		return this.enhancingHandlerCount;
	}
	
	public EtmConfiguration setEnhancingHandlerCount(int enhancingHandlerCount) {
		if (enhancingHandlerCount >= 0) {
			this.enhancingHandlerCount = enhancingHandlerCount;
		}
		return this;
	}

	public int getPersistingHandlerCount() {
		return this.persistingHandlerCount;
	}
	
	public EtmConfiguration setPersistingHandlerCount(int persistingHandlerCount) {
		if (persistingHandlerCount >=0) {
			this.persistingHandlerCount = persistingHandlerCount;
		}
		return this;
	}

	public int getEventBufferSize() {
		return this.eventBufferSize;
	}
	
	public EtmConfiguration setEventBufferSize(int eventBufferSize) {
		if (eventBufferSize > 0) {
			this.eventBufferSize = eventBufferSize;
		}
		return this;
	}

	// Etm persisting configuration.
	
	public int getPersistingBulkSize() {
		return this.persistingBulkSize;
	}
	
	public EtmConfiguration setPersistingBulkSize(int persistingBulkSize) {
		if (persistingBulkSize > 0) {
			this.persistingBulkSize = persistingBulkSize;
		}
		return this;
	}
	
	public int getShardsPerIndex() {
		return this.shardsPerIndex;
	}
	
	public EtmConfiguration setShardsPerIndex(int shardsPerIndex) {
		if (shardsPerIndex > 0) {
			this.shardsPerIndex = shardsPerIndex;
		} 
		return this;
	}

	public int getReplicasPerIndex() {
		return this.replicasPerIndex;
	}
	
	public EtmConfiguration setReplicasPerIndex(int replicasPerIndex) {
		if (replicasPerIndex > 0) {
			this.replicasPerIndex = replicasPerIndex;
		}
		return this;
	}

	public String getNodeName() {
		return this.nodeName;
	}
	
	public String getComponent() {
		return this.component;
	}
	
	public void addConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		if (!this.changeListeners.contains(configurationChangeListener)) {
			this.changeListeners.add(configurationChangeListener);
		}
	}
	
	public void removeConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		this.changeListeners.remove(configurationChangeListener);
	}

	/**
	 * Merge the configuration items from the given
	 * <code>EtmConfiguration</code> into this instance.
	 * <code>ConfigurationChangeListener</code>s are notified in a single event.
	 * 
	 * @param etmConfiguration
	 *            The <code>EtmConfiguration</code> to merge into this instance.
	 * @return <code>true</code> when the configuration is changed,
	 *         <code>false</code> otherwise.
	 */
	public boolean merge(EtmConfiguration etmConfiguration) {
		if (etmConfiguration == null) {
			return false;
		}
		List<String> changed = new ArrayList<String>();
		if (!ObjectUtils.equalsNullProof(this.license, etmConfiguration.getLicense())) {
			this.license = etmConfiguration.getLicense();
			changed.add(CONFIG_KEY_LICENSE);
		}
		if (this.enhancingHandlerCount != etmConfiguration.getEnhancingHandlerCount()) {
			this.enhancingHandlerCount = etmConfiguration.getEnhancingHandlerCount();
			changed.add(CONFIG_KEY_ENHANCING_HANDLER_COUNT);
		}
		if (this.persistingHandlerCount != etmConfiguration.getPersistingHandlerCount()) {
			this.persistingHandlerCount = etmConfiguration.getPersistingHandlerCount();
			changed.add(CONFIG_KEY_PERSISTING_HANDLER_COUNT);
		}
		if (this.eventBufferSize != etmConfiguration.getEventBufferSize()) {
			this.eventBufferSize = etmConfiguration.getEventBufferSize();
			changed.add(CONFIG_KEY_EVENT_BUFFER_SIZE);
		}
		if (this.persistingBulkSize != etmConfiguration.getPersistingBulkSize()) {
			this.persistingBulkSize = etmConfiguration.getPersistingBulkSize();
			changed.add(CONFIG_KEY_PERSISTING_BULK_SIZE);
		}
		if (this.shardsPerIndex != etmConfiguration.getShardsPerIndex()) {
			this.shardsPerIndex = etmConfiguration.getShardsPerIndex();
			changed.add(CONFIG_KEY_SHARDS_PER_INDEX);
		}
		if (this.replicasPerIndex != etmConfiguration.getReplicasPerIndex()) {
			this.replicasPerIndex = etmConfiguration.replicasPerIndex;
			changed.add(CONFIG_KEY_REPLICAS_PER_INDEX);
		}
		if (changed.size() > 0) {
			ConfigurationChangedEvent event = new ConfigurationChangedEvent(changed);
			this.changeListeners.forEach(c -> c.configurationChanged(event));
		}
		return changed.size() > 0;
	}
}
