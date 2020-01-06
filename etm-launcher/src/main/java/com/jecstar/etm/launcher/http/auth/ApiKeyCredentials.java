package com.jecstar.etm.launcher.http.auth;

import io.undertow.security.idm.Credential;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApiKeyCredentials implements Credential {

    private final List<String> apiKeys;

    public ApiKeyCredentials(String apiKeys) {
        this.apiKeys = Arrays.stream(apiKeys.split(",")).collect(Collectors.toList());
    }

    public List<String> getApiKeys() {
        return this.apiKeys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiKeyCredentials)) return false;
        ApiKeyCredentials that = (ApiKeyCredentials) o;
        return Objects.equals(apiKeys, that.apiKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKeys);
    }

}
