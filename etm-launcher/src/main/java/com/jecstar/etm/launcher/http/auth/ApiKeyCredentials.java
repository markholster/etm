package com.jecstar.etm.launcher.http.auth;

import io.undertow.security.idm.Credential;

import java.util.Objects;

public class ApiKeyCredentials implements Credential {

    private final String apiKey;

    public ApiKeyCredentials(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ApiKeyCredentials) {
            return Objects.equals(this.apiKey, ((ApiKeyCredentials) obj).apiKey);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.apiKey);
    }
}
