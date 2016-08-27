package com.jecstar.etm.server.core.domain;

public enum EtmPrincipalRole {

	ADMIN("admin"), 
	SEARCHER("searcher"), 
	CONTROLLER("controller"), 
	PROCESSOR("processor"),
	IIB_ADMIN("iib-admin");
	
	private final String roleName;
	
	private EtmPrincipalRole(String roleName) {
		this.roleName = roleName;
	}
	
	public String getRoleName() {
		return this.roleName;
	}

}
