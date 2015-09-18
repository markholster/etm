package com.jecstar.etm.core.configuration;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.elasticsearch.client.Client;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

//TODO document this class and the different properties. 
//TODO fallback to default enum values for proprties with illegal values. 
//TODO Zookeeper authentication
public class EtmConfiguration {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(EtmConfiguration.class);

	private static final String PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtBCraSZNjqfqnDK/ESEqwZWZiDY6YRe72N8id//B5LHv7eO41cgRrKzAIn+WH10C3jOjGpJjF1RITKTJg1FM4CK+L66hYP3HQVX8ghtQT99TkHuTkTGxbbBMZd4VF77TR5mTa0LjMTGz7+r9q0PAQEGPol/WqaOTxGHiizh7/qmA0hvAA4Ff39T0CsFyWFpI4hmfS5JG/sLsG8WKd125A1VJFk76ZH7kWP1ysrzGzbR1vSQznQpzz7GPpbzFgjDWJpvQzLREv7qSn1z7MGD4YKlLpgaYxoPUsF2kg4N3YzvZw+RfMTFS2v689VmLccZbySXSoqXyssSq6oMlXIwDSus5qFaB1TeYFJWHZh/t6QHHYeyI0RW6pzIAAG/yGF9uX13uiIb9J9+Qu02XAPstl0ZsVfAVdbzV1AKFMPVOCzMHk6T8YcLsFKedigeH4K2vzdQyHC4L0oZ+2xYiDp904Y7A20HfTyBhVJmz7OIKLjJbnuCh8wP1g9VAR9NC468/nhEdCBxT2nHvvJMLzw2xUBYNIoSw5rWd5+nO9kiCWD7OoNpL5nTRRlX3jBpuqEJmszQo3wF0jZEqAi/pYn3c60iEljtx8m8K8EgjylS/C49qBDUfCQnwfNQxGjxzEzeFc9+mJRox87kxYMUsCyT5u46f8P1wfHOWxzubRgcr0hECAwEAAQ==";

	public static final String ETM_ENHANCING_HANDLER_COUNT = "etm.enhancing_handler_count";
	public static final String ETM_INDEXING_HANDLER_COUNT = "etm.indexing_handler_count";
	public static final String ETM_PERSISTING_HANDLER_COUNT = "etm.persisting_handler_count";
	public static final String ETM_RINGBUFFER_SIZE = "etm.ringbuffer_size";
	public static final String ETM_PERSISTING_BULK_SIZE = "etm.persisting_bulk_size";
	public static final String ETM_PERSISTING_SHARDS_PER_INDEX = "etm.persisting_shards_per_index";
	public static final String ETM_PERSISTING_REPLICAS_PER_INDEX = "etm.persisting_replicas_per_index";
	public static final String ETM_ENDPOINT_CACHE_EXPIRY_TIME = "etm.endpoint_cache_expiry_time";
	public static final String ETM_DATA_CORRELATION_MAX_MATCHES = "etm.data_correlation_max_matches";
	public static final String ETM_DATA_CORRELATION_TIME_OFFSET = "etm.data_correlation_time_offset";
	public static final String ETM_DATA_RETENTION_TIME = "etm.data_retention_time";
	public static final String ETM_DATA_RETENTION_CHECK_INTERVAL = "etm.data_retention_check_interval";
	public static final String ETM_DATA_RETENTION_PRESERVE_EVENT_COUNTS = "etm.data_retention_preserve_event_counts";
	public static final String ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES = "etm.data_retention_preserve_event_performances";
	public static final String ETM_DATA_RETENTION_PRESERVE_EVENT_SLAS = "etm.data_retention_preserve_transaction_slas";

