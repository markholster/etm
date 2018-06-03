package com.jecstar.etm.server.core.converter;

public interface EntityConverter<T, V> {

    T read(V value);

    V write(T entity);
}
