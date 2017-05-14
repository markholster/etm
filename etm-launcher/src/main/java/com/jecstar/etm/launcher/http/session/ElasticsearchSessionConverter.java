package com.jecstar.etm.launcher.http.session;

import io.undertow.server.session.SessionConfig;

public interface ElasticsearchSessionConverter<T> {

	ElasticsearchSession read(T content, ElasticsearchSessionManager sessionManager, SessionConfig sessionConfig);
	T write(ElasticsearchSession elasticsearchSession);
	
	ElasticsearchSessionTags getTags();

}
