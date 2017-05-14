package com.jecstar.etm.launcher.http.session;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;

import io.undertow.server.session.SessionConfig;

public interface ElasticsearchSessionConverter<T> {

	ElasticsearchSession read(T content, EtmConfiguration etmConfiguration, ElasticsearchSessionManager sessionManager, SessionConfig sessionConfig);
	T write(ElasticsearchSession elasticsearchSession);
	
	ElasticsearchSessionTags getTags();

}
