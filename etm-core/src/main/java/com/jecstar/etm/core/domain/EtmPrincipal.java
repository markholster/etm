package com.jecstar.etm.core.domain;

import java.security.Principal;
import java.util.Locale;
import java.util.TimeZone;

public class EtmPrincipal implements Principal {
	
	private final String id;
	
	private final Locale locale = Locale.getDefault();
	
	private final TimeZone timeZone = TimeZone.getDefault();

	public EtmPrincipal(String id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return this.id;
	}
	
	public Locale getLocale() {
		return this.locale;
	}
	
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

}
