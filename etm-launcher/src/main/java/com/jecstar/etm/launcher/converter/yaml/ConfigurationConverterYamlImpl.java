package com.jecstar.etm.launcher.converter.yaml;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.launcher.Configuration;
import com.jecstar.etm.launcher.YamlConfiguration;
import com.jecstar.etm.launcher.converter.ConfigurationConverter;

public class ConfigurationConverterYamlImpl implements ConfigurationConverter<Map<String, Object>> {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ConfigurationConverterYamlImpl.class);

	
	@Override
	public Configuration convert(Map<String, Object> content) {
		Configuration configuration = new Configuration();
		Field[] fields = Configuration.class.getDeclaredFields();
		for (Field field : fields) {
			YamlConfiguration yamlConfiguration = field.getAnnotation(YamlConfiguration.class);
			if (yamlConfiguration == null) {
				continue;
			}
			String configKey = yamlConfiguration.key();
			if (!content.containsKey(configKey)) {
				continue;
			}
			String configValue = content.get(configKey).toString();
			Class<?> fieldType = field.getType();
			try {
				if (String.class.equals(fieldType)) {
					field.set(configuration, configValue);
				} else if (Integer.class.equals(fieldType) || int.class.equals(fieldType)) {
					field.setInt(configuration, Integer.valueOf(configValue));
				} else if (Boolean.class.equals(fieldType) || boolean.class.equals(fieldType)) {
					field.setBoolean(configuration, Boolean.valueOf(configValue));
				} else if (File.class.equals(fieldType)) {
					field.set(configuration, new File(configValue));
				} else {
					log.logErrorMessage("Error setting configuration value for '" + configKey + "' with value '"
							+ configValue + "' because the type '" + fieldType.getName() + "' is unknown.");
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				log.logErrorMessage("Error setting configuration value for '" + configKey + "' with value '" + configValue + "'", e);
			}
		}
		return configuration;
	}

}
