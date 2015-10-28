package com.jecstar.etm.launcher.converter.yaml;

import com.jecstar.etm.launcher.converter.ConfigurationConverterTags;

public class ConfigurationConverterTagsYamlImpl implements ConfigurationConverterTags {

	@Override
	public String getClusterNameTag() {
		return "cluster.name";
	}
	
	@Override
	public String getNodeNameTag() {
		return "node.name";
	}

	@Override
	public String getBindingAddressTag() {
		return "binding.address";
	}
	
	@Override
	public String getBindingPortOffsetTag() {
		return "binding.port.offset";
	}

	@Override
	public String getHttpPortTag() {
		return "http.port";
	}

	@Override
	public String getDataPathTag() {
		return "path.data";
	}
	
	@Override
	public String getRestEnabledTag() {
		return "rest.enabled";
	}


}
