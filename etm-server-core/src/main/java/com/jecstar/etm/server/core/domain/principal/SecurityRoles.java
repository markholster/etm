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

package com.jecstar.etm.server.core.domain.principal;

import java.util.Arrays;

/**
 * Utility class that defines all JEE security roles.
 */
public final class SecurityRoles {

    public static final String ETM_EVENT_READ = "etm_event_read";
    public static final String ETM_EVENT_WRITE = "etm_event_write";
    public static final String ETM_EVENT_READ_WRITE = "etm_event_read_write";

    public static final String USER_DASHBOARD_READ_WRITE = "user_dashboard_read_write";
    public static final String GROUP_DASHBOARD_READ = "group_dashboard_read";
    public static final String GROUP_DASHBOARD_READ_WRITE = "group_dashboard_read_write";

    public static final String USER_SIGNAL_READ_WRITE = "user_signal_read_write";
    public static final String GROUP_SIGNAL_READ = "group_signal_read";
    public static final String GROUP_SIGNAL_READ_WRITE = "group_signal_read_write";

    public static final String IIB_NODE_READ = "iib_node_read";
    public static final String IIB_NODE_READ_WRITE = "iib_node_read_write";

    public static final String IIB_EVENT_READ = "iib_event_read";
    public static final String IIB_EVENT_READ_WRITE = "iib_event_read_write";

    public static final String AUDIT_LOG_READ = "audit_log_read";

    public static final String CLUSTER_SETTINGS_READ = "cluster_settings_read";
    public static final String CLUSTER_SETTINGS_READ_WRITE = "cluster_settings_read_write";

    public static final String IMPORT_PROFILES_READ = "import_profiles_read";
    public static final String IMPORT_PROFILES_READ_WRITE = "import_profiles_read_write";

    public static final String GROUP_SETTINGS_READ = "group_settings_read";
    public static final String GROUP_SETTINGS_READ_WRITE = "group_settings_read_write";

    public static final String INDEX_STATISTICS_READ = "index_statistics_read";

    public static final String LICENSE_READ = "license_read";
    public static final String LICENSE_READ_WRITE = "license_read_write";

    public static final String NODE_SETTINGS_READ = "node_settings_read";
    public static final String NODE_SETTINGS_READ_WRITE = "node_settings_read_write";

    public static final String NOTIFIERS_READ = "notifiers_read";
    public static final String NOTIFIERS_READ_WRITE = "notifiers_read_write";

    public static final String PARSER_SETTINGS_READ = "parser_settings_read";
    public static final String PARSER_SETTINGS_READ_WRITE = "parser_settings_read_write";

    public static final String USER_SETTINGS_READ = "user_settings_read";
    public static final String USER_SETTINGS_READ_WRITE = "user_settings_read_write";

    /**
     * All roles defined as String array. The array must be sorted!
     */
    public static final String[] ALL_ROLES_ARRAY = new String[]{
            AUDIT_LOG_READ,
            CLUSTER_SETTINGS_READ,
            CLUSTER_SETTINGS_READ_WRITE,
            ETM_EVENT_READ,
            ETM_EVENT_READ_WRITE,
            ETM_EVENT_WRITE,
            GROUP_DASHBOARD_READ,
            GROUP_DASHBOARD_READ_WRITE,
            GROUP_SETTINGS_READ,
            GROUP_SETTINGS_READ_WRITE,
            GROUP_SIGNAL_READ,
            GROUP_SIGNAL_READ_WRITE,
            IIB_EVENT_READ,
            IIB_EVENT_READ_WRITE,
            IIB_NODE_READ,
            IIB_NODE_READ_WRITE,
            IMPORT_PROFILES_READ,
            IMPORT_PROFILES_READ_WRITE,
            INDEX_STATISTICS_READ,
            LICENSE_READ,
            LICENSE_READ_WRITE,
            NODE_SETTINGS_READ,
            NODE_SETTINGS_READ_WRITE,
            NOTIFIERS_READ,
            NOTIFIERS_READ_WRITE,
            PARSER_SETTINGS_READ,
            PARSER_SETTINGS_READ_WRITE,
            USER_DASHBOARD_READ_WRITE,
            USER_SETTINGS_READ,
            USER_SETTINGS_READ_WRITE,
            USER_SIGNAL_READ_WRITE
    };


    /**
     * All roles defined a String that can be used in annotations.
     */
    public static final String ALL_ROLES = "{" +
            "\"" + AUDIT_LOG_READ + "\"" +
            ",\"" + CLUSTER_SETTINGS_READ + "\"" +
            ",\"" + CLUSTER_SETTINGS_READ_WRITE + "\"" +
            ",\"" + ETM_EVENT_READ + "\"" +
            ",\"" + ETM_EVENT_READ_WRITE + "\"" +
            ",\"" + ETM_EVENT_WRITE + "\"" +
            ",\"" + GROUP_DASHBOARD_READ + "\"" +
            ",\"" + GROUP_DASHBOARD_READ_WRITE + "\"" +
            ",\"" + GROUP_SETTINGS_READ + "\"" +
            ",\"" + GROUP_SETTINGS_READ_WRITE + "\"" +
            ",\"" + GROUP_SIGNAL_READ + "\"" +
            ",\"" + GROUP_SIGNAL_READ_WRITE + "\"" +
            ",\"" + IIB_EVENT_READ + "\"" +
            ",\"" + IIB_EVENT_READ_WRITE + "\"" +
            ",\"" + IIB_NODE_READ + "\"" +
            ",\"" + IIB_NODE_READ_WRITE + "\"" +
            ",\"" + IMPORT_PROFILES_READ + "\"" +
            ",\"" + IMPORT_PROFILES_READ_WRITE + "\"" +
            ",\"" + INDEX_STATISTICS_READ + "\"" +
            ",\"" + LICENSE_READ + "\"" +
            ",\"" + LICENSE_READ_WRITE + "\"" +
            ",\"" + NODE_SETTINGS_READ + "\"" +
            ",\"" + NODE_SETTINGS_READ_WRITE + "\"" +
            ",\"" + NOTIFIERS_READ + "\"" +
            ",\"" + NOTIFIERS_READ_WRITE + "\"" +
            ",\"" + PARSER_SETTINGS_READ + "\"" +
            ",\"" + PARSER_SETTINGS_READ_WRITE + "\"" +
            ",\"" + USER_DASHBOARD_READ_WRITE + "\"" +
            ",\"" + USER_SETTINGS_READ + "\"" +
            ",\"" + USER_SETTINGS_READ_WRITE + "\"" +
            ",\"" + USER_SIGNAL_READ_WRITE + "\"" +
            "}";

    /**
     * All roles that grant read and write access to all objects.
     */
    public static final String[] ALL_READ_WRITE_SECURITY_ROLES = Arrays.stream(ALL_ROLES_ARRAY)
            .filter(p -> {
                if (p.endsWith("_read") && Arrays.stream(ALL_ROLES_ARRAY).anyMatch(t -> t.equals(p + "_write"))) {
                    return false;
                } else return !ETM_EVENT_WRITE.equals(p);
            })
            .toArray(String[]::new);

    public static final boolean isValidRole(String role) {
        return Arrays.binarySearch(ALL_ROLES_ARRAY, role) >= 0;
    }
}