	private static final String[] CONFIGURATION_KEYS = new String[] { ETM_ENHANCING_HANDLER_COUNT, ETM_INDEXING_HANDLER_COUNT,
	        ETM_PERSISTING_HANDLER_COUNT, ETM_RINGBUFFER_SIZE, ETM_PERSISTING_BULK_SIZE, ETM_ENDPOINT_CACHE_EXPIRY_TIME, ETM_DATA_CORRELATION_MAX_MATCHES,
	        ETM_DATA_CORRELATION_TIME_OFFSET, ETM_DATA_RETENTION_TIME, ETM_DATA_RETENTION_CHECK_INTERVAL,
	        ETM_DATA_RETENTION_PRESERVE_EVENT_COUNTS, ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES,
	        ETM_DATA_RETENTION_PRESERVE_EVENT_SLAS };

	private List<ConfigurationChangeListener> configurationChangeListeners = new ArrayList<ConfigurationChangeListener>();
	
	private final String nodeName;

	private Properties etmProperties;

	private String companyName;
	private Date licenseExpiry;
	private LicenseType licenseType;

	private Client elasticClient;

	public EtmConfiguration(String nodeName, Client elasticClient, String component) {
		this.nodeName = nodeName;
		this.elasticClient = elasticClient;
		this.etmProperties = loadEtmProperties();
	}

	private Properties loadEtmProperties() {
		Properties properties = new Properties();
		fillDefaults(properties);
		return properties;
	}

