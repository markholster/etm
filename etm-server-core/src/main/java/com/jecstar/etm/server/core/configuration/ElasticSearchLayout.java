package com.jecstar.etm.server.core.configuration;

public class ElasticSearchLayout {

	public static final String ETM_METRICS_INDEX_PREFIX = "etm_metrics_";
	
	public static final String ETM_EVENT_INDEX_ALIAS_ALL = "etm_event_all";
	public static final String ETM_EVENT_INDEX_PREFIX = "etm_event_";
	public static final String ETM_EVENT_INDEX_TYPE_BUSINESS = "business";
	public static final String ETM_EVENT_INDEX_TYPE_HTTP = "http";
	public static final String ETM_EVENT_INDEX_TYPE_LOG = "log";
	public static final String ETM_EVENT_INDEX_TYPE_MESSAGING = "messaging";
	public static final String ETM_EVENT_INDEX_TYPE_SQL = "sql";
	
	public static final String CONFIGURATION_INDEX_NAME = "etm_configuration";
	public static final String CONFIGURATION_INDEX_TYPE_LICENSE = "license";
	public static final String CONFIGURATION_INDEX_TYPE_LICENSE_ID = "default_configuration";
	public static final String CONFIGURATION_INDEX_TYPE_PARSER = "parser";
	public static final String CONFIGURATION_INDEX_TYPE_ENDPOINT = "endpoint";
	public static final String CONFIGURATION_INDEX_TYPE_ENDPOINT_DEFAULT = "default_configuration";
	public static final String CONFIGURATION_INDEX_TYPE_NODE = "node";
	public static final String CONFIGURATION_INDEX_TYPE_NODE_DEFAULT = "default_configuration";
	public static final String CONFIGURATION_INDEX_TYPE_USER = "user";
	
	
}
