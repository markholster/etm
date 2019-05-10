package com.jecstar.etm.server.core.domain.converter;

import com.jecstar.etm.server.core.domain.ImportProfile;

public interface ImportProfileConverter<T> {

    ImportProfile read(T content);

    T write(ImportProfile importProfile);

    ImportProfileTags getTags();
}
