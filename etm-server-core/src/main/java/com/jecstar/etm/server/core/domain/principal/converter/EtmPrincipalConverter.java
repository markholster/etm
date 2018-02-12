package com.jecstar.etm.server.core.domain.principal.converter;

import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;

public interface EtmPrincipalConverter<T> {

    T writePrincipal(EtmPrincipal etmPrincipal);

    EtmPrincipal readPrincipal(T content);

    T writeGroup(EtmGroup etmGroup);

    EtmGroup readGroup(T content);

    EtmPrincipalTags getTags();
}
