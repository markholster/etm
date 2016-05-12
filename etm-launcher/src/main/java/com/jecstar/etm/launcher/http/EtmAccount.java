package com.jecstar.etm.launcher.http;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.jecstar.etm.core.domain.EtmPrincipal;
import com.jecstar.etm.core.domain.EtmPrincipal.PrincipalRole;

import io.undertow.security.idm.Account;

public class EtmAccount implements Account {
	
	private final EtmPrincipal principal;
	private long lastUpdated;
	private Set<PrincipalRole> roles = new HashSet<PrincipalRole>();
	
	EtmAccount(EtmPrincipal principal) {
		this.principal = principal;
	}

	@Override
	public EtmPrincipal getPrincipal() {
		return this.principal;
	}

	@Override
	public Set<String> getRoles() {
		return this.roles.stream().map(r -> r.getRoleName()).collect(Collectors.toSet());
	}
	
	public void setRoles(Collection<PrincipalRole> roles) {
		this.roles.clear();
		if (roles != null) {
			this.roles.addAll(roles);
		}
		this.lastUpdated = System.currentTimeMillis();
	}
	
	public long getLastUpdated() {
		return this.lastUpdated;
	}

}
