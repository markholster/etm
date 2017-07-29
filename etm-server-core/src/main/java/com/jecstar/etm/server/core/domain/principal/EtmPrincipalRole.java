package com.jecstar.etm.server.core.domain.principal;

public enum EtmPrincipalRole {

	ADMIN("admin"), 
	SEARCHER("searcher"), 
	CONTROLLER("controller"), 
	PROCESSOR("processor"),
	IIB_ADMIN("iib-admin");
	
	private final String roleName;
	
	EtmPrincipalRole(String roleName) {
		this.roleName = roleName;
	}
	
	public String getRoleName() {
		return this.roleName;
	}
	
	public static EtmPrincipalRole fromRoleName(String roleName) {
		for (EtmPrincipalRole role : EtmPrincipalRole.values()) {
			if (role.getRoleName().equals(roleName)) {
				return role;
			}
		}
		return null;
 	}

}
