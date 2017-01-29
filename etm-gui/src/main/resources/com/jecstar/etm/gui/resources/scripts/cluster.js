function buildClusterPage() {
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

	$('#btn-save-search').click(function(event) {
		if (!document.getElementById('form-search').checkValidity()) {
			return;
		}
		event.preventDefault();
		saveCluster('Search');
	});

	$('#btn-save-visualizations').click(function(event) {
		if (!document.getElementById('form-visualizations').checkValidity()) {
			return;
		}
		event.preventDefault();
		saveCluster('Visualizations');
	});

	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/cluster',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        setClusterData(data)
	    }
	});
	
	
	function saveCluster(context) {
		var clusterData = createClusterData(context);
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/cluster',
            data: JSON.stringify(clusterData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		$('#cluster_infoBox').text(context + ' configuration saved.').show('fast').delay(5000).hide('fast');
            }
        });    		
	}
	
	function setClusterData(data) {
		$("#input-shards-per-index").val(data.shards_per_index);
		$("#input-replicas-per-index").val(data.replicas_per_index);
		$("#input-max-event-indices").val(data.max_event_index_count);
		$("#input-max-metrics-indices").val(data.max_metrics_index_count);
		$("#input-wait-for-active-shards").val(data.wait_for_active_shards);
		$("#input-retries-on-conflict").val(data.retry_on_conflict_count);
		$("#input-query-timeout").val(data.query_timeout);
		$("#input-search-export-max-rows").val(data.max_search_result_download_rows);
		$("#input-search-max-templates").val(data.max_search_template_count);
		$("#input-search-max-history-size").val(data.max_search_history_count);
		$("#input-visualization-max-graph-count").val(data.max_graph_count);
		$("#input-visualization-max-dashboard-count").val(data.max_dashboard_count);
		$("#input-enhancing-handler-count").val(data.enhancing_handler_count);
		$("#input-persisting-handler-count").val(data.persisting_handler_count);
		$("#input-event-buffer-size").val(data.event_buffer_size);
		$("#input-persisting-bulk-count").val(data.persisting_bulk_count);
		$("#input-persisting-bulk-size").val(data.persisting_bulk_size);
		$("#input-persisting-bulk-time").val(data.persisting_bulk_time);
	}
	
	function createClusterData(context) {
		var clusterData = {};
		if ('Elasticsearch' == context) {
			clusterData.shards_per_index = Number($("#input-shards-per-index").val());
			clusterData.replicas_per_index = Number($("#input-replicas-per-index").val());
			clusterData.max_event_index_count = Number($("#input-max-event-indices").val());
			clusterData.max_metrics_index_count = Number($("#input-max-metrics-indices").val());
			clusterData.wait_for_active_shards = Number($("#input-wait-for-active-shards").val());
			clusterData.retry_on_conflict_count = Number($("#input-retries-on-conflict").val());
			clusterData.query_timeout = Number($("#input-query-timeout").val());
		} else if ('Persisting' == context) {
			clusterData.enhancing_handler_count = Number($("#input-enhancing-handler-count").val());
			clusterData.persisting_handler_count = Number($("#input-persisting-handler-count").val());
			clusterData.event_buffer_size = Number($("#input-event-buffer-size").val());
			clusterData.persisting_bulk_count = Number($("#input-persisting-bulk-count").val());
			clusterData.persisting_bulk_size = Number($("#input-persisting-bulk-size").val());
			clusterData.persisting_bulk_time = Number($("#input-persisting-bulk-time").val());
		} else if ('Search' == context) {
			clusterData.max_search_result_download_rows = Number($("#input-search-export-max-rows").val());
			clusterData.max_search_template_count = Number($("#input-search-max-templates").val());
			clusterData.max_search_history_count = Number($("#input-search-max-history-size").val());
		} else if ('Visualizations' == context) {
			clusterData.max_graph_count = Number($("#input-visualization-max-graph-count").val());
			clusterData.max_dashboard_count = Number($("#input-visualization-max-dashboard-count").val());
		}
		return clusterData;
	}

}