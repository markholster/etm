package com.jecstar.etm.launcher.background;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;

import com.jecstar.etm.server.core.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.configuration.License;
import com.jecstar.etm.server.core.configuration.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class LicenseUpdater implements Runnable {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(LicenseUpdater.class);

	
	private final EtmConfiguration etmConfiguration;
	private final Client client;
	private final JsonConverter converter = new JsonConverter();
	private final EtmConfigurationConverterJsonImpl etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();

	public LicenseUpdater(final EtmConfiguration etmConfiguration, final Client client) {
		this.etmConfiguration = etmConfiguration;
		this.client = client;
	}

	@Override
	public void run() {
		License license = etmConfiguration.getLicense();
		if (license == null || (license.getOwner().equals("Jecstar Free License") && license.getExpiryDate().isBefore(Instant.now().plus(18, ChronoUnit.HOURS)))) {
			String licenseKey = downloadLicenseKey();
			if (licenseKey != null) {
				try {
					etmConfiguration.setLicenseKey(licenseKey);
					Map<String, Object> values = new HashMap<>();
					values.put(this.etmConfigurationConverter.getTags().getLicenseTag(), licenseKey);
					client.prepareUpdate(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_LICENSE, ElasticsearchLayout.CONFIGURATION_INDEX_TYPE_LICENSE_ID)
						.setDoc(values)
						.setDocAsUpsert(true)
						.setWaitForActiveShards(getActiveShardCount(etmConfiguration))
						.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
						.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
						.get();
					// Because the access to the etmConfiguration in the above statement could cause a reload of the configuration 
					// the old license may still be applied. To prevent this, we set the license again at this place.
					etmConfiguration.setLicenseKey(licenseKey);
				} catch (Exception e) {
					if (log.isDebugLevelEnabled()) {
						log.logDebugMessage("Unable to update license automatically.", e);
					}
				}
			}
		}
	}
	
	private String downloadLicenseKey() {
		BufferedReader in = null;
		try {
			URL url = new URL("https://www.jecstar.com/rest/license/etm/free/Jecstar%20Free%20License");
			in = new BufferedReader(new InputStreamReader(url.openStream()));

			String inputLine;
			StringBuilder result = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				result.append(inputLine);
			}
			Map<String, Object> valueMap = this.converter.toMap(result.toString());
			return this.converter.getString("key", valueMap);
		} catch (IOException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Unable to retrieve free license.", e);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}
	
    protected ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
    	if (-1 == etmConfiguration.getWaitForActiveShards()) {
    		return ActiveShardCount.ALL;
    	} else if (0 == etmConfiguration.getWaitForActiveShards()) {
    		return ActiveShardCount.NONE;
    	}
    	return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }


}
