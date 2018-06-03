package com.jecstar.etm.gui.rest;

import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.rest.AbstractJsonService;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

public abstract class AbstractGuiService extends AbstractJsonService {

    @Context
    protected SecurityContext securityContext;

    private final JsonConverter jsonConverter = new JsonConverter();

    protected EtmPrincipal getEtmPrincipal() {
        return (EtmPrincipal) this.securityContext.getUserPrincipal();
    }


}
