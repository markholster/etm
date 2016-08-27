function buildNodePage() {
	var nodeMap = {};
	$('#sel-node').change(function(event) {
		event.preventDefault();
		var nodeData = nodeMap[$(this).val()];
		if ('undefined' == typeof nodeData) {
			resetValues();
			return;
		}
		$('#input-node-name').val(nodeData.name);
		$('#input-host').val(nodeData.host);
		$('#input-port').val(nodeData.port);
		$('#input-queue-manager').val(nodeData.queue_manager);
		$('#input-channel').val(nodeData.channel);
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
	    url: '../rest/iib/nodes',
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
            url: '../rest/iib/node/' + encodeURIComponent(nodeData.name),
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
            url: '../rest/iib/node/' + encodeURIComponent(nodeName),
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
			name: $('#input-node-name').val(),
			host: $('#input-host').val(),
			port: Number($('#input-port').val()),
			queue_manager: $('#input-queue-manager').val(),
			channel: $('#input-channel').val() ? $('#input-channel').val() : null
		}
		return nodeData;
	}

	function resetValues() {
		$('#input-node-name').val('');
		$('#input-host').val('');
		$('#input-port').val('1414');
		$('#input-queue-manager').val('');
		$('#input-channel').val('');
		enableOrDisableButtons();
	}
}