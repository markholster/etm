function showEvent(id, type, index) {
	var scrollTo =  $(window).scrollTop();
	$('#search-container').hide();
	$eventTab = $('#event-tab');
	$eventTab.empty();	
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
			$eventTab.append(createEndpointsMap($endpoints));
			$(function() {
			cytoscape({
				  container: document.querySelector('#endpoint_content'),
				    
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
				    nodes: [
				      { data: { id: 'app1', name: 'Enterprise Telemetry Monitor', shape: 'roundrectangle', width: 215, color: '#ffffff', background_color: '#777' } },
				      { data: { id: 'endpoint1', name: 'QUEUE.IN', shape: 'roundrectangle', width: 85, color: '#ffffff', background_color: '#8FBC8F' } },
				      { data: { id: 'app2', name: 'App 2', shape: 'roundrectangle', width: 80, color: '#ffffff', background_color: '#777' } },
				      { data: { id: 'app3', name: 'App 3', shape: 'roundrectangle', width: 80, color: '#ffffff', background_color: '#777' } }
				    ],
				    edges: [
				      { data: { source: 'app1', target: 'endpoint1' } },
				      { data: { source: 'endpoint1', target: 'app2' } },
				      { data: { source: 'endpoint1', target: 'app3' } }
				    ]
				  },
				  layout: {
					  name: 'grid',
					  cols: 3, 
					  position: function( node ){
						  if ('app1' === node.data('id')) {
							  return {row: 0, col: 0}
						  } else if ('endpoint1' === node.data('id')) {
							  return {row: 0, col: 1}
						  } else if ('app2' === node.data('id')) {
							  return {row: 0, col: 2}
						  } else if ('app3' === node.data('id')) {
							  return {row: 1, col: 2}
						  }
					  },
					  padding: 10
				  }
				});
			});
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

function createEndpointsMap(endpoints) {
	$endpointsMap = $('<div>').addClass('panel panel-default').append(
			$('<div>').addClass('panel-heading clearfix').append(
					$('<div>').addClass('pull-left').append($('<a>').addClass('font-weight-bold').attr('href', '#').text('Endpoints').click(function (event) {
						event.preventDefault();
						$('#endpoints_panel_collapse').collapse('toggle');
					}))
			),
			$('<div>').attr('id', 'endpoints_panel_collapse').addClass('panel-collapse collapse in').append(
					$('<div>').attr('id', 'endpoint_content').addClass('panel-body').attr('style', 'height: 6rem;')
			)
	);
	return $endpointsMap;
	
}

function capitalize(text) {
	if (!text) {
		return text;
	}
	return text.charAt(0).toUpperCase() + text.toLowerCase().slice(1);
}

