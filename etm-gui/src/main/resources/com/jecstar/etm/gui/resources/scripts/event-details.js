var cyEndpoints; 

function showEvent(type, id) {
	var scrollTo =  $(window).scrollTop();
	$('#search-container').hide();
	$('#event-tabs').children().slice(1).remove();
	$('#tabcontents').children().slice(1).remove();
	$eventTab = $('#event-tab');
	$eventTab.empty();	
	$eventTab.tab('show');
	$('#event-tab-header').addClass('active').attr('area-expanded', 'true');
	if (cyEndpoints) {
		cyEndpoints.destroy();
	}
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/search/event/' + encodeURIComponent(type) + '/' + encodeURIComponent(id),
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
			$('#event-card-title').text(data.source.name);
		}
		writeEventDataToTab($eventTab, $('#event-tab-header'), data, false);
		var $endpoints = $(data.source.endpoints);
		if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
			createEndpointsTab($endpoints, data.time_zone);
		}

	}
}

function writeEventDataToTab(tab, tabHeader, data, correlated) {
	$eventTab = $(tab);
	$tabHeader = $(tabHeader);
	appendToContainerInRow($eventTab, 'Id', data.id);
	appendToContainerInRow($eventTab, 'Name', data.source.name);
	appendToContainerInRow($eventTab, 'Correlation id', data.source.correlation_id);
	appendToContainerInRow($eventTab, 'Payload format', data.source.payload_format);
	var $endpoints = $(data.source.endpoints);
	if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
		var writing_times = $endpoints.map(function () {return this.writing_endpoint_handler.handling_time}).get();
		appendToContainerInRow($eventTab, 'First write time', moment.tz(Math.min.apply(Math, writing_times), data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
	}
	appendToContainerInRow($eventTab, 'Messaging type', data.source.messaging_type);
	appendToContainerInRow($eventTab, 'Expiry time', moment.tz(data.source.expiry, data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
	if ('log' === data.type) {
		appendToContainerInRow($eventTab, 'Log level', data.source.log_level);
	} else if ('http' === data.type) {
		if (data.source.http_type) {
			// http type known, determine request or response.
			if ('RESPONSE' === data.source.http_type) {
				$tabHeader.text('Http response');
			} else {
				$tabHeader.text('Http request');
			}
		}
		appendToContainerInRow($eventTab, 'Http type', data.source.http_type);
		if ('RESPONSE' !== data.source.http_type) {
			appendToContainerInRow($eventTab, 'Expiry time', moment.tz(data.source.expiry, data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
		}
	} else if ('messaging' === data.type) {
		if (data.source.messaging_type) {
			// messaging type known, determine request or response.
			if ('RESPONSE' === data.source.messaging_type) {
				$tabHeader.text('Response message');
				if (data.source.correlation_id && !correlated) {
					addCorrelationTab(data.type, data.source.correlation_id, 'request-tab', 'Request message');
				}
			} else if ('REQUEST' === data.source.messaging_type) {
				$tabHeader.text('Request message');
				if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
					var response_times = $endpoints.map(function () {return this.writing_endpoint_handler.response_time}).get();
					if (response_times) {
						appendToContainerInRow($eventTab, 'Highest writer response time', response_times.length > 0 ? Math.max.apply(Math, response_times) : null);
					}
					response_times = $endpoints.map(function () {
						if (this.reading_endpoint_handlers) {
							return $(this.reading_endpoint_handlers).map(function () {
								return this.response_time;
							}).get();
						}
					}).get();
					if (response_times) {
						appendToContainerInRow($eventTab, 'Highest reader response time', response_times.length > 0 ? Math.max.apply(Math, response_times) : null);
					}
				}
				if (data.source.correlations && !correlated) {
					$.each(data.source.correlations, function (index, correlation_id) {
						addCorrelationTab(data.type, correlation_id, 'response-tab' + index, 'Response message');
					});
				}
			} else {
				$tabHeader.text('Fire-forget message');
			}
		}
	} else if ('sql' === data.type) {
	}
	if ("undefined" != typeof data.source.metadata) {
    	$eventTab.append(createDetailMap('metadata', data.source.metadata));
    }
    if ("undefined" != typeof data.source.extracted_data) {
    	$eventTab.append(createDetailMap('extracts', data.source.extracted_data));
    }
    $eventTab.append(
    		$('<div>').addClass('row').append(
    				$('<div>').addClass('col-sm-12').append(
    						$('<pre>').attr('style', 'white-space: pre-wrap;').text(data.source.payload)
    				)
    		)
    );
    
    if ('log' === data.type && "undefined" != typeof data.source.stack_trace) {
    	$eventTab.append(
    		$('<div>').addClass('row').append(
    				$('<div>').addClass('col-sm-12').append(
    						$('<pre>').attr('style', 'white-space: pre-wrap;').text(data.source.stack_trace)
    				)
    		)
		);
    }
	
}

function addCorrelationTab(type, id, tabType, tabName) {
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/search/event/' + encodeURIComponent(type) + '/' + encodeURIComponent(id),
	    success: function(correlation_data) {
	        if (!correlation_data) {
	            return;
	        }
			$('#event-tabs').children().eq(0).after(
				$('<li>').addClass('nav-item').append(
					$('<a>').attr('id',  tabType + '-header')
						.attr('aria-expanded', 'false')
						.attr('role', 'tab')
						.attr('data-toggle', 'tab')
						.attr('href', '#' + tabType)
						.addClass('nav-link')
						.text(tabName)
				)
			);
			$('#tabcontents').children().eq(0).after(
				$('<div>').attr('id', tabType)
					.attr('aria-labelledby', tabType + '-header')
					.attr('role', 'tabpanel')
					.attr('aria-expanded', 'false')
					.addClass('tab-pane fade')
			);
	        writeEventDataToTab($('#' + tabType), $('#' + tabType + '-header'), correlation_data, true);
	    }
	});
}

function appendToContainerInRow(container, name, value) {
	var $container = $(container);
	var $row = $container.children(":last-child");
	if ($row.children().length > 0 && $row.children().length <= 2) {
		$row.append(
		    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold form-control-static').text(name)),
		    $('<div>').addClass('col-md-4').attr('style', 'word-wrap: break-word;').append($('<p>').addClass('form-control-static').text(value))
		);
	} else {
		$container.append(
				$('<div>').addClass('row')
					.append(
					    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold form-control-static').text(name)),
					    $('<div>').addClass('col-md-4').attr('style', 'word-wrap: break-word;').append($('<p>').addClass('form-control-static').text(value))
				)
		);
	}
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
				name: endpoint.name ? endpoint.name : '?',
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
		appendToContainerInRow($this, 'Write time', eh.handling_time);
		appendToContainerInRow($this, 'Response time', eh.response_time);
		appendToContainerInRow($this, 'Transaction id', eh.transaction_id);
		appendToContainerInRow($this, 'Location', eh.location);
		appendToContainerInRow($this, 'Application name', eh.application_name);
		appendToContainerInRow($this, 'Application version', eh.application_version);
		appendToContainerInRow($this, 'Application instance', eh.application_instance);
		appendToContainerInRow($this, 'Application user', eh.application_principal);
		appendToContainerInRow($this, 'Application address', eh.application_host);
		$this.append('<br>');
		$this.fadeIn('fast', function () {
			cyEndpoints.resize();
		});
	});
}

