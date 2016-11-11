package com.jecstar.etm.server.core.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EtmGroup {

	private String name;
	
	private Set<EtmPrincipalRole> roles = new HashSet<EtmPrincipalRole>();

	private String filterQuery = null;

	private QueryOccurrence filterQueryOccurrence = QueryOccurrence.MUST;
	
	private boolean alwaysShowCorrelatedEvents = false;
	
	public EtmGroup(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getFilterQuery() {
		return this.filterQuery;
	}
	
	public void setFilterQuery(String filterQuery) {
		this.filterQuery = filterQuery;
	}
	
	public QueryOccurrence getFilterQueryOccurrence() {
		return this.filterQueryOccurrence ;
	}
	
	public void setFilterQueryOccurrence(QueryOccurrence filterQueryOccurrence) {
		this.filterQueryOccurrence = filterQueryOccurrence;
	}
	
	public boolean isAlwaysShowCorrelatedEvents() {
		return this.alwaysShowCorrelatedEvents;
	}

	public void setAlwaysShowCorrelatedEvents(boolean alwaysShowCorrelatedEvents) {
		this.alwaysShowCorrelatedEvents = alwaysShowCorrelatedEvents;
	}

	public Set<EtmPrincipalRole> getRoles() {
		return Collections.unmodifiableSet(this.roles);
	}
	
	public void addRole(EtmPrincipalRole role) {
		if (role != null && !this.roles.contains(role)) {
			this.roles.add(role);
		}
	}
	
	public void addRoles(Collection<EtmPrincipalRole> roles) {
		if (roles == null) {
			return;
		}
		for (EtmPrincipalRole role : roles) {
			addRole(role);
		}
	}
	
	public boolean isInRole(EtmPrincipalRole role) {
		return this.roles.contains(role);
	}
	
	public boolean isInAnyRole(EtmPrincipalRole... roles) {
		for (EtmPrincipalRole role : roles) {
			if (this.roles.contains(role)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isInAllRoles(EtmPrincipalRole... roles) {
		for (EtmPrincipalRole role : roles) {
			if (!this.roles.contains(role)) {
				return false;
			}
		}
		return true;		
	}
}
