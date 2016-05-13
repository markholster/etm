package com.jecstar.etm.server.core.domain.converter;

import com.jecstar.etm.server.core.domain.EtmPrincipal;

public interface EtmPrincipalConverter<T> {

	T write(EtmPrincipal etmPrincipal);
	EtmPrincipal read(T content);
	
	EtmPrincipalTags getTags();
}
