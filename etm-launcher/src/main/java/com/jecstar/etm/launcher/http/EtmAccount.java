package com.jecstar.etm.launcher.http;

import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.undertow.security.idm.Account;

public class EtmAccount implements Account {
	
	private final Principal principal;
	private long lastUpdated;
	private Set<String> roles = new HashSet<String>();
	
	EtmAccount(Principal principal) {
		this.principal = principal;
	}

	@Override
	public Principal getPrincipal() {
		return this.principal;
	}

	@Override
	public Set<String> getRoles() {
		return this.roles;
	}
	
	public void setRoles(Collection<String> roles) {
		this.roles.clear();
		this.roles.addAll(roles);
		this.lastUpdated = System.currentTimeMillis();
	}
	
	public long getLastUpdated() {
		return this.lastUpdated;
	}

}
