package com.jecstar.etm.core.domain;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class EtmPrincipal implements Principal {
	
	private final String id;
	private final String passwordHash;
	
	private String name = null;
	private Locale locale = Locale.getDefault();
	private Set<String> roles = new HashSet<String>();
	private TimeZone timeZone = TimeZone.getDefault();


	public EtmPrincipal(String id, String passwordHash) {
		if (id == null || passwordHash == null) {
			throw new NullPointerException("id: '" + id + "', passwordHash: '" + passwordHash + "'");
		}
		this.id = id;
		this.passwordHash = passwordHash;
	}

	@Override
	public String getName() {
		return this.name == null ? this.id : this.name;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getPasswordHash() {
		return this.passwordHash;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Locale getLocale() {
		return this.locale;
	}
	
	public void setLocale(Locale locale) {
		if (locale == null) {
			return;
		}
		this.locale = locale;
	}
	
	public TimeZone getTimeZone() {
		return this.timeZone;
	}
	
	public void setTimeZone(TimeZone timeZone) {
		if (timeZone == null) {
			return;
		}
		this.timeZone = timeZone;
	}
	
	public Set<String> getRoles() {
		return Collections.unmodifiableSet(this.roles);
	}
	
	public void addRole(String role) {
		if (role != null) {
			this.roles.add(role);
		}
	}
	
	public void addRoles(Collection<String> roles) {
		if (roles == null) {
			return;
		}
		this.roles.addAll(roles);
	}
}
