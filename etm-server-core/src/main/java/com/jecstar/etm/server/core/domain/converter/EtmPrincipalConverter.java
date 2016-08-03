package com.jecstar.etm.server.core.domain.converter;

import com.jecstar.etm.server.core.domain.EtmGroup;
import com.jecstar.etm.server.core.domain.EtmPrincipal;

public interface EtmPrincipalConverter<T> {

	T writePrincipal(EtmPrincipal etmPrincipal);
	EtmPrincipal readPrincipal(T content);
	
	T writeGroup(EtmGroup etmGroup);
	EtmGroup readGroup(T contenr);
	
	EtmPrincipalTags getTags();
}
