package com.jecstar.etm.launcher.http.session;

public interface ElasticsearchSessionConverter<T> {

	void read(T content, ElasticsearchSession session);
	T write(ElasticsearchSession elasticsearchSession);
	
	ElasticsearchSessionTags getTags();

}
