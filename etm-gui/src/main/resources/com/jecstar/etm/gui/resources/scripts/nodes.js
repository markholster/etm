function buildNodePage() {
    const nodeMap = {};
    $('#sel-node').change(function (event) {
        event.preventDefault();
        const nodeData = nodeMap[$(this).val()];
        if ('undefined' == typeof nodeData) {
            resetValues();
            return;
        }
        setNodeData(nodeData);
        enableOrDisableButtons();
    });

    $('#btn-confirm-save-node').on('click', function (event) {
        event.preventDefault();
        if (!document.getElementById('node_form').checkValidity()) {
            return;
        }
        const nodeName = $('#input-node-name').val();
        if (isNodeExistent(nodeName)) {
            $('#overwrite-node-name').text(nodeName);
            $('#modal-node-overwrite').modal();
        } else {
            saveNode();
        }
    });

    $('#btn-save-node').on('click', function (event) {
        event.preventDefault();
        saveNode();
    });

    $('#btn-confirm-remove-node').on('click', function (event) {
        event.preventDefault();
        $('#remove-node-name').text($('#input-node-name').val());
        $('#modal-node-remove').modal();
    });

    $('#btn-remove-node').on('click', function (event) {
        event.preventDefault();
        removeNode($('#input-node-name').val());
    });

    $('#input-node-name').on('input', enableOrDisableButtons);

    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/settings/cluster',
        cache: false,
        success: function (data) {
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
        cache: false,
        success: function (data) {
            if (!data) {
                return;
            }
            const $nodeSelect = $('#sel-node');
            $.each(data.nodes, function (index, node) {
                $nodeSelect.append($('<option>').attr('value', node.name).text(node.name));
                nodeMap[node.name] = node;
            });
            commons.sortSelectOptions($nodeSelect)
            $nodeSelect.val('');
        }
    });

	function enableOrDisableButtons() {
        const nodeName = $('#input-node-name').val();
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
        const nodeData = createNodeData();
        $.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/node/' + encodeURIComponent(nodeData.name),
            cache: false,
            data: JSON.stringify(nodeData),
            success: function (data) {
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
            url: '../rest/settings/node/' + encodeURIComponent(nodeName),
            cache: false,
            success: function(data) {
                if (!data) {
                    return;
                }
                delete nodeMap[nodeName];
                $("#sel-node > option").filter(function (i) {
                    return $(this).attr("value") === nodeName;
                }).remove();
                commons.showNotification('Node \'' + nodeName + '\' removed.', 'success');
            }
        }).always(function() {
            commons.hideModals($('#modal-node-remove'));
        });
	}
	
	function createNodeData() {
        const nodeData = {
            name: $('#input-node-name').val()
        }
        if ($("#input-import-profile-cache-size").val()) {
            nodeData.import_profile_cache_size = Number($("#input-import-profile-cache-size").val());
        }
        if ($("#input-enhancing-handler-count").val()) {
            nodeData.enhancing_handler_count = Number($("#input-enhancing-handler-count").val());
        }
        if ($("#input-persisting-handler-count").val()) {
            nodeData.persisting_handler_count = Number($("#input-persisting-handler-count").val());
        }
        if ($("#input-event-buffer-size").val()) {
            nodeData.event_buffer_size = Number($("#input-event-buffer-size").val());
        }
        nodeData.wait_strategy = $("#sel-wait-strategy").val();
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
		$("#input-import-profile-cache-size").val(nodeData.import_profile_cache_size);
		$("#input-enhancing-handler-count").val(nodeData.enhancing_handler_count);
		$("#input-persisting-handler-count").val(nodeData.persisting_handler_count);
		$("#input-event-buffer-size").val(nodeData.event_buffer_size);
		if (nodeData.wait_strategy) {
			$("#sel-wait-strategy").val(nodeData.wait_strategy);
		}
		$("#input-persisting-bulk-count").val(nodeData.persisting_bulk_count);
		$("#input-persisting-bulk-size").val(nodeData.persisting_bulk_size);
		$("#input-persisting-bulk-time").val(nodeData.persisting_bulk_time);
	}
	
	function setPlaceholderData(clusterData) {
        $("#input-import-profile-cache-size").attr('placeholder', clusterData.import_profile_cache_size);
        $("#input-enhancing-handler-count").attr('placeholder', clusterData.enhancing_handler_count);
        $("#input-persisting-handler-count").attr('placeholder', clusterData.persisting_handler_count);
        $("#input-event-buffer-size").attr('placeholder', clusterData.event_buffer_size);

        $("#sel-wait-strategy").children('option').each(function (ix, option) {
            if ($(option).attr('value') === clusterData.wait_strategy) {
                $(option).text(option.text + ' (Cluster default)').attr('selected', 'selected').attr('data-cluter-default', 'true');
            }
        });

        $("#input-persisting-bulk-count").attr('placeholder', clusterData.persisting_bulk_count);
        $("#input-persisting-bulk-size").attr('placeholder', clusterData.persisting_bulk_size);
        $("#input-persisting-bulk-time").attr('placeholder', clusterData.persisting_bulk_time);
    }

	function resetValues() {
		document.getElementById("node_form").reset();
		enableOrDisableButtons();
	}
}