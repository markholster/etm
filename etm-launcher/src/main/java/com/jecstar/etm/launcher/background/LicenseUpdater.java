/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.launcher.background;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.configuration.License;
import com.jecstar.etm.server.core.domain.configuration.converter.json.EtmConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.UpdateRequestBuilder;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class LicenseUpdater implements Runnable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(LicenseUpdater.class);


    private final EtmConfiguration etmConfiguration;
    private final DataRepository dataRepository;
    private final RequestEnhancer requestEnhancer;
    private final EtmConfigurationConverterJsonImpl etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
    private final String licenseUpdateUrl;
    private final JsonConverter jsonConverter = new JsonConverter();

    public LicenseUpdater(final EtmConfiguration etmConfiguration, final DataRepository dataRepository, final String licenseUpdateUrl) {
        this.etmConfiguration = etmConfiguration;
        this.dataRepository = dataRepository;
        this.requestEnhancer = new RequestEnhancer(etmConfiguration);
        this.licenseUpdateUrl = licenseUpdateUrl != null ? licenseUpdateUrl : "https://www.jecstar.com/rest/license/v3/etm/free/Jecstar%20Free%20License";
    }

    @Override
    public void run() {
        License license = etmConfiguration.getLicense();
        // No license available OR
        if (license == null || (
                // license is almost expired AND
                license.getExpiryDate().isBefore(Instant.now().plus(18, ChronoUnit.HOURS)) && (
                        // it's an on prem free license OR
                        (license.getOwner().equals("Jecstar Free License") && License.LicenseType.ON_PREM.equals(license.getLicenseType())) ||
                                // it's a cloud license
                                License.LicenseType.CLOUD.equals(license.getLicenseType()))
        )) {
            String licenseKey = downloadLicenseKey();
            if (licenseKey != null) {
                try {
                    etmConfiguration.setLicenseKey(licenseKey);
                    Map<String, Object> values = new HashMap<>();
                    values.put(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE);
                    Map<String, Object> licenseObject = new HashMap<>();
                    licenseObject.put(this.etmConfigurationConverter.getTags().getLicenseTag(), licenseKey);
                    values.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_LICENSE, licenseObject);
                    UpdateRequestBuilder builder = this.requestEnhancer.enhance(
                            new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_ID_LICENSE_DEFAULT)
                    )
                            .setDoc(values)
                            .setDocAsUpsert(true);
                    dataRepository.update(builder);
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
            URL url = new URL(this.licenseUpdateUrl);
            in = new BufferedReader(new InputStreamReader(url.openStream()));

            String inputLine;
            StringBuilder result = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
            }
            Map<String, Object> valueMap = this.jsonConverter.toMap(result.toString());
            return this.jsonConverter.getString("key", valueMap);
        } catch (IOException e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Unable to retrieve license.", e);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    if (log.isDebugLevelEnabled()) {
                        log.logDebugMessage("Unable to close input stream.", e);
                    }
                }
            }
        }
        return null;
    }
}
