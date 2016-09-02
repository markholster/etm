function buildEventPage() {
	var nodeMap = {};
	$('#sel-node').change(function(event) {
		event.preventDefault();
		$serverSelect = $('#sel-server');
		$serverSelect.children().slice(1).remove();
		var serverData = nodeMap[$(this).val()];
		if ('undefined' == typeof serverData) {
			resetValues();
			return;
		}
		$.each(serverData, function(index, server) {
			$serverSelect.append($('<option>').attr('value', server).text(server));
		});
	});
	
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
				$.ajax({
				    type: 'GET',
				    contentType: 'application/json',
				    async: true,
				    url: '../rest/iib/node/' + encodeURIComponent(node.name) + '/servers',
				    success: function(serverData) {
				        if (!serverData) {
				            return;
				        }
				        nodeMap[node.name] = serverData.servers;
				    }
				});
	        });
	        sortSelectOptions($nodeSelect);
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
	
	function resetValues() {
	}
	
}