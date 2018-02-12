package com.jecstar.etm.domain;

public enum PayloadEncoding {

    BASE64,
    BASE64_CA_API_GATEWAY;

    public static PayloadEncoding safeValueOf(String value) {
        if (value == null) {
            return null;
        }
        if ("base64CaApiGateway".equalsIgnoreCase(value)) {
            // Support for an old value that wasn't in this ENUM when published.
            return BASE64_CA_API_GATEWAY;
        }
        try {
            return PayloadEncoding.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
