package com.jecstar.etm.server.core.domain;

import java.security.Principal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class EtmPrincipal implements Principal {
	
	public static final int DEFAULT_HISTORY_SIZE = 5;
	
	private final String id;
	private String passwordHash;
	
	private String name = null;
	private Locale locale = Locale.getDefault();
	private Set<EtmPrincipalRole> roles = new HashSet<EtmPrincipalRole>();
	private Set<EtmGroup> groups = new HashSet<EtmGroup>();
	private TimeZone timeZone = TimeZone.getDefault();
	private String filterQuery = null;
	private QueryOccurrence filterQueryOccurrence = QueryOccurrence.MUST;
	private boolean alwaysShowCorrelatedEvents = false;
	private int historySize = DEFAULT_HISTORY_SIZE;

	
	// State properties. DO NOT CHANGE!!
	public boolean forceReload = false;

	
	public EtmPrincipal(String id) {
		this.id = id;
	}
	
	public EtmPrincipal(String id, String passwordHash) {
		this(id);
		setPasswordHash(passwordHash);
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

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
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
	
	public Set<EtmGroup> getGroups() {
		return this.groups;
	}
	
	public void addGroup(EtmGroup group) {
		if (group != null && !this.groups.contains(group)) {
			this.groups.add(group);
		}
	}
	
	public void addGroups(Collection<EtmGroup> groups) {
		if (groups== null) {
			return;
		}
		for (EtmGroup group : groups) {
			addGroup(group);
		}
	}
	
	public boolean isInRole(EtmPrincipalRole role) {
		if (this.roles.contains(role)) {
			return true;
		}
		for (EtmGroup group : this.groups) {
			if (group.isInRole(role)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isInAnyRole(EtmPrincipalRole... roles) {
		for (EtmPrincipalRole role : roles) {
			if (this.roles.contains(role)) {
				return true;
			}
		}
		for (EtmGroup group : this.groups) {
			if (group.isInAnyRole(roles)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isInGroup(String groupName) {
		for (EtmGroup group : this.groups) {
			if (group.getName().equals(groupName)) {
				return true;
			}
		}
		return false;
	}
	
	public String getFilterQuery() {
		return this.filterQuery;
	}
	
	public void setFilterQuery(String filterQuery) {
		this.filterQuery = filterQuery;
	}
	
	public QueryOccurrence getFilterQueryOccurrence() {
		return this.filterQueryOccurrence;
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
	
	public int getHistorySize() {
		return this.historySize;
	}
	
	public void setHistorySize(int historySize) {
		if (historySize < 0) {
			this.historySize =  0;
		}
		this.historySize = historySize;
	}
	
	public NumberFormat getNumberFormat() {
		return NumberFormat.getInstance(getLocale());
	}
	
	public DateFormat getISO8601DateFormat() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", getLocale());
		dateFormat.setTimeZone(getTimeZone());
		return dateFormat;
	}
	
	public DateFormat getDateFormat(String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format, getLocale());
		dateFormat.setTimeZone(getTimeZone());
		return dateFormat;
	}
}
