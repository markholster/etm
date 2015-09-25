package com.jecstar.etm.core.configuration;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

//TODO document this class and the different properties. 
//TODO fallback to default enum values for proprties with illegal values. 
public class EtmConfiguration {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(EtmConfiguration.class);

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

	public EtmConfiguration(String nodeName, String component) {
		this.nodeName = nodeName;
		this.component = component;
	}

	// Etm license configuration

	public License getLicense() {
		return this.license;
	}

	// Etm processor configuration

	public int getEnhancingHandlerCount() {
		return this.enhancingHandlerCount;
	}

	public int getPersistingHandlerCount() {
		return this.persistingHandlerCount;
	}

	public int getEventBufferSize() {
		return this.eventBufferSize;
	}

	// Etm persisting configuration.
	public int getPersistingBulkSize() {
		return this.persistingBulkSize;
	}
	
	public int getShardsPerIndex() {
		return this.shardsPerIndex;
	}

	public int getReplicasPerIndex() {
		return this.replicasPerIndex;
	}

	public void setLicenseKey(String licenseKey) {
		this.license = new License(licenseKey);
		// TODO Controleren of de huidige versie een trial is en de nieuwe ook.
		// Als dat zo is, dan de key afwijzen?
	}

	public String getNodeName() {
		return this.nodeName;
	}
}
