package com.jecstar.etm.server.core.parsers;

import java.util.EnumSet;
import java.util.Set;

import com.jayway.jsonpath.Configuration.Defaults;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

class JsonPathDefaults implements Defaults {

	private final JacksonMappingProvider mappingProvider = new JacksonMappingProvider();
	
	@Override
    public JsonProvider jsonProvider() {
	    return new JacksonJsonProvider();
    }

	@Override
    public Set<Option> options() {
		return EnumSet.noneOf(Option.class);
    }

	@Override
    public MappingProvider mappingProvider() {
	    return this.mappingProvider;
    }

}
