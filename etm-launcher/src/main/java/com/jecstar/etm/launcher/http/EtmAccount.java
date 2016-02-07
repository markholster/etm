package com.jecstar.etm.launcher.http;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.jecstar.etm.core.domain.EtmPrincipal;

import io.undertow.security.idm.Account;

public class EtmAccount implements Account {
	
	private final EtmPrincipal principal;
	private long lastUpdated;
	private Set<String> roles = new HashSet<String>();
	
	EtmAccount(EtmPrincipal principal) {
		this.principal = principal;
	}

	@Override
	public EtmPrincipal getPrincipal() {
		return this.principal;
	}

	@Override
	public Set<String> getRoles() {
		return this.roles;
	}
	
	public void setRoles(Collection<String> roles) {
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
