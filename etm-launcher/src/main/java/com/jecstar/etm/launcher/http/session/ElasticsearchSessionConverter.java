package com.jecstar.etm.launcher.http.session;

interface ElasticsearchSessionConverter<T> {

    void read(T content, ElasticsearchSession session);

    T write(ElasticsearchSession elasticsearchSession);

    ElasticsearchSessionTags getTags();

}
