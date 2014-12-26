package com.holster.etm.jee.configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.holster.etm.processor.EtmException;

@ManagedBean
@Singleton
public class ConfigurationProducer {

	private Properties properties;

	@Produces
	@EtmConfiguration
	public Properties getEtmConfiguration() {
		synchronized (this) {
			if (this.properties == null) {
				InputStream settingsStream = null;
				try {
					File file = new File("etm.properties");
					if (file.exists() && file.canRead()) {
						settingsStream = new FileInputStream(file);
					} else {
						settingsStream = getClass().getResourceAsStream("/etm.properties");
					}
					this.properties = new Properties();
					if (settingsStream != null) {
						this.properties.load(settingsStream);
					}
				} catch (IOException e) {
					throw new EtmException(EtmException.CONFIGURATION_LOAD_EXCEPTION, e);
				} finally {
					if (settingsStream != null) {
						try {
	                        settingsStream.close();
                        } catch (IOException e) {
	                        // TODO logging
                        }
					}
				}
			}
		}
		return this.properties;
	}
}
