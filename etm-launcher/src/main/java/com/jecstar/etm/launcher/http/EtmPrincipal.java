package com.jecstar.etm.launcher.http;

import java.security.Principal;

public class EtmPrincipal implements Principal {
	
	private final String name;

	public EtmPrincipal(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
