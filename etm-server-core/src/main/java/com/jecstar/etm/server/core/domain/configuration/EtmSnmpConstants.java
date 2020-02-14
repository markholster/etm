/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
    public static final String ETM_SIGNAL_NOTIFICATION_MAX_FREQUENCY_OF_EXCEEDANCE_SUFFIX = ".4";
    public static final String ETM_SIGNAL_NOTIFICATION_FREQUENCY_OF_EXCEEDANCE_SUFFIX = ".5";
}
