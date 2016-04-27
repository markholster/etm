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
			$('#search-container').show('fast');
		    $('html,body').animate({scrollTop: scrollTo},'fast');
    });
	$('#event-container').show('fast');
	
}

function addContent(data) {
	$('#event-card-title').text('Event ' + data.id);
	$('#event-tab-header').text(capitalize(data.type));
	if (data.source) {
		if (data.source.name) {
			$('#event-card-title').text('Event ' + data.source.name);
		}
		$('#event_id').text(data.id);
		$('#event_name').text(data.source.name);
		$('#event_correlation_id').text(data.source.correlation_id);
		$('#event_transaction_id').text(data.source.transaction_id);
		$('#event_payload_format').text(capitalize(data.source.payload_format));
		$('#event_content').text(data.source.payload);
		var writing_times = $(data.source.endpoints).map(function () {return this.writing_endpoint_handler.handling_time}).get();
		$('#event_write_time').text(moment.tz(Math.min.apply(Math, writing_times), data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
		
		
		$('#event_metadata_table > tbody').empty();
		$('#event_extracted_data_table > tbody').empty();
        if (data.source.metadata != undefined) {
            $.each(data.source.metadata, function(key, value) {
            	if (key.endsWith('_as_number') || key.endsWith('_as_boolean') || key.endsWith('_as_date')) {
            		return true;
            	}
                var rowContent = '<tr><td style="padding: 0.1rem">' + escapeToHtml(key) + '</td><td style="padding: 0.1rem">' + escapeToHtml(value) + '</td></tr>';
                $(rowContent).appendTo($('#event_metadata_table > tbody'));
            });
            $('#event_metadata_panel').show();
        } else {
            $('#event_metadata_panel').hide();
        }
        if (data.source.extracted_data != undefined) {
        	$.each(data.source.extracted_data, function(key, value) {
        		if (key.endsWith('_as_number') || key.endsWith('_as_boolean') || key.endsWith('_as_date')) {
        			return true;
        		}
        		var rowContent = '<tr><td style="padding: 0.1rem">' + escapeToHtml(key) + '</td><td style="padding: 0.1rem">' + escapeToHtml(value) + '</td></tr>';
        		$(rowContent).appendTo($('#event_extracted_data_table > tbody'));
        	});
        	$('#event_extracted_data_panel').show();
        } else {
        	$('#event_extracted_data_panel').hide();
        }
	}
}

function capitalize(text) {
	if (!text) {
		return text;
	}
	return text.charAt(0).toUpperCase() + text.toLowerCase().slice(1);
}

$('#event_metadata_panel_collapse').collapse();
$('#event_metadata_toggle').click(function (e) {
   e.preventDefault();
   $('#event_metadata_panel_collapse').collapse("toggle");
});

$('#event_extracted_data_panel_collapse').collapse();
$('#event_extracted_data_toggle').click(function (e) {
   e.preventDefault();
   $('#event_extracted_data_panel_collapse').collapse("toggle");
});

