function buildEventPage() {
	var nodeMap = {};
	var serverMap = {}
	$('#sel-node').change(function(event) {
		event.preventDefault();
		$serverSelect = $('#sel-server');
		emptyServerSelect();
		emptyContainerSelect();
		var serverData = nodeMap[$(this).val()];
		if ('undefined' == typeof serverData) {
			$('#sel-server, #sel-container').attr('disabled', 'disabled');
			return;
		}
		$.each(serverData, function(index, server) {
			$serverSelect.append($('<option>').attr('value', server).text(server));
		});
		sortSelectOptions($serverSelect);
		$serverSelect.removeAttr('disabled');
		$serverSelect.val('');
	});
	
	$('#sel-server').change(function(event) {
		event.preventDefault();
		$applicationGroup = $('#sel-container-application-group');
		$libraryGroup = $('#sel-container-library-group');
		$flowGroup = $('#sel-container-flow-group');
		
		emptyContainerSelect();
		if ($(this).val() == '') {
			$('#sel-container').attr('disabled', 'disabled');
			return;
		}
		
		var deploymentData = serverMap[$('#sel-node').val() + '_'  + $(this).val()];
		if ('undefined' == typeof deploymentData) {
			$.ajax({
			    type: 'GET',
			    contentType: 'application/json',
			    url: '../rest/iib/node/' + encodeURIComponent($('#sel-node').val()) + '/server/' + encodeURIComponent($(this).val()),
			    success: function(data) {
			        if (!data) {
			            return;
			        }
			        serverMap[$('#sel-node').val() + '_'  + $('#sel-server').val()] = data.deployments;
			        fillContainerSelect(data.deployments);
			    },
			    // Handle spinner here because it's an synchronous call
			    beforeSend: function() {
			    	$('#sel-container').attr('disabled', 'disabled');
			    }
			});		
		} else {
			fillContainerSelect(deploymentData);
		}
		
		function fillContainerSelect(deploymentData) {
			$.each(deploymentData.applications, function(index, application) {
				$applicationGroup.append($('<option>').attr('value', 'application:' + application.name).text(application.name));
			});
			$.each(deploymentData.libraries, function(index, library) {
				$libraryGroup.append($('<option>').attr('value', 'library:' + library.name).text(library.name));
			});
			$.each(deploymentData.flows, function(index, flow) {
				$flowGroup.append($('<option>').attr('value', 'flow:' + flow.name).text(flow.name));
			});	
			sortSelectOptions($applicationGroup);
			sortSelectOptions($libraryGroup);
			sortSelectOptions($flowGroup);
			$('#sel-container').removeAttr('disabled');			
		}
	});
	
	$('#sel-container').change(function(event) {
		emptyContainerInfos();
		if ($(this).val() == '') {
			return;
		}
		// Add http://stackoverflow.com/questions/29063244/consistent-styling-for-nested-lists-with-bootstrap here....
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
	
	function emptyServerSelect() {
		$('#sel-server').children().slice(1).remove();
	}
	
	function emptyContainerSelect() {
		$('#sel-container-application-group').empty();
		$('#sel-container-library-group').empty();
		$('#sel-container-flow-group').empty();
	}
	
	function emptyContainerInfos() {
		$('#application_fields').empty().hide();
		$('#library_fields').empty().hide();
		$('#flows_fields').empty().hide();
	}
	
	function startsWith(text, textToStartWith) {
		return text.indexOf(textToStartWith) == 0;
	}
	
}