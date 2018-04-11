function buildNodePage() {
	var Base64={_keyStr:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",encode:function(e){var t="";var n,r,i,s,o,u,a;var f=0;e=Base64._utf8_encode(e);while(f<e.length){n=e.charCodeAt(f++);r=e.charCodeAt(f++);i=e.charCodeAt(f++);s=n>>2;o=(n&3)<<4|r>>4;u=(r&15)<<2|i>>6;a=i&63;if(isNaN(r)){u=a=64}else if(isNaN(i)){a=64}t=t+this._keyStr.charAt(s)+this._keyStr.charAt(o)+this._keyStr.charAt(u)+this._keyStr.charAt(a)}return t},decode:function(e){var t="";var n,r,i;var s,o,u,a;var f=0;e=e.replace(/[^A-Za-z0-9+/=]/g,"");while(f<e.length){s=this._keyStr.indexOf(e.charAt(f++));o=this._keyStr.indexOf(e.charAt(f++));u=this._keyStr.indexOf(e.charAt(f++));a=this._keyStr.indexOf(e.charAt(f++));n=s<<2|o>>4;r=(o&15)<<4|u>>2;i=(u&3)<<6|a;t=t+String.fromCharCode(n);if(u!=64){t=t+String.fromCharCode(r)}if(a!=64){t=t+String.fromCharCode(i)}}t=Base64._utf8_decode(t);return t},_utf8_encode:function(e){e=e.replace(/rn/g,"n");var t="";for(var n=0;n<e.length;n++){var r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r)}else if(r>127&&r<2048){t+=String.fromCharCode(r>>6|192);t+=String.fromCharCode(r&63|128)}else{t+=String.fromCharCode(r>>12|224);t+=String.fromCharCode(r>>6&63|128);t+=String.fromCharCode(r&63|128)}}return t},_utf8_decode:function(e){var t="";var n=0;var r=c1=c2=0;while(n<e.length){r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r);n++}else if(r>191&&r<224){c2=e.charCodeAt(n+1);t+=String.fromCharCode((r&31)<<6|c2&63);n+=2}else{c2=e.charCodeAt(n+1);c3=e.charCodeAt(n+2);t+=String.fromCharCode((r&15)<<12|(c2&63)<<6|c3&63);n+=3}}return t}}

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
            cache: false,
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
            hideModals($('#modal-node-overwrite'));
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
        		$('#nodes_infoBox').text('Node \'' + nodeName + '\' removed.').show('fast').delay(5000).hide('fast');
            }
        }).always(function() {
            hideModals($('#modal-node-remove'));
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
		    data = Base64.decode(data);
		}
		return data;
	}
	
	function encode(data) {
		if (!data) {
			return null;
		}
		for (i = 0; i < 7; i++) {
		    data = Base64.encode(data);
		}
		return data;
	}
}