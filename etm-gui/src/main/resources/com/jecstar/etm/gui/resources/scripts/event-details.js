var cyEndpoints; 

function showEvent(id, type, index) {
	var scrollTo =  $(window).scrollTop();
	$('#search-container').hide();
	$eventTab = $('#event-tab');
	$eventTab.empty();	
	$eventTab.tab('show');
	$('#event-tab-header').addClass('active').attr('area-expanded', 'true');
	$('#endpoint-tab-header').remove();
	$('#endpoint-tab').remove();
	if (cyEndpoints) {
		cyEndpoints.destroy();
	}
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/search/event/' + encodeURIComponent(index) + '/' + encodeURIComponent(type) + '/' + encodeURIComponent(id),
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        addContent(data);
	    }
	});
	
	$('#btn-back-to-results, #link-back-to-results')
		.unbind('click')
		.click(function(event) {
			event.preventDefault();
			$('#event-container').hide();
			$('#search-container').show();
			$('html,body').animate({scrollTop: scrollTo},'fast');
    });
	$('#event-container').show();
	
}

function addContent(data) {
	$('#event-card-title').text('Event ' + data.id);
	$('#event-tab-header').text(capitalize(data.type));
	$eventTab = $('#event-tab');
	if (data.source) {
		if (data.source.name) {
			$('#event-card-title').text('Event ' + data.source.name);
		}
		$eventTab.append(createDetailRow('Id', data.id, 'Name', data.source.name));
		$eventTab.append(createDetailRow('Correlation id', data.source.correlation_id, 'Payload format', data.source.payload_format));
		$endpoints = $(data.source.endpoints);
		var writing_times = $endpoints.map(function () {return this.writing_endpoint_handler.handling_time}).get();
		if ('log' === data.type) {
			$eventTab.append(createDetailRow('Write time', moment.tz(Math.min.apply(Math, writing_times), data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'), 'Log level', data.source.log_level));
		} else {
			$eventTab.append(createDetailRow('Write time', moment.tz(Math.min.apply(Math, writing_times), data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ')));
		}
		if (data.source.metadata != undefined) {
        	$eventTab.append(createDetailMap('metadata', data.source.metadata));
        }
        if (data.source.extracted_data != undefined) {
        	$eventTab.append(createDetailMap('extracts', data.source.extracted_data));
        }
        
        $eventTab.append(
        		$('<div>').addClass('row').append(
        				$('<div>').addClass('col-sm-12').append(
        						$('<pre>').attr('style', 'white-space: pre-wrap;').text(data.source.payload)
        				)
        		)
        );
        
        if ('log' === data.type && data.source.stack_trace != undefined) {
        	$eventTab.append(
	    		$('<div>').addClass('row').append(
	    				$('<div>').addClass('col-sm-12').append(
	    						$('<pre>').attr('style', 'white-space: pre-wrap;').text(data.source.stack_trace)
	    				)
	    		)
    		);
        }
        
		
		if ($endpoints != undefined) {
			createEndpointsTab($endpoints, data.time_zone);
		}

	}
}

function createDetailRow(name1, value1, name2, value2) {
	return $('<div>').addClass('row')
		.append(
		    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold form-control-static').text(name1)),
		    $('<div>').addClass('col-md-4').attr('style', 'word-wrap: break-word;').append($('<p>').addClass('form-control-static').text(value1)),
		    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold form-control-static').text(name2)),
		    $('<div>').addClass('col-md-4').attr('style', 'word-wrap: break-word;').append($('<p>').addClass('form-control-static').text(value2))		    	
		);
}

function createDetailMap(name, valueMap) {
	$detailMap = $('<div>').addClass('panel panel-default').append(
			$('<div>').addClass('panel-heading clearfix').append(
					$('<div>').addClass('pull-left').append($('<a>').addClass('font-weight-bold').attr('href', '#').text(capitalize(name)).click(function (event) {
						event.preventDefault();
						$('#event_' + name + '_panel_collapse').collapse('toggle');
					}))
			),
			$('<div>').attr('id', 'event_' + name + '_panel_collapse').addClass('panel-collapse collapse').append(
					$('<div>').addClass('panel-body').append(
							$('<div>').addClass('table-responsive').append(
									$('<table>').addClass('table table-sm table-striped table-hover').append(
											$('<thead>').append($('<tr>').append($('<th>').attr('style', 'padding: 0.1rem;').text('Name')).append($('<th>').attr('style', 'padding: 0.1rem;').text('Value')))
									).append(function () {
										$tbody = $('<tbody>');
							            $.each(valueMap, function(key, value) {
							            	if (key.endsWith('_as_number') || key.endsWith('_as_boolean') || key.endsWith('_as_date')) {
							            		return true;
							            	}
							            	$tbody.append(
							            			$('<tr>').append(
							            					$('<td>').attr('style', 'padding: 0.1rem;').text(key),
							            					$('<td>').attr('style', 'padding: 0.1rem;').text(value)
							            			)
							            	);
							            });
										return $tbody;
									})
							)
					)
			)
	);
	return $detailMap;
}


function createEndpointsTab(endpoints, timeZone) {
	$('#event-tabs').append(
			$('<li>').addClass('nav-item').append(
					$('<a>').attr('id', 'endpoint-tab-header')
						.attr('aria-expanded', 'false')
						.attr('role', 'tab')
						.attr('data-toggle', 'tab')
						.attr('href', '#endpoint-tab')
						.addClass('nav-link')
						.text('Endpoints')
			)
	);

	// Sort endpoints on writing time
	endpoints.sort(function (ep1, ep2) {
		return ep1.writing_endpoint_handler.handling_time - ep2.writing_endpoint_handler.handling_time;
	});
	
	var nodesData = [];
	var edgesData = [];
	var rowIx = -1;
	$.each(endpoints, function (index, endpoint) {
		rowIx++;
		var endpointId = rowIx + '-1';
		if (endpoint.writing_endpoint_handler.application) {
			var name = endpoint.writing_endpoint_handler.application.name ? endpoint.writing_endpoint_handler.application.name : '?';
			nodesData.push({
				data: {
					id: rowIx + '-0',
					name: name,
					shape: 'roundrectangle',
					width: 'label',
					color: '#ffffff',
					background_color: '#777',
					row: rowIx,
					col: 0,
					writing_endpoint_handler: endpoint.writing_endpoint_handler,
					time_zone: timeZone
				}
			});
			edgesData.push({ data: { source: rowIx + '-0', target: endpointId } });
		}
		nodesData.push({
			data: {
				id: endpointId,
				name: endpoint.name,
				shape: 'roundrectangle',
				width: 'label',
				color: '#ffffff',
				background_color: '#8fbc8f',
				row: rowIx,
				col: 1,
				endpoint: endpoint,
				time_zone: timeZone
			}
		});
		if (endpoint.reading_endpoint_handlers) {
			endpoint.reading_endpoint_handlers.sort(function (han1, han2) {
				return han1.handling_time - han2.handling_time;
			});
			$.each(endpoint.reading_endpoint_handlers, function (repIx, rep) {
				if (repIx !== 0) {
					rowIx++;
				}
				var name = rep.application.name ? rep.application.name : '?';
				nodesData.push({
					data: {
						id: rowIx + '-2',
						name: name,
						shape: 'roundrectangle',
						width: 'label',
						color: '#ffffff',
						background_color: '#777',
						row: rowIx,
						col: 2,
						reading_endpoint_handler: rep,
						time_zone: timeZone
					}
				});
				edgesData.push({ data: { source: endpointId, target: rowIx + '-2' } });
			});
		}
	}); 
	
	$('#tabcontents').append(
			$('<div>').attr('id', 'endpoint-tab')
				.attr('aria-labelledby', 'endpoint-tab-header')
				.attr('role', 'tabpanel')
				.attr('aria-expanded', 'false')
				.addClass('tab-pane fade')
				.append(
						$('<div>').attr('id', 'endpoint-node-detail').append(
								$('<div>').addClass('row').append(
										$('<div>').addClass('col-sm-12').append(
												$('<p>').addClass('text-xs-center').text('Click on a node to view more details')
										)		
								)		
						),
						$('<div>').addClass('row').append(
								$('<div>').attr('id', 'endpoint-overview').attr('style', 'height: ' + (rowIx + 1) * 3 + 'em; width: 100%;')
						)
				) 
	);
	$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
		var target = $(e.target).attr("href") // activated tab
		if (target == '#endpoint-tab' && !$('#endpoint-overview > div > canvas').length) {
			cyEndpoints = cytoscape({
				  container: document.querySelector('#endpoint-overview'),
				  zoomingEnabled: false,
				  panningEnabled: true,
				  boxSelectionEnabled: false,
				  autoungrabify: true, 
				  
				  style: cytoscape.stylesheet()
				    .selector('node')
				      .css({
				        'content': 'data(name)',
				        'shape': 'data(shape)',
				        'width': 'data(width)',
				        'text-valign': 'center',
				        'color': 'data(color)',
				        'background-color': 'data(background_color)'
				      })
				    .selector('edge')
				      .css({
				    	'width': 2,
				        'target-arrow-shape': 'triangle'
				      }),				  
				  elements: {
				    nodes: nodesData,
				    edges: edgesData
				  },
				  layout: {
					  name: 'grid',
					  condense: false,
					  cols: 3,
					  rows: rowIx + 1,
					  position: function( node ){
						  return {row: node.data('row'), col: node.data('col')} 
					  }
				  }
				});
			cyEndpoints.on('tap', function(event) {
				  var evtTarget = event.cyTarget;
				  if( evtTarget !== cyEndpoints ){
					  if (evtTarget.data('writing_endpoint_handler')) {
						  displayWritingEndpointHandler(cyEndpoints, evtTarget.data('writing_endpoint_handler'), evtTarget.data('time_zone'));
					  } else if (evtTarget.data('endpoint')) {
						  displayEndpoint(cyEndpoints, evtTarget.data('endpoint'), evtTarget.data('time_zone'));
					  } else if (evtTarget.data('reading_endpoint_handler')) {
						  displayReadingEndpointHandler(cyEndpoints, evtTarget.data('reading_endpoint_handler'), evtTarget.data('time_zone'));
					  }
				  }
			});
		}
	});
}

function displayWritingEndpointHandler(cyEndpoints, endpoint_handler, timeZone) {
	$('#endpoint-node-detail').fadeOut('fast', function () {
		$this = $(this).empty();
		var eh = formatEndpointHandler(endpoint_handler, timeZone);
		$this.append(createDetailRow('Write time', eh.handling_time, 'Transaction id', eh.transaction_id));
		$this.append(createDetailRow('Location', eh.location, 'Application name', eh.application_name));
		$this.append(createDetailRow('Application version', eh.application_version, 'Application instance', eh.application_instance));
		$this.append(createDetailRow('Application user', eh.application_principal, 'Application address', eh.application_host));
		$this.append('<br>');
		$this.fadeIn('fast', function () {
			cyEndpoints.resize();
		});
	});
}

function displayEndpoint(cyEndpoints, endpoint, timeZone) {
	$('#endpoint-node-detail').fadeOut('fast', function () {
		$this = $(this).empty();
		$this.append(createDetailRow('Write time', moment.tz(endpoint.writing_endpoint_handler.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'), 'Endpoint name', endpoint.name));
		$this.append('<br>');
		$this.hide().fadeIn('fast', function () {
			cyEndpoints.resize();
		});
	});
}

function displayReadingEndpointHandler(cyEndpoints, endpoint_handler, timeZone) {
	$('#endpoint-node-detail').fadeOut('fast', function () {
		$this = $(this).empty();
		var eh = formatEndpointHandler(endpoint_handler, timeZone);
		$this.append(createDetailRow('Read time', eh.handling_time, 'Transaction id', eh.transaction_id));
		$this.append(createDetailRow('Location', eh.location, 'Application name', eh.application_name));
		$this.append(createDetailRow('Application version', eh.application_version, 'Application version', eh.application_instance));
		$this.append(createDetailRow('Application user', eh.application_principal, 'Application address', eh.application_host));
		$this.append('<br>');
		$this.hide().fadeIn('fast', function () {
			cyEndpoints.resize();
		});
	});
}

function formatEndpointHandler(endpoint_handler, timeZone) {
	var flat = {
		handling_time: undefined,
		transaction_id: endpoint_handler.transaction_id,
		application_name: undefined,
		application_version: undefined,
		application_instance: undefined,
		application_principal: undefined,
		application_host: undefined,
		location: undefined,
	};
	if (endpoint_handler.handling_time) {
		flat.handling_time = moment.tz(endpoint_handler.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ');
	}
	if (endpoint_handler.application) {
		flat.application_name = endpoint_handler.application.name; 
		flat.application_version = endpoint_handler.application.version; 
		flat.application_instance = endpoint_handler.application.instance; 
		flat.application_principal = endpoint_handler.application.principal;
		if (endpoint_handler.application.host_name) {
			flat.application_host = endpoint_handler.application.host_name;
			if (endpoint_handler.application.host_address && endpoint_handler.application.host_address !== endpoint_handler.application.host_name) {
				flat.application_host += ' (' + endpoint_handler.application.host_address + ')'
			}
		} else {
			flat.application_host = endpoint_handler.application.host_address;
		}
	}
	if (endpoint_handler.location) {
		flat.location = convertDDToDMS(endpoint_handler.location.lat, false) + ' ' + convertDDToDMS(endpoint_handler.location.lon, true); 
	}
	
	function convertDDToDMS(D, lng){
	    var dms =  {
	        dir : D<0?lng?'W':'S':lng?'E':'N',
	        deg : 0|(D<0?D=-D:D),
	        min : 0|D%1*60,
	        sec :(0|D*60%1*6000)/100
	    };
	    return dms.deg + '°' + dms.min + '\'' + dms.sec + '" ' + dms.dir
	}
	
	return flat;
}

function capitalize(text) {
	if (!text) {
		return text;
	}
	return text.charAt(0).toUpperCase() + text.toLowerCase().slice(1);
}

