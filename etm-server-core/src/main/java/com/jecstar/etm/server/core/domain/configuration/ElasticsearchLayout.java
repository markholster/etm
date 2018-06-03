package com.jecstar.etm.server.core.domain.configuration;

import com.jecstar.etm.domain.writer.TelemetryEventTags;

/**
 * Class with constants used in the Elasticsearch mappgins.
 */
public final class ElasticsearchLayout {

    /**
     * The prefix all for all indices used by Enterprise Telemetry Monitor.
     */
    public static final String ETM_INDEX_PREFIX = "etm_";

    /**
     * The name of the attribute that old the object type. Object types are different Java object mapped to the same Elasticsearch index.
     */
    public static final String ETM_TYPE_ATTRIBUTE_NAME = "object_type";

    /**
     * The name of the default elasticsearch index type. From Elasticsearch 6.0 and later only a single index type is allowed.
     */
    public static final String ETM_DEFAULT_TYPE = "doc";

    /**
     * The object name that holds the default configuration for an entity.
     */
    public static final String ETM_OBJECT_NAME_DEFAULT = "default_configuration";

    public static final String METRICS_TEMPLATE_NAME = ETM_INDEX_PREFIX + "metrics";
    public static final String METRICS_INDEX_PREFIX = METRICS_TEMPLATE_NAME + "_";
    public static final String METRICS_INDEX_ALIAS_ALL = METRICS_INDEX_PREFIX + "all";
    public static final String METRICS_OBJECT_TYPE_ETM_NODE = "etm_node";

    public static final String EVENT_TEMPLATE_NAME = ETM_INDEX_PREFIX + "event";
    public static final String EVENT_INDEX_PREFIX = EVENT_TEMPLATE_NAME + "_";
    public static final String EVENT_INDEX_ALIAS_ALL = EVENT_INDEX_PREFIX + "all";
    public static final String EVENT_OBJECT_TYPE_BUSINESS = TelemetryEventTags.EVENT_OBJECT_TYPE_BUSINESS;
    public static final String EVENT_OBJECT_TYPE_HTTP = TelemetryEventTags.EVENT_OBJECT_TYPE_HTTP;
    public static final String EVENT_OBJECT_TYPE_LOG = TelemetryEventTags.EVENT_OBJECT_TYPE_LOG;
    public static final String EVENT_OBJECT_TYPE_MESSAGING = TelemetryEventTags.EVENT_OBJECT_TYPE_MESSAGING;
    public static final String EVENT_OBJECT_TYPE_SQL = TelemetryEventTags.EVENT_OBJECT_TYPE_SQL;

    public static final String CONFIGURATION_INDEX_NAME = ETM_INDEX_PREFIX + "configuration";
    public static final String CONFIGURATION_OBJECT_TYPE_LICENSE = "license";
    public static final String CONFIGURATION_OBJECT_ID_LICENSE_DEFAULT = CONFIGURATION_OBJECT_TYPE_LICENSE + "_" + ETM_OBJECT_NAME_DEFAULT;
    public static final String CONFIGURATION_OBJECT_TYPE_PARSER = "parser";
    public static final String CONFIGURATION_OBJECT_TYPE_PARSER_ID_PREFIX = CONFIGURATION_OBJECT_TYPE_PARSER + "_";
    public static final String CONFIGURATION_OBJECT_TYPE_ENDPOINT = "endpoint";
    public static final String CONFIGURATION_OBJECT_TYPE_ENDPOINT_ID_PREFIX = CONFIGURATION_OBJECT_TYPE_ENDPOINT + "_";
    public static final String CONFIGURATION_OBJECT_ID_ENDPOINT_DEFAULT = CONFIGURATION_OBJECT_TYPE_ENDPOINT_ID_PREFIX + ETM_OBJECT_NAME_DEFAULT;
    public static final String CONFIGURATION_OBJECT_TYPE_LDAP = "ldap";
    public static final String CONFIGURATION_OBJECT_TYPE_LDAP_ID_PREFIX = CONFIGURATION_OBJECT_TYPE_LDAP + "_";
    public static final String CONFIGURATION_OBJECT_ID_LDAP_DEFAULT = CONFIGURATION_OBJECT_TYPE_LDAP_ID_PREFIX + ETM_OBJECT_NAME_DEFAULT;
    public static final String CONFIGURATION_OBJECT_TYPE_NODE = "node";
    public static final String CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX = CONFIGURATION_OBJECT_TYPE_NODE + "_";
    public static final String CONFIGURATION_OBJECT_ID_NODE_DEFAULT = CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + ETM_OBJECT_NAME_DEFAULT;
    public static final String CONFIGURATION_OBJECT_TYPE_USER = "user";
    public static final String CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX = CONFIGURATION_OBJECT_TYPE_USER + "_";
    public static final String CONFIGURATION_OBJECT_TYPE_GROUP = "group";
    public static final String CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX = CONFIGURATION_OBJECT_TYPE_GROUP + "_";
    public static final String CONFIGURATION_OBJECT_TYPE_IIB_NODE = "iib-node";
    public static final String CONFIGURATION_OBJECT_TYPE_IIB_NODE_ID_PREFIX = CONFIGURATION_OBJECT_TYPE_IIB_NODE + "_";
    public static final String CONFIGURATION_OBJECT_TYPE_NOTIFIER = "notifier";
    public static final String CONFIGURATION_OBJECT_TYPE_NOTIFIER_ID_PREFIX = CONFIGURATION_OBJECT_TYPE_NOTIFIER + "_";

    public static final String AUDIT_LOG_TEMPLATE_NAME = ETM_INDEX_PREFIX + "audit";
    public static final String AUDIT_LOG_INDEX_PREFIX = AUDIT_LOG_TEMPLATE_NAME + "_";
    public static final String AUDIT_LOG_INDEX_ALIAS_ALL = AUDIT_LOG_INDEX_PREFIX + "all";
    public static final String AUDIT_LOG_OBJECT_TYPE_LOGIN = "login";
    public static final String AUDIT_LOG_OBJECT_TYPE_LOGOUT = "logout";
    public static final String AUDIT_LOG_OBJECT_TYPE_SEARCH = "search";
    public static final String AUDIT_LOG_OBJECT_TYPE_GET_EVENT = "getevent";

    public static final String STATE_INDEX_NAME = ETM_INDEX_PREFIX + "state";
    public static final String STATE_OBJECT_TYPE_SESSION = "session";
    public static final String STATE_OBJECT_TYPE_SESSION_ID_PREFIX = STATE_OBJECT_TYPE_SESSION + "_";


    public static boolean OLD_EVENT_TYPES_PRESENT = true;

}
