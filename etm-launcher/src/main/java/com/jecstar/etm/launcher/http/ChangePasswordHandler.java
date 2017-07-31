package com.jecstar.etm.launcher.http;

import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;

import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

class ChangePasswordHandler implements HttpHandler {

	private final HttpHandler next;
	private final String contextRoot;
	
	public ChangePasswordHandler(final String contextRoot, final HttpHandler next) {
		this.next = next;
		this.contextRoot = contextRoot;
	}
	
	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (exchange.getSecurityContext() != null 
				&& Methods.GET.equals(exchange.getRequestMethod()) 
				&& exchange.getRequestPath().endsWith(".html")
				&& !exchange.getRequestPath().endsWith("/preferences/change_password.html")) {
			Account authenticatedAccount = exchange.getSecurityContext().getAuthenticatedAccount();
			if (authenticatedAccount != null && authenticatedAccount.getPrincipal() != null) {
				EtmPrincipal etmPrincipal = (EtmPrincipal) authenticatedAccount.getPrincipal();
				if (etmPrincipal.isChangePasswordOnLogon()) {
			        int ix = exchange.getRequestPath().indexOf(this.contextRoot);
			        if (ix != -1) {
			        	exchange.setStatusCode(StatusCodes.FOUND);
				        exchange.getResponseHeaders().put(Headers.LOCATION, exchange.getRequestPath().substring(0, ix + this.contextRoot.length()) + "preferences/change_password.html");
				        exchange.endExchange();	
			        	return;
				    }
			    }
			}
		}
		next.handleRequest(exchange);
	}

}
