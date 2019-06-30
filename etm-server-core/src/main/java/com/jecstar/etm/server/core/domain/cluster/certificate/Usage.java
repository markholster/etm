package com.jecstar.etm.server.core.domain.cluster.certificate;

public enum Usage {
    LDAP, SMTP;

    public static Usage safeValueOf(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Usage.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}