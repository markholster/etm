package com.jecstar.etm.launcher.http;

import java.util.HashSet;
import java.util.Set;

import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipalRole;

import io.undertow.security.idm.Account;

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
		for (EtmPrincipalRole role : this.principal.getRoles()) {
			roles.add(role.getRoleName());
		}
		for (EtmGroup group : this.principal.getGroups()) {
			for (EtmPrincipalRole role : group.getRoles()) {
				roles.add(role.getRoleName());
			}			
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
