package com.jecstar.etm.launcher.http;

import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import io.undertow.security.idm.Account;

import java.util.HashSet;
import java.util.Set;

class EtmAccount implements Account {

    /**
     * The serialVersionUID for this class.
     */
    private static final long serialVersionUID = -7980565495248385591L;

    private final EtmPrincipal principal;
    private final long lastUpdated;
    private final Set<String> roles = new HashSet<>();

    EtmAccount(EtmPrincipal principal) {
        this.principal = principal;
        this.roles.addAll(principal.getRoles());
        for (EtmGroup group : this.principal.getGroups()) {
            this.roles.addAll(group.getRoles());
        }
        this.lastUpdated = System.currentTimeMillis();
    }

    @Override
    public EtmPrincipal getPrincipal() {
        return this.principal;
    }

    @Override
    public Set<String> getRoles() {
        return this.roles;
    }

    public long getLastUpdated() {
        return this.lastUpdated;
    }

}