	private String[] decodeLicenseData(byte[] data) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY));
			PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

			Cipher decrpyptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			decrpyptCipher.init(Cipher.DECRYPT_MODE, publicKey);

			byte[] decryptedBytes = decrpyptCipher.doFinal(Base64.getDecoder().decode(data));
			String license = new String(decryptedBytes);
			String[] split = license.split(":");
			if (split.length != 3) {
				throw new EtmException(EtmException.INVALID_LICENSE_KEY_EXCEPTION);
			}
			return split;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException
		        | IllegalBlockSizeException | BadPaddingException e) {
			throw new EtmException(EtmException.INVALID_LICENSE_KEY_EXCEPTION, e);
		}
	}

	private void fillDefaults(Properties properties) {
		checkDefaultValue(properties, ETM_ENHANCING_HANDLER_COUNT, "5");
		checkDefaultValue(properties, ETM_INDEXING_HANDLER_COUNT, "5");
		checkDefaultValue(properties, ETM_PERSISTING_HANDLER_COUNT, "5");
		checkDefaultValue(properties, ETM_RINGBUFFER_SIZE, "4096");
		checkDefaultValue(properties, ETM_PERSISTING_BULK_SIZE, "50");
		checkDefaultValue(properties, ETM_PERSISTING_SHARDS_PER_INDEX, "2");
		checkDefaultValue(properties, ETM_PERSISTING_REPLICAS_PER_INDEX, "1");
		checkDefaultValue(properties, ETM_ENDPOINT_CACHE_EXPIRY_TIME, "60000");
		checkDefaultValue(properties, ETM_DATA_CORRELATION_MAX_MATCHES, "100");
		checkDefaultValue(properties, ETM_DATA_CORRELATION_TIME_OFFSET, "30000");
		checkDefaultValue(properties, ETM_DATA_RETENTION_TIME, "604800000");
		checkDefaultValue(properties, ETM_DATA_RETENTION_CHECK_INTERVAL, "60000");
		checkDefaultValue(properties, ETM_DATA_RETENTION_PRESERVE_EVENT_COUNTS, "false");
		checkDefaultValue(properties, ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES, "false");
		checkDefaultValue(properties, ETM_DATA_RETENTION_PRESERVE_EVENT_SLAS, "false");
	}

	// Etm license configuration

	public String getCompanyName() {
		return this.companyName;
	}

	public Date getLicenseExpriy() {
		return this.licenseExpiry;
	}

	// Etm processor configuration

	public int getEnhancingHandlerCount() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_ENHANCING_HANDLER_COUNT));
	}

	public int getIndexingHandlerCount() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_INDEXING_HANDLER_COUNT));
	}

	public int getPersistingHandlerCount() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_PERSISTING_HANDLER_COUNT));
	}

	public int getRingbufferSize() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_RINGBUFFER_SIZE));
	}

	// Etm persistng configuration.
	public int getPersistingBulkSize() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_PERSISTING_BULK_SIZE));
	}
	
	public int getPersistingShardsPerIndex() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_PERSISTING_SHARDS_PER_INDEX));
	}

	public int getPersistingReplicasPerIndex() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_PERSISTING_REPLICAS_PER_INDEX));
	}
	
	public long getEndpointCacheExpiryTime() {
		return Long.valueOf(this.etmProperties.getProperty(ETM_ENDPOINT_CACHE_EXPIRY_TIME));
	}

	// Etm data correlation configuration.

	public int getDataCorrelationMax() {
		return Integer.valueOf(this.etmProperties.getProperty(ETM_DATA_CORRELATION_MAX_MATCHES));
	}

	public long getDataCorrelationTimeOffset() {
		return Long.valueOf(this.etmProperties.getProperty(ETM_DATA_CORRELATION_TIME_OFFSET));
	}

	// Etm data retention configuration

	public long getDataRetentionTime() {
		return Long.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_TIME));
	}

	public long getDataRetentionCheckInterval() {
		return Long.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_CHECK_INTERVAL));
	}

	public boolean isDataRetentionPreserveEventCounts() {
		return Boolean.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_PRESERVE_EVENT_COUNTS));
	}

	public boolean isDataRetentionPreserveEventPerformances() {
		return Boolean.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_PRESERVE_EVENT_PERFORMANCES));
	}

	public boolean isDataRetentionPreserveEventSlas() {
		return Boolean.valueOf(this.etmProperties.getProperty(ETM_DATA_RETENTION_PRESERVE_EVENT_SLAS));
	}

	public void addEtmConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		addConfigurationChangeListener(configurationChangeListener);
	}

	public void removeEtmConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		removeConfigurationChangeListener(configurationChangeListener);
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


	/**
	 * Returns a list with nodes that have connected to the ETM cluster at least
	 * one time.
	 * 
	 * @return A list with nodes.
	 */
	public List<Node> getNodes() {
		return new ArrayList<Node>();
	}

	/**
	 * Gives a list with nodes that are connected to the ETM cluster at this
	 * moment.
	 * 
	 * @return A list with live nodes.
	 */
	public List<String> getLiveNodes() {
		return new ArrayList<String>();
	}

	/**
	 * Gives the configuration parameters of a given node.
	 * 
	 * @param nodeName
	 *            The name of the node.
	 * @return The configuration parameters of the node.
	 */
	public Properties getNodeConfiguration(String nodeName) {
		Properties properties = getNodeConfiguration(this.elasticClient, nodeName, "etm.properties");
		fillDefaults(properties);
		return properties;
	}

	private Properties getNodeConfiguration(Client elasticClient, String nodeName, String string) {
	    return new Properties();
    }

	public void update(String nodeName, Properties properties) {
		Properties defaultValues = new Properties();
		fillDefaults(defaultValues);
		updateNodeConfiguration(this.elasticClient, nodeName, "etm.properties", CONFIGURATION_KEYS, defaultValues, properties);
	}

	private void updateNodeConfiguration(Client elasticClient, String nodeName, String string, String[] configurationKeys, Properties defaultValues, Properties properties) {
	    this.etmProperties.putAll(properties);
    }

	public void setLicenseKey(String licenseKey) {
		// Check if the key is valid, by decoding the String.
		decodeLicenseData(licenseKey.getBytes());
		// TODO Controleren of de huidige versie een trial is en de nieuwe ook.
		// Als dat zo is, dan de key afwijzen?
		try {
		} catch (Exception e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	private enum LicenseType {
		TRIAL, SUBSCRIPTION
	}

	public String getNodeName() {
		return this.nodeName;
	}
}
