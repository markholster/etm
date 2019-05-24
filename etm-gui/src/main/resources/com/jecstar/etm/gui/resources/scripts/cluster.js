function buildClusterPage() {
	var Base64={_keyStr:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",encode:function(e){var t="";var n,r,i,s,o,u,a;var f=0;e=Base64._utf8_encode(e);while(f<e.length){n=e.charCodeAt(f++);r=e.charCodeAt(f++);i=e.charCodeAt(f++);s=n>>2;o=(n&3)<<4|r>>4;u=(r&15)<<2|i>>6;a=i&63;if(isNaN(r)){u=a=64}else if(isNaN(i)){a=64}t=t+this._keyStr.charAt(s)+this._keyStr.charAt(o)+this._keyStr.charAt(u)+this._keyStr.charAt(a)}return t},decode:function(e){var t="";var n,r,i;var s,o,u,a;var f=0;e=e.replace(/[^A-Za-z0-9+/=]/g,"");while(f<e.length){s=this._keyStr.indexOf(e.charAt(f++));o=this._keyStr.indexOf(e.charAt(f++));u=this._keyStr.indexOf(e.charAt(f++));a=this._keyStr.indexOf(e.charAt(f++));n=s<<2|o>>4;r=(o&15)<<4|u>>2;i=(u&3)<<6|a;t=t+String.fromCharCode(n);if(u!=64){t=t+String.fromCharCode(r)}if(a!=64){t=t+String.fromCharCode(i)}}t=Base64._utf8_decode(t);return t},_utf8_encode:function(e){e=e.replace(/rn/g,"n");var t="";for(var n=0;n<e.length;n++){var r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r)}else if(r>127&&r<2048){t+=String.fromCharCode(r>>6|192);t+=String.fromCharCode(r&63|128)}else{t+=String.fromCharCode(r>>12|224);t+=String.fromCharCode(r>>6&63|128);t+=String.fromCharCode(r&63|128)}}return t},_utf8_decode:function(e){var t="";var n=0;var r=c1=c2=0;while(n<e.length){r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r);n++}else if(r>191&&r<224){c2=e.charCodeAt(n+1);t+=String.fromCharCode((r&31)<<6|c2&63);n+=2}else{c2=e.charCodeAt(n+1);c3=e.charCodeAt(n+2);t+=String.fromCharCode((r&15)<<12|(c2&63)<<6|c3&63);n+=3}}return t}}

	$('#btn-save-general').click(function(event) {
		if (!document.getElementById('form-general').checkValidity()) {
			return;
		}
		event.preventDefault();
		saveCluster('General');
	});

	$('#btn-save-elasticsearch').click(function(event) {
		if (!document.getElementById('form-elasticsearch').checkValidity()) {
			return;
		}
		event.preventDefault();
		saveCluster('Elasticsearch');
	});

	$('#btn-save-persisting').click(function(event) {
		if (!document.getElementById('form-persisting').checkValidity()) {
			return;
		}
		event.preventDefault();
		saveCluster('Persisting');
	});

	$('#btn-save-ldap').click(function(event) {
		if (!document.getElementById('form-ldap').checkValidity()) {
			return;
		}
		event.preventDefault();
		saveLdap();
	});

	$('#btn-confirm-remove-ldap').click(function(event) {
		event.preventDefault();
		$('#modal-ldap-remove').modal();
	});

	$('#btn-save-notifications').click(function(event) {
        if (!document.getElementById('form-notifications').checkValidity()) {
            return;
        }
        event.preventDefault();
        saveCluster('Notifications');
    });

	$('#btn-remove-ldap').click(function(event) {
		event.preventDefault();
		$.ajax({
		    type: 'DELETE',
		    contentType: 'application/json',
		    url: '../rest/settings/ldap',
		    cache: false,
		    success: function(data) {
		        if (!data) {
		            return;
		        }
				document.getElementById('form-ldap').reset();
				$('#btn-confirm-remove-ldap').attr('disabled', 'disabled');
				commons.showNotification('Ldap configuration removed.', 'success');
		    }
		}).always(function () {
            commons.hideModals($('#modal-ldap-remove'));
        });
	});
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/cluster',
	    cache: false,
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        setClusterData(data)
	    }
	});
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/ldap',
	    cache: false,
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        setLdapData(data)
	        $('#btn-confirm-remove-ldap').removeAttr('disabled');
	    }
	});
	
	function saveCluster(context) {
		var clusterData = createClusterData(context);
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/cluster',
            cache: false,
            data: JSON.stringify(clusterData),
            success: function(data) {
                if (!data) {
                    return;
                }
				commons.showNotification((context + ' configuration saved.', 'success'));
            }
        });    		
	}
	
	function saveLdap() {
        const ldapData = createLdapData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/ldap',
            cache: false,
            data: JSON.stringify(ldapData),
            success: function(data) {
                if (!data) {
                    return;
                }
				commons.showNotification('Ldap configuration saved.', 'success');
        		$('#btn-confirm-remove-ldap').removeAttr('disabled');
            }
        });    		
	}
	
	function setClusterData(data) {
		$("#input-session-timeout").val(data.session_timeout);
		$("#input-import-profile-cache-size").val(data.import_profile_cache_size);
		$("#input-shards-per-index").val(data.shards_per_index);
		$("#input-replicas-per-index").val(data.replicas_per_index);
		$("#input-max-event-indices").val(data.max_event_index_count);
		$("#input-max-metrics-indices").val(data.max_metrics_index_count);
		$("#input-max-audit-log-indices").val(data.max_audit_log_index_count);
		$("#input-wait-for-active-shards").val(data.wait_for_active_shards);
		$("#input-retries-on-conflict").val(data.retry_on_conflict_count);
		$("#input-query-timeout").val(data.query_timeout);
		$("#input-search-export-max-rows").val(data.max_search_result_download_rows);
		$("#input-search-max-templates").val(data.max_search_template_count);
		$("#input-search-max-history-size").val(data.max_search_history_count);
		$("#input-visualization-max-graph-count").val(data.max_graph_count);
		$("#input-visualization-max-dashboard-count").val(data.max_dashboard_count);
		$("#input-signal-max-signal-count").val(data.max_signal_count);
		$("#input-enhancing-handler-count").val(data.enhancing_handler_count);
		$("#input-persisting-handler-count").val(data.persisting_handler_count);
		$("#input-event-buffer-size").val(data.event_buffer_size);
		$("#sel-wait-strategy").val(data.wait_strategy);
		$("#input-persisting-bulk-count").val(data.persisting_bulk_count);
		$("#input-persisting-bulk-size").val(data.persisting_bulk_size);
		$("#input-persisting-bulk-time").val(data.persisting_bulk_time);
		$("#input-persisting-bulk-threads").val(data.persisting_bulk_threads);
	}
	
	function createClusterData(context) {
		var clusterData = {};
		if ('General' == context) {
			clusterData.session_timeout = Number($("#input-session-timeout").val());
            clusterData.endpoint_configuration_cache_size = Number($("#input-endpoint-configuration-cache-size").val());
			clusterData.max_search_result_download_rows = Number($("#input-search-export-max-rows").val());
			clusterData.max_search_template_count = Number($("#input-search-max-templates").val());
			clusterData.max_search_history_count = Number($("#input-search-max-history-size").val());
			clusterData.max_graph_count = Number($("#input-visualization-max-graph-count").val());
			clusterData.max_dashboard_count = Number($("#input-visualization-max-dashboard-count").val());
			clusterData.max_signal_count = Number($("#input-signal-max-signal-count").val());
		} else if ('Elasticsearch' == context) {
			clusterData.shards_per_index = Number($("#input-shards-per-index").val());
			clusterData.replicas_per_index = Number($("#input-replicas-per-index").val());
			clusterData.max_event_index_count = Number($("#input-max-event-indices").val());
			clusterData.max_metrics_index_count = Number($("#input-max-metrics-indices").val());
			clusterData.max_audit_log_index_count = Number($("#input-max-audit-log-indices").val());
			clusterData.wait_for_active_shards = Number($("#input-wait-for-active-shards").val());
			clusterData.retry_on_conflict_count = Number($("#input-retries-on-conflict").val());
			clusterData.query_timeout = Number($("#input-query-timeout").val());
		} else if ('Persisting' == context) {
			clusterData.enhancing_handler_count = Number($("#input-enhancing-handler-count").val());
			clusterData.persisting_handler_count = Number($("#input-persisting-handler-count").val());
			clusterData.event_buffer_size = Number($("#input-event-buffer-size").val());
			clusterData.wait_strategy = $("#sel-wait-strategy").val();
			clusterData.persisting_bulk_count = Number($("#input-persisting-bulk-count").val());
			clusterData.persisting_bulk_size = Number($("#input-persisting-bulk-size").val());
			clusterData.persisting_bulk_time = Number($("#input-persisting-bulk-time").val());
			clusterData.persisting_bulk_threads = Number($("#input-persisting-bulk-threads").val());
		}
		return clusterData;
	}
	
	function setLdapData(data) {
		$('#input-ldap-host').val(data.host);
		$('#input-ldap-port').val(data.port);
		$('#sel-ldap-connection-security').val(data.connection_security);
		$('#input-ldap-bind-dn').val(data.bind_dn);
		$('#input-ldap-bind-password').val(decode(data.bind_password));
		$('#input-ldap-connection-pool-min').val(data.min_pool_size);
		$('#input-ldap-connection-pool-max').val(data.max_pool_size);
		$('#input-ldap-connection-test-base-dn').val(data.connection_test_base_dn);
		$('#input-ldap-connection-test-search-filter').val(data.connection_test_search_filter);
		$('#input-ldap-user-base-dn').val(data.user_base_dn);
		$('#input-ldap-user-search-filter').val(data.user_search_filter);
		$('#sel-ldap-user-search-in-subtree').val(data.user_search_in_subtree ? 'true' : 'false');
		$('#input-ldap-user-id-attribute').val(data.user_identifier_attribute);
		$('#input-ldap-user-fullname-attribute').val(data.user_full_name_attribute);
		$('#input-ldap-user-email-attribute').val(data.user_email_attribute);
		$('#input-ldap-user-member-of-groups-attribute').val(data.user_member_of_groups_attribute);
		$('#input-ldap-user-groups-query-base-dn').val(data.user_groups_query_base_dn);
		$('#input-ldap-user-groups-query-filter').val(data.user_groups_query_filter);
		$('#input-ldap-group-base-dn').val(data.group_base_dn);
		$('#input-ldap-group-search-filter').val(data.group_search_filter);
	}
	
	function createLdapData() {
		var ldapData = {};
		ldapData.host = $('#input-ldap-host').val();
		ldapData.port = Number($('#input-ldap-port').val());
		ldapData.connection_security = $('#sel-ldap-connection-security').val() ? $('#sel-ldap-connection-security').val() : null;
		ldapData.bind_dn = $('#input-ldap-bind-dn').val();
		ldapData.bind_password = encode($('#input-ldap-bind-password').val());
		ldapData.min_pool_size = Number($('#input-ldap-connection-pool-min').val());
		ldapData.max_pool_size = Number($('#input-ldap-connection-pool-max').val());
		ldapData.connection_test_base_dn = $('#input-ldap-connection-test-base-dn').val();
		ldapData.connection_test_search_filter = $('#input-ldap-connection-test-search-filter').val();
		ldapData.user_base_dn = $('#input-ldap-user-base-dn').val();
		ldapData.user_search_filter = $('#input-ldap-user-search-filter').val();
		ldapData.user_search_in_subtree = $('#sel-ldap-user-search-in-subtree').val() == 'true' ? true : false;
		ldapData.user_identifier_attribute = $('#input-ldap-user-id-attribute').val();
		ldapData.user_full_name_attribute = $('#input-ldap-user-fullname-attribute').val();
		ldapData.user_email_attribute = $('#input-ldap-user-email-attribute').val();
		ldapData.user_member_of_groups_attribute = $('#input-ldap-user-member-of-groups-attribute').val();
		ldapData.user_groups_query_base_dn = $('#input-ldap-user-groups-query-base-dn').val();
		ldapData.user_groups_query_filter = $('#input-ldap-user-groups-query-filter').val();
		ldapData.group_base_dn = $('#input-ldap-group-base-dn').val();
		ldapData.group_search_filter = $('#input-ldap-group-search-filter').val();

		return ldapData;
	}
	
	function decode(data) {
		if (!data) {
			return null;
		}
        for (let i = 0; i < 7; i++) {
		    data = Base64.decode(data);
		}
		return data;
	}
	
	function encode(data) {
		if (!data) {
			return null;
		}
        for (let i = 0; i < 7; i++) {
		    data = Base64.encode(data);
		}
		return data;
	}

}