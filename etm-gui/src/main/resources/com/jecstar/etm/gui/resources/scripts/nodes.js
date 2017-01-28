function buildNodePage() {
	var nodeMap = {};
	$('#sel-node').change(function(event) {
		event.preventDefault();
		var nodeData = nodeMap[$(this).val()];
		if ('undefined' == typeof nodeData) {
			resetValues();
			return;
		}
		setNodeData(nodeData);
		enableOrDisableButtons();
	});
	
	$('#btn-confirm-save-node').click(function(event) {
		event.preventDefault();
		if (!document.getElementById('node_form').checkValidity()) {
			return;
		}
		var nodeName = $('#input-node-name').val();
		if (isNodeExistent(nodeName)) {
			$('#overwrite-node-name').text(nodeName);
			$('#modal-node-overwrite').modal();
		} else {
			saveNode();
		}
	});
	
	$('#btn-save-node').click(function(event) {
		saveNode();
	});
	
	$('#btn-confirm-remove-node').click(function(event) {
		event.preventDefault();
		$('#remove-node-name').text($('#input-node-name').val());
        $('#modal-node-remove').modal();
	});	

	$('#btn-remove-node').click(function(event) {
		removeNode($('#input-node-name').val());
	});

	
	$('#input-node-name').on('input', enableOrDisableButtons);
	
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/cluster',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        setPlaceholderData(data);
	    }
	});
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/nodes',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        $nodeSelect = $('#sel-node');
	        $.each(data.nodes, function(index, node) {
	        	$nodeSelect.append($('<option>').attr('value', node.name).text(node.name));
	        	nodeMap[node.name] = node;
	        });
	        sortSelectOptions($nodeSelect)
	        $nodeSelect.val('');
	    }
	});
	
	function sortSelectOptions($select) {
		var options = $select.children('option');
		options.detach().sort(function(a,b) {
		    var at = $(a).text();
		    var bt = $(b).text();         
		    return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
		});
		options.appendTo($select);
	}
	
	function enableOrDisableButtons() {
		var nodeName = $('#input-node-name').val();
		if (nodeName) {
			$('#btn-confirm-save-node').removeAttr('disabled');
			if (isNodeExistent(nodeName)) {
				$('#btn-confirm-remove-node').removeAttr('disabled');
			} else {
				$('#btn-confirm-remove-node').attr('disabled', 'disabled');
			}
		} else {
			$('#btn-confirm-save-node, #btn-confirm-remove-node').attr('disabled', 'disabled');
		}
	}
	
	function isNodeExistent(nodeName) {
		return "undefined" != typeof nodeMap[nodeName];
	}
	
	function saveNode() {
		var nodeData = createNodeData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/node/' + encodeURIComponent(nodeData.name),
            data: JSON.stringify(nodeData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if (!isNodeExistent(nodeData.name)) {
        			$nodeSelect = $('#sel-node');
        			$nodeSelect.append($('<option>').attr('value', nodeData.name).text(nodeData.name));
        			sortSelectOptions($nodeSelect);
        		}
        		nodeMap[nodeData.name] = nodeData;
        		$('#nodes_infoBox').text('Node \'' + nodeData.name + '\' saved.').show('fast').delay(5000).hide('fast');
            }
        }).always(function () {
        	$('#modal-node-overwrite').modal('hide');
        });  		
	}
	
	function removeNode(nodeName) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/node/' + encodeURIComponent(nodeName),
            success: function(data) {
                if (!data) {
                    return;
                }
        		delete nodeMap[nodeName];
        		$("#sel-node > option").filter(function(i){
        		       return $(this).attr("value") == nodeName;
        		}).remove();
        		$('#nodes_infoBox').text('Node \'' + nodeName + '\' removed.').show('fast').delay(5000).hide('fast');
            }
        }).always(function() {
        	$('#modal-node-remove').modal('hide');
        });  		
	}
	
	function createNodeData() {
		var nodeData = {
			name: $('#input-node-name').val()
		}
		if ($("#input-enhancing-handler-count").val()) {
			nodeData.enhancing_handler_count =  Number($("#input-enhancing-handler-count").val());
		}
		if ($("#input-persisting-handler-count").val()) {
			nodeData.persisting_handler_count = Number($("#input-persisting-handler-count").val());
		}
		if ($("#input-event-buffer-size").val()) {
			nodeData.event_buffer_size = Number($("#input-event-buffer-size").val());
		}
		if ($("#input-persisting-bulk-count").val()) {
			nodeData.persisting_bulk_count = Number($("#input-persisting-bulk-count").val());
		}
		if ($("#input-persisting-bulk-size").val()) {
			nodeData.persisting_bulk_size = Number($("#input-persisting-bulk-size").val());
		}
		if ($("#input-persisting-bulk-time").val()) {
			nodeData.persisting_bulk_time = Number($("#input-persisting-bulk-time").val());
		}
 		return nodeData;
	}
	
	function setNodeData(nodeData) {
		$('#input-node-name').val(nodeData.name);
		$("#input-enhancing-handler-count").val(nodeData.enhancing_handler_count);
		$("#input-persisting-handler-count").val(nodeData.persisting_handler_count);
		$("#input-event-buffer-size").val(nodeData.event_buffer_size);
		$("#input-persisting-bulk-count").val(nodeData.persisting_bulk_count);
		$("#input-persisting-bulk-size").val(nodeData.persisting_bulk_size);
		$("#input-persisting-bulk-time").val(nodeData.persisting_bulk_time);
	}
	
	function setPlaceholderData(clusterData) {
		$("#input-enhancing-handler-count").attr('placeholder', clusterData.enhancing_handler_count);
		$("#input-persisting-handler-count").attr('placeholder', clusterData.persisting_handler_count);
		$("#input-event-buffer-size").attr('placeholder', clusterData.event_buffer_size);
		$("#input-persisting-bulk-count").attr('placeholder', clusterData.persisting_bulk_count);
		$("#input-persisting-bulk-size").attr('placeholder', clusterData.persisting_bulk_size);
		$("#input-persisting-bulk-time").attr('placeholder', clusterData.persisting_bulk_time);
	}

	function resetValues() {
		document.getElementById("node_form").reset();
		enableOrDisableButtons();
	}
}