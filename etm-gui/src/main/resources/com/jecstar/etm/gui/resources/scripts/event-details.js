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
		$('#event_metadata_table > tbody').empty();
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
	}
}

$('#event_metadata_panel_collapse').collapse();
$('#event_metadata_toggle').click(function (e) {
   e.preventDefault();
   $('#event_metadata_panel_collapse').collapse("toggle");
});

function capitalize(text) {
	if (!text) {
		return text;
	}
	return text.charAt(0).toUpperCase() + text.toLowerCase().slice(1);
}