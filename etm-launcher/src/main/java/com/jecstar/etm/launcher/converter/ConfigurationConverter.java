package com.jecstar.etm.launcher.converter;

import com.jecstar.etm.launcher.Configuration;

public interface ConfigurationConverter<T> {

	Configuration convert(T content);
	T convert(Configuration configuration);
	
	ConfigurationConverterTags getTags();
}
