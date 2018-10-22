package com.jecstar.etm.server.core.domain.configuration;

public final class EtmSnmpConstants {

    public static final int JECSTAR_PEN = 52111;
    public static final String ENTERPRISE_OID = "1.3.6.1.4.1." + JECSTAR_PEN;
    public static final String ETM_OID = ENTERPRISE_OID + ".1";
    public static final String ETM_OBJECTS_OID = ETM_OID + ".3";


    public static final String ETM_NOTIFOCATION_OID = ETM_OBJECTS_OID + ".1";
    public static final String ETM_GENERAL_NOTIFICATION_OID = ETM_NOTIFOCATION_OID + ".1";
    public static final String ETM_SIGNAL_NOTIFICATION_OID = ETM_NOTIFOCATION_OID + ".2";

    // Signal Trap oid suffixes.
    public static final String ETM_SIGNAL_NOTIFICATION_CLUSTER_NAME_SUFFIX = ".1";
    public static final String ETM_SIGNAL_NOTIFICATION_NAME_SUFFIX = ".2";
    public static final String ETM_SIGNAL_NOTIFICATION_THRESHOLD_SUFFIX = ".3";
    public static final String ETM_SIGNAL_NOTIFICATION_LIMIT_SUFFIX = ".4";
    public static final String ETM_SIGNAL_NOTIFICATION_COUNT_SUFFIX = ".5";
}
