function buildClusterPage() {
	$('#btn-save-cluster').click(function(event) {
		if (!document.getElementById('cluster_form').checkValidity()) {
			return;
		}
		event.preventDefault();
		saveCluster();
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
	
	
	function saveCluster() {
		var clusterData = createClusterData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/cluster',
            data: JSON.stringify(clusterData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		$('#cluster_infoBox').text('Cluster configuration saved.').show('fast').delay(5000).hide('fast');
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
	
	function createClusterData() {
		var clusterData = {
		  shards_per_index : Number($("#input-shards-per-index").val()),
		  replicas_per_index : Number($("#input-replicas-per-index").val()),
		  max_event_index_count : Number($("#input-max-event-indices").val()),
		  max_metrics_index_count : Number($("#input-max-metrics-indices").val()),
		  wait_for_active_shards : Number($("#input-wait-for-active-shards").val()),
		  retry_on_conflict_count : Number($("#input-retries-on-conflict").val()),
		  query_timeout : Number($("#input-query-timeout").val()),
		  max_search_result_download_rows : Number($("#input-search-export-max-rows").val()),
		  max_search_template_count : Number($("#input-search-max-templates").val()),
		  max_search_history_count : Number($("#input-search-max-history-size").val()),
		  max_graph_count : Number($("#input-visualization-max-graph-count").val()),
		  max_dashboard_count : Number($("#input-visualization-max-dashboard-count").val()),
		  enhancing_handler_count : Number($("#input-enhancing-handler-count").val()),
		  persisting_handler_count : Number($("#input-persisting-handler-count").val()),
		  event_buffer_size : Number($("#input-event-buffer-size").val()),
		  persisting_bulk_count : Number($("#input-persisting-bulk-count").val()),
		  persisting_bulk_size : Number($("#input-persisting-bulk-size").val()),
		  persisting_bulk_time : Number($("#input-persisting-bulk-time").val())
		}
		return clusterData;
	}

}