function showEvent(id, type, index) {
	var scrollTo =  $(window).scrollTop();
	$('#search-container').hide();
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
	$eventTab.empty();
	if (data.source) {
		if (data.source.name) {
			$('#event-card-title').text('Event ' + data.source.name);
		}
		$eventTab.append(createDetailRow('Id', data.id, 'Name', data.source.name));
		$eventTab.append(createDetailRow('Correlation id', data.source.correlation_id, 'Payload format', data.source.payload_format));
		var writing_times = $(data.source.endpoints).map(function () {return this.writing_endpoint_handler.handling_time}).get();
		$eventTab.append(createDetailRow('Transaction id', data.source.transaction_id, 'Write time', moment.tz(Math.min.apply(Math, writing_times), data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ')));
		
		if ('log' === data.type) {
			$eventTab.append(createDetailRow('Log level', data.source.log_level, '', ''));
		}
		
		if (data.source.endpoints != undefined) {
			$eventTab.append(createEndpointsMap(data.source.endpoints));
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
					$('<div>').addClass('panel-body').append(
							$('<p>').text('Endpoint panel')
					)
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

