package com.jecstar.etm.core.domain.converter;

import com.jecstar.etm.domain.EtmPrincipal;

public interface EtmPrincipalConverter<T> {

	T convert(EtmPrincipal etmPrincipal);
	EtmPrincipal convert(T content);
	
	EtmPrincipalConverterTags getTags();
}
