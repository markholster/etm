function showEvent(id, type, index) {
	var scrollTo =  $(window).scrollTop();
	$('#search-container').hide();
	$eventTab = $('#event-tab');
	$eventTab.empty();	
	$eventTab.tab('show');
	$('#event-tab-header').addClass('active').attr('area-expanded', 'true');
	$('#endpoint-tab-header').remove();
	$('#endpoint-tab').remove();
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
	
	$('#btn-back-to-results')
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
		
		if ($endpoints != undefined) {
			createEndpointsTab($endpoints);
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
    		$('<div>').addClass('row').append(
    				$('<div>').addClass('col-sm-12').append(
    						$('<pre>').attr('style', 'white-space: pre-wrap;').text(data.source.stack_trace)
    				)
    		);        	
        }
	}
}

function createDetailRow(name1, value1, name2, value2) {
	return $('<div>').addClass('row')
		.append(
		    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold').text(name1)),
		    $('<div>').addClass('col-md-4').append($('<p>').addClass('form-control-static').attr('style', 'padding-bottom: 0px;').text(value1)),
		    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold').text(name2)),
		    $('<div>').addClass('col-md-4').append($('<p>').addClass('form-control-static').attr('style', 'padding-bottom: 0px;').text(value2))		    	
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


function createEndpointsTab(endpoints) {
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
					col: 0
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
				col: 1
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
						col: 2
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
						$('<div>').addClass('row').append(
								$('<div>').addClass('col-sm-12').append($('<p>').text('Click on a node to view more details'))
						),
						$('<div>').addClass('row').append(
								$('<div>').attr('id', 'endpoint-overview').attr('style', 'height: ' + (rowIx + 1) * 4 + 'em; width: 100%;')
						)		
				) 
	);
	$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
		var target = $(e.target).attr("href") // activated tab
		if (target == '#endpoint-tab' && !$('#endpoint-overview > div > canvas').length) {
			var cy = cytoscape({
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
				    	'width': 5,
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
			cy.on('tap', function(event) {
				  var evtTarget = event.cyTarget;
				  if( evtTarget !== cy ){
					  // TODO add info on click.
					  alert(evtTarget.data('col'));
				  }
			});
		}
	});
}

function capitalize(text) {
	if (!text) {
		return text;
	}
	return text.charAt(0).toUpperCase() + text.toLowerCase().slice(1);
}

