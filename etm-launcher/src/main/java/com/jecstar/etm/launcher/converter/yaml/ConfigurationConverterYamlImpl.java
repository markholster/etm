package com.jecstar.etm.launcher.converter.yaml;

import java.util.Map;

import com.jecstar.etm.launcher.Configuration;
import com.jecstar.etm.launcher.converter.ConfigurationConverter;
import com.jecstar.etm.launcher.converter.ConfigurationConverterTags;

public class ConfigurationConverterYamlImpl implements ConfigurationConverter<Map<String, Object>>{
	
	private ConfigurationConverterTags tags = new ConfigurationConverterTagsYamlImpl();
	
	@Override
	public Configuration convert(Map<String, Object> content) {
		Configuration configuration = new Configuration();
		if (content.containsKey(this.tags.getClusterNameTag())) {
			configuration.clusterName = content.get(this.tags.getClusterNameTag()).toString();
		}
		if (content.containsKey(this.tags.getNodeNameTag())) {
			configuration.nodeName = content.get(this.tags.getNodeNameTag()).toString();
		}
		if (content.containsKey(this.tags.getBindingAddressTag())) {
			configuration.bindingAddress = content.get(this.tags.getBindingAddressTag()).toString();
		}
		if (content.containsKey(this.tags.getBindingPortOffsetTag())) {
			configuration.bindingPortOffset = Integer.valueOf(content.get(this.tags.getBindingPortOffsetTag()).toString());
		}
		if (content.containsKey(this.tags.getHttpPortTag())) {
			configuration.httpPort = Integer.valueOf(content.get(this.tags.getHttpPortTag()).toString());
		}
		if (content.containsKey(this.tags.getRestEnabledTag())) {
			configuration.restEnabled = Boolean.valueOf(content.get(this.tags.getRestEnabledTag()).toString());
		}
		return configuration;
	}

	@Override
	public Map<String, Object> convert(Configuration configuration) {
		return null;
	}

	@Override
	public ConfigurationConverterTags getTags() {
		return null;
	}

}
