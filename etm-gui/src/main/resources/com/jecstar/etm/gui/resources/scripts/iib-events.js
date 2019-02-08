function buildEventPage() {
	var nodeMap = {};
	var serverMap = {}
	$('#sel-node').change(function(event) {
		event.preventDefault();
		$serverSelect = $('#sel-server');
		emptyServerSelect();
		emptyContainerSelect();
		emptyContainerInfos();
		var serverData = nodeMap[$(this).val()];
		if ('undefined' == typeof serverData) {
			$('#sel-server, #sel-container').attr('disabled', 'disabled');
			return;
		}
		$.each(serverData, function(index, server) {
			$serverSelect.append($('<option>').attr('value', server).text(server));
		});
        commons.sortSelectOptions($serverSelect);
		$serverSelect.removeAttr('disabled');
		$serverSelect.val('');
	});
	
	$('#sel-server').change(function(event) {
		event.preventDefault();
		$applicationGroup = $('#sel-container-application-group');
		$flowGroup = $('#sel-container-flow-group');
		
		emptyContainerSelect();
		emptyContainerInfos();
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
			    cache: false,
			    success: function(data) {
			        if (!data) {
			            return;
			        }
			        serverMap[$('#sel-node').val() + '_'  + $('#sel-server').val()] = data.deployments;
			        fillContainerSelect(data.deployments);
			    },
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
			$.each(deploymentData.flows, function(index, flow) {
				$flowGroup.append($('<option>').attr('value', 'flow:' + flow.name).text(flow.name));
            });
            commons.sortSelectOptions($applicationGroup);
            commons.sortSelectOptions($flowGroup);
			$('#sel-container').removeAttr('disabled');			
		}
	});
	
	$('#sel-container').change(function(event) {
		emptyContainerInfos();
		var selectedContainer = $(this).val();
		if (selectedContainer == '') {
			return;
		}
		var serverMapKey = $('#sel-node').val() + '_'  + $('#sel-server').val();
		if (startsWith(selectedContainer, 'application:')) {
			var applicationName = selectedContainer.substring(12);
			var applicationData = $.grep(serverMap[serverMapKey].applications, function(n, i) {
				return n.name === applicationName;
			})[0];
			if (applicationData) {
				$applicationFields = $('div[data-container-type="application"]');
				$applicationFields.append(
					$('<h5>').text('Application ' + applicationData.name)
				);
				$.each(applicationData.flows, function(index, flow) {
					$flowDiv = $('<div>').attr('data-flow-type', 'application.flow');
					appendFlowData($flowDiv, flow);
					$applicationFields.append(
						$('<strong>').text('Flow ' + flow.name),
						$flowDiv
					);
				});
				$applicationFields.show();
			}
		} else if (startsWith(selectedContainer, 'flow:')) {
			var flowName = selectedContainer.substring(5);
			var flowData = $.grep(serverMap[serverMapKey].flows, function(n, i) {
				return n.name === flowName;
			})[0];
			if (flowData) {
				$flowFields = $('div[data-container-type="flow"]');
				$flowFields.append(
					$('<h5>').text('Flow ' + flowData.name)
				);
				appendFlowData($flowFields, flowData);
				$flowFields.show();
			}
		}
		$('#btn-apply-events').removeAttr('disabled');
		
		function appendFlowData(container, flowData) {
			$(container).attr('data-flow-name', flowData.name);
			$(container).append(
				$('<fieldset>').addClass('form-group').append(
				    $('<label>').text('Monitoring enabled'),
				    $('<select>').attr('name', 'monitoring_active').addClass('form-control custom-select').append(
				    	$('<option>').attr('value', 'true').text('Yes'),
				    	$('<option>').attr('value', 'false').text('No')
				    ).val(flowData.monitoring_active ? 'true' : 'false')
				),
				$('<div>').addClass('row').append(
						$('<div>').addClass('col-sm-6').append($('<strong>').text('Node name')),
						$('<div>').addClass('col-sm-3').append($('<strong>').text('Node type')),
						$('<div>').addClass('col-sm-3').append($('<strong>').text('Event enabled'))
				)
			);
			$.each(flowData.nodes, function(index, node) {
				$(container).append(
						$('<div>').attr('data-node-name', node.name).addClass('row').attr('style', 'margin-top: 5px;').append(
							$('<div>').addClass('col-sm-6').text(node.name),
							$('<div>').addClass('col-sm-3').text(formateNodeType(node.type)),
							$('<div>').addClass('col-sm-3').append(
								$('<select>').attr('name', 'monitoring_set').addClass('form-control custom-select').append(
								    	$('<option>').attr('value', 'true').text('Yes'),
								    	$('<option>').attr('value', 'false').text('No')
								).val(node.monitoring_set ? 'true' : 'false')
							)
						)
				);
			});			
		}
	});
	
	$("#btn-apply-events").click(function (event) {
		event.preventDefault();
		var selectedContainer = $('#sel-container').val();
		if (selectedContainer == '') {
			return;
		}
		var objectType;
		if (startsWith(selectedContainer, 'application:')) {
			objectType = 'application'
		} else if (startsWith(selectedContainer, 'flow:')) {
			objectType = 'flow'
		}
		
		var monitoringData = createMonitoringData();
		if (monitoringData) {
			$.ajax({
			    type: 'POST',
			    contentType: 'application/json',
			    url: '../rest/iib/node/' + encodeURIComponent($('#sel-node').val()) + '/server/' + encodeURIComponent($('#sel-server').val()) + '/' + encodeURIComponent(objectType),
			    cache: false,
			    data: JSON.stringify(monitoringData),
			    success: function(data) {
			        if (!data) {
			            return;
			        }
			        $('#events_infoBox').text('Event monitoring configuration applied.').show('fast').delay(5000).hide('fast');
					var serverMapKey = $('#sel-node').val() + '_'  + $('#sel-server').val();
					var serverData = serverMap[serverMapKey];
					if ('application' == objectType) {
						$.each(serverData.applications, function (index, application) {
							if (application.name == monitoringData.name) {
								serverData.applications[index] = monitoringData;
								return false;
							}
						});
					} else if ('flow' == objectType) {
						$.each(serverData.flows, function (index, flow) {
							if (flow.name == monitoringData.name) {
								serverData.flows[index] = monitoringData;
								return false;
							}
						});						
					}
			    },
			    beforeSend: function() {
			    	$('#sel-node, #sel-server, #sel-container, select[name="monitoring_active"], select[name="monitoring_set"], #btn-apply-events').attr('disabled', 'disabled');
			    },
			    complete: function () {
			    	$('#sel-node, #sel-server, #sel-container, select[name="monitoring_active"], select[name="monitoring_set"], #btn-apply-events').removeAttr('disabled');
			    }
			});			
		}
	});
	
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
				$.ajax({
				    type: 'GET',
				    contentType: 'application/json',
				    url: '../rest/iib/node/' + encodeURIComponent(node.name) + '/servers',
				    cache: false,
				    success: function(serverData) {
				        if (!serverData) {
				            return;
				        }
				        nodeMap[node.name] = serverData.servers;
				    }
				});
	        });
            commons.sortSelectOptions($nodeSelect);
	        $nodeSelect.val('');
	    }
	});
	
	function emptyServerSelect() {
		$('#sel-server').children().slice(1).remove();
	}
	
	function emptyContainerSelect() {
		$('#sel-container-application-group').empty();
		$('#sel-container-flow-group').empty();
	}
	
	function emptyContainerInfos() {
		$('#btn-apply-events').attr('disabled', 'disabled');
		$('div[data-container-type]').empty().hide();
	}
	
	function startsWith(text, textToStartWith) {
		return text.indexOf(textToStartWith) == 0;
	}
	
	function formateNodeType(nodeType) {
		if ('ComIbmMQInputNode' == nodeType) {
			return 'MQ Input'
		} else if ('ComIbmMQOutputNode' == nodeType) {
			return 'MQ Output'
		} else if ('ComIbmMQGetNode' == nodeType) {
			return 'MQ Get'
		} else if ('ComIbmMQReply' == nodeType) {
			return 'MQ Reply'
		} else if ('ComIbmSOAPInputNode' == nodeType) {
			return 'SOAP Input'
		} else if ('ComIbmSOAPReplyNode' == nodeType) {
			return 'SOAP Reply'
		}
		return nodeType
	}
	
	function createMonitoringData() {
		var serverMapKey = $('#sel-node').val() + '_'  + $('#sel-server').val();
		var selectedContainer = $('#sel-container').val();
		if (selectedContainer == '') {
			return;
		}
		if (startsWith(selectedContainer, 'application:')) {
			var applicationName = selectedContainer.substring(12);
			// Clone the current data.
			var applicationData = $.extend(true, {}, $.grep(serverMap[serverMapKey].applications, function(n, i) {
				return n.name === applicationName;
			})[0]);
			
			if (applicationData) {
				$.each(applicationData.flows, function (index, flow) {
					setFlowMonitoringData(flow, 'application.flow');
				});
				return applicationData;
			}
		} else if (startsWith(selectedContainer, 'flow:')) {
			var flowName = selectedContainer.substring(5);
			// Clone the current data.
			var flowData =$.extend(true, {}, $.grep(serverMap[serverMapKey].flows, function(n, i) {
				return n.name === flowName;
			})[0]);
			if (flowData) {
				setFlowMonitoringData(flowData);
				return flowData;
			}
		}
	}
	
	function setFlowMonitoringData(flow, flowType) {
		$flowDiv = $($.grep($("div[data-flow-name]"), function(n, i) {
			return $(n).attr('data-flow-name') === flow.name && $(n).attr('data-flow-type') === flowType;
		})[0]);
		flow.monitoring_active = $flowDiv.find('select[name="monitoring_active"]').val() == 'true' ? true : false;
		$.each(flow.nodes, function (index, node) {
			var nodeDiv = $.grep($flowDiv.find("div[data-node-name]"), function(n, i) {
				return $(n).attr('data-node-name') === node.name;
			})[0];
			node.monitoring_set = $(nodeDiv).find('select[name="monitoring_set"]').val() == 'true' ? true : false;
		});
	}
	
}