function displayEndpoint(cyEndpoints, endpoint, timeZone) {
	$('#endpoint-node-detail').fadeOut('fast', function () {
		$this = $(this).empty();
		appendToContainerInRow($this, 'Write time', moment.tz(endpoint.writing_endpoint_handler.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
		appendToContainerInRow($this, 'Endpoint name', endpoint.name);
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
		appendToContainerInRow($this, 'Read time', eh.handling_time);
		appendToContainerInRow($this, 'Response time', eh.response_time);
		appendToContainerInRow($this, 'Transaction id', eh.transaction_id);
		appendToContainerInRow($this, 'Location', eh.location);
		appendToContainerInRow($this, 'Application name', eh.application_name);
		appendToContainerInRow($this, 'Application version', eh.application_version);
		appendToContainerInRow($this, 'Application instance', eh.application_instance);
		appendToContainerInRow($this, 'Application user', eh.application_principal);
		appendToContainerInRow($this, 'Application address', eh.application_host);
		appendToContainerInRow($this, 'Latency', eh.latency);
		$this.append('<br>');
		$this.hide().fadeIn('fast', function () {
			cyEndpoints.resize();
		});
	});
}

function formatEndpointHandler(endpoint_handler, timeZone) {
	var flat = {
		handling_time: undefined,
		latency: endpoint_handler.latency,
		response_time: endpoint_handler.response_time,
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

