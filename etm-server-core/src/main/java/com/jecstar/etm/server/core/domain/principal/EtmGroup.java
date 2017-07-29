package com.jecstar.etm.server.core.domain.principal;

import com.jecstar.etm.server.core.domain.QueryOccurrence;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EtmGroup implements Serializable {
	
	/**
	 * The serialVersionUID for this class. 
	 */
	private static final long serialVersionUID = 7152085459917438053L;
	
	private final String name;
	private final Set<EtmPrincipalRole> roles = new HashSet<>();
	private String filterQuery = null;
	private QueryOccurrence filterQueryOccurrence = QueryOccurrence.MUST;
	private boolean alwaysShowCorrelatedEvents = false;
	private boolean ldapBase;
	
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

	public boolean isLdapBase() {
		return this.ldapBase;
	}
	
	public void setLdapBase(boolean ldapBase) {
		this.ldapBase = ldapBase;
	}
	
	public Set<EtmPrincipalRole> getRoles() {
		return Collections.unmodifiableSet(this.roles);
	}
	
	private void addRole(EtmPrincipalRole role) {
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
	
	@Override
	public boolean equals(Object obj) {
        return obj instanceof EtmGroup && this.name.equals(((EtmGroup) obj).getName());
    }
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
}
