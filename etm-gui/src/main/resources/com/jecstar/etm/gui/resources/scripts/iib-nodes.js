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
		$('#input-username').val(nodeData.username);
		$('#input-password').val(decode(nodeData.password));
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
	    cache: false,
	    success: function(data) {
	        if (!data) {
	            return;
	        }
            const $nodeSelect = $('#sel-node');
	        $.each(data.nodes, function(index, node) {
	        	$nodeSelect.append($('<option>').attr('value', node.name).text(node.name));
	        	nodeMap[node.name] = node;
	        });
            commons.sortSelectOptions($nodeSelect)
	        $nodeSelect.val('');
	    }
	});
	
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
            cache: false,
            data: JSON.stringify(nodeData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if (!isNodeExistent(nodeData.name)) {
                    const $nodeSelect = $('#sel-node');
        			$nodeSelect.append($('<option>').attr('value', nodeData.name).text(nodeData.name));
                    commons.sortSelectOptions($nodeSelect);
        		}
        		nodeMap[nodeData.name] = nodeData;
				commons.showNotification('Node \'' + nodeData.name + '\' saved.', 'success');
            }
        }).always(function () {
            commons.hideModals($('#modal-node-overwrite'));
        });
	}
	
	function removeNode(nodeName) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/iib/node/' + encodeURIComponent(nodeName),
            cache: false,
            success: function(data) {
                if (!data) {
                    return;
                }
        		delete nodeMap[nodeName];
        		$("#sel-node > option").filter(function(i){
        		       return $(this).attr("value") == nodeName;
        		}).remove();
				commons.showNotification('Node \'' + nodeName + '\' removed.', 'success');
            }
        }).always(function() {
            commons.hideModals($('#modal-node-remove'));
        });
	}
	
	function createNodeData() {
		var nodeData = {
			name: $('#input-node-name').val(),
			host: $('#input-host').val(),
			port: Number($('#input-port').val()),
			username: $('#input-username').val(),
			password: encode($('#input-password').val()),
			queue_manager: $('#input-queue-manager').val(),
			channel: $('#input-channel').val() ? $('#input-channel').val() : null
		}
		return nodeData;
	}

	function resetValues() {
		$('#input-node-name').val('');
		$('#input-host').val('');
		$('#input-port').val(1414);
		$('#input-username').val('');
		$('#input-password').val('');
		$('#input-queue-manager').val('');
		$('#input-channel').val('');
		enableOrDisableButtons();
	}
	
	function decode(data) {
		if (!data) {
			return null;
		}
		for (i = 0; i < 7; i++) {
            data = commons.base64Decode(data);
		}
		return data;
	}
	
	function encode(data) {
		if (!data) {
			return null;
		}
		for (i = 0; i < 7; i++) {
            data = commons.base64Decode(data);
		}
		return data;
	}
}