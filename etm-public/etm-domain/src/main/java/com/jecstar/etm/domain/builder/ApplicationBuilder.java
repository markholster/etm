package com.jecstar.etm.domain.builder;

import com.jecstar.etm.domain.Application;

import java.net.InetAddress;

public class ApplicationBuilder {

	private final Application application;
	
	public ApplicationBuilder() {
		this.application = new Application();
	}
	
	public Application build() {
		return this.application;
	}
	
	public ApplicationBuilder setName(String name) {
		this.application.name = name;
		return this;
	}
	
	public ApplicationBuilder setHostAddress(InetAddress hostAddress) {
		this.application.hostAddress = hostAddress;
		return this;
	}
	
	public ApplicationBuilder setInstance(String instance) {
		this.application.instance = instance;
		return this;
	}
	
	public ApplicationBuilder setPrincipal(String principal) {
		this.application.principal = principal;
		return this;
	}
	
	public ApplicationBuilder setVersion(String version) {
		this.application.version = version;
		return this;
	}
	
}
