package com.jecstar.etm.launcher.http.auth;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormParserFactory;

import java.util.Map;

import static io.undertow.UndertowMessages.MESSAGES;
import static io.undertow.security.api.AuthenticationMechanism.AuthenticationMechanismOutcome.NOT_ATTEMPTED;

public class ApiKeyAuthenticationMechanism implements AuthenticationMechanism {

    public static final AuthenticationMechanismFactory FACTORY = new Factory();

    public static final String NAME = "API_KEY";
    public static final String API_KEY_HEADER = "apikey";
    private final String mechanismName;
    private final IdentityManager identityManager;

    public ApiKeyAuthenticationMechanism(String mechanismName, IdentityManager identityManager) {
        this.mechanismName = mechanismName;
        this.identityManager = identityManager;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        String apiKey = exchange.getRequestHeaders().getFirst(API_KEY_HEADER);
        if (apiKey == null || apiKey.trim().length() == 0) {
            return NOT_ATTEMPTED;
        }
        Account account = this.identityManager.verify(new ApiKeyCredentials(apiKey));
        if (account == null) {
            securityContext.authenticationFailed(MESSAGES.authenticationFailed(apiKey), this.mechanismName);
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
        securityContext.authenticationComplete(account, this.mechanismName, false);
        return AuthenticationMechanismOutcome.AUTHENTICATED;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        return ChallengeResult.NOT_SENT;
    }

    public static class Factory implements AuthenticationMechanismFactory {


        public Factory() {

        }

        @Override
        public AuthenticationMechanism create(String mechanismName, IdentityManager identityManager, FormParserFactory formParserFactory, Map<String, String> properties) {
            return new ApiKeyAuthenticationMechanism(mechanismName, identityManager);
        }
    }
}
