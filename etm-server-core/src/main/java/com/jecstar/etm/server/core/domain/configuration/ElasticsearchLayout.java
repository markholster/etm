package com.jecstar.etm.server.core.domain.configuration;

import com.jecstar.etm.domain.writer.TelemetryEventTags;

public class ElasticsearchLayout {

	public static final String ETM_METRICS_TEMPLATE_NAME = "etm_metrics";
	public static final String ETM_METRICS_INDEX_PREFIX = ETM_METRICS_TEMPLATE_NAME + "_";
	public static final String ETM_METRICS_INDEX_ALIAS_ALL = ETM_METRICS_INDEX_PREFIX + "all";
	
	public static final String ETM_EVENT_TEMPLATE_NAME = "etm_event";
	public static final String ETM_EVENT_INDEX_PREFIX = ETM_EVENT_TEMPLATE_NAME + "_";
	public static final String ETM_EVENT_INDEX_ALIAS_ALL = ETM_EVENT_INDEX_PREFIX + "all";
	public static final String ETM_EVENT_INDEX_TYPE_BUSINESS = TelemetryEventTags.EVENT_TYPE_BUSINESS;
	public static final String ETM_EVENT_INDEX_TYPE_HTTP = TelemetryEventTags.EVENT_TYPE_HTTP;
	public static final String ETM_EVENT_INDEX_TYPE_LOG = TelemetryEventTags.EVENT_TYPE_LOG;
	public static final String ETM_EVENT_INDEX_TYPE_MESSAGING = TelemetryEventTags.EVENT_TYPE_MESSAGING;
	public static final String ETM_EVENT_INDEX_TYPE_SQL = TelemetryEventTags.EVENT_TYPE_SQL;
	
	public static final String CONFIGURATION_INDEX_NAME = "etm_configuration";
	public static final String CONFIGURATION_INDEX_TYPE_DASHBOARD = "dashboard";
	public static final String CONFIGURATION_INDEX_TYPE_GRAPH = "graph";
	public static final String CONFIGURATION_INDEX_TYPE_LICENSE = "license";
	public static final String CONFIGURATION_INDEX_TYPE_LICENSE_ID = "default_configuration";
	public static final String CONFIGURATION_INDEX_TYPE_PARSER = "parser";
	public static final String CONFIGURATION_INDEX_TYPE_ENDPOINT = "endpoint";
	public static final String CONFIGURATION_INDEX_TYPE_ENDPOINT_DEFAULT = "default_configuration";
	public static final String CONFIGURATION_INDEX_TYPE_LDAP = "ldap";
	public static final String CONFIGURATION_INDEX_TYPE_LDAP_DEFAULT = "default_configuration";
	public static final String CONFIGURATION_INDEX_TYPE_NODE = "node";
	public static final String CONFIGURATION_INDEX_TYPE_NODE_DEFAULT = "default_configuration";
	public static final String CONFIGURATION_INDEX_TYPE_USER = "user";
	public static final String CONFIGURATION_INDEX_TYPE_GROUP = "group";
	public static final String CONFIGURATION_INDEX_TYPE_IIB_NODE = "iib-node";
	
	public static final String ETM_AUDIT_LOG_TEMPLATE_NAME = "etm_audit";
	public static final String ETM_AUDIT_LOG_INDEX_PREFIX = ETM_AUDIT_LOG_TEMPLATE_NAME + "_";
	public static final String ETM_AUDIT_LOG_INDEX_ALIAS_ALL = ETM_AUDIT_LOG_INDEX_PREFIX + "all";
	public static final String ETM_AUDIT_LOG_INDEX_TYPE_LOGIN = "login";
	public static final String ETM_AUDIT_LOG_INDEX_TYPE_LOGOUT = "logout";
	public static final String ETM_AUDIT_LOG_INDEX_TYPE_SEARCH = "search";
	public static final String ETM_AUDIT_LOG_INDEX_TYPE_GET_EVENT = "getevent";
	
	public static final String STATE_INDEX_NAME = "etm_state";
	public static final String STATE_INDEX_TYPE_SESSION = "session";
	
	public static final String[] USER_CASCADING = new String[] {
			CONFIGURATION_INDEX_TYPE_USER,
			CONFIGURATION_INDEX_TYPE_GRAPH,
			CONFIGURATION_INDEX_TYPE_DASHBOARD
	}; 
	
}
