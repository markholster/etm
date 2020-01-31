package com.jecstar.etm.server.core;

public class Etm {

    private static final String version;

    static {
        version = System.getProperty("app.version");
    }

    public static String getVersion() {
        return version;
    }

    public static boolean hasVersion() {
        return version != null && version.length() > 0;
    }
}
