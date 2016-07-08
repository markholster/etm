function buildEndpointPage() {
	var endpointMap = {};
	$('#sel-endpoint').change(function(event) {
		event.preventDefault();
		var endpointData = endpointMap[$(this).val()];
		if ('undefined' == typeof endpointData) {
			resetValues();
			return;
		}
		$('#input-endpoint-name').val(getEndpointNameById(endpointData.name));
		enableOrDisableButtons();
	});
	
	$('#btn-confirm-save-endpoint').click(function(event) {
		if (!document.getElementById('endpoint_form').checkValidity()) {
			return;
		}
		event.preventDefault();
		var endpointName = $('#input-endpoint-name').val();
		if (isEndpointExistent(endpointName)) {
			$('#overwrite-endpoint-name').text(endpointName);
			$('#modal-endpoint-overwrite').modal();
		} else {
			saveEndpoint();
		}
	});
	
	$('#btn-save-endpoint').click(function(event) {
		saveEndpoint();
	});
	
	$('#btn-confirm-remove-endpoint').click(function(event) {
		event.preventDefault();
		$('#remove-endpoint-name').text($('#input-endpoint-name').val());
        $('#modal-endpoint-remove').modal();
	});	

	$('#btn-remove-endpoint').click(function(event) {
		removeEndpoint($('#input-endpoint-name').val());
	});

	
	$('#input-endpoint-name').on('input', enableOrDisableButtons);
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/endpoints',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        $endpointSelect = $('#sel-endpoint');
	        $.each(data.endpoints, function(index, endpoint) {
	        	$endpointSelect.append($('<option>').attr('value', endpoint.name).text(getEndpointNameById(endpoint.name)));
	        	endpointMap[endpoint.name] = endpoint;
	        });
	        sortSelectOptions($endpointSelect)
	        $endpointSelect.val('');
	    }
	});
	
	function sortSelectOptions($endpointSelect) {
		var options = $endpointSelect.children('option');
		options.detach().sort(function(a,b) {
		    var at = $(a).text();
		    var bt = $(b).text();   
		    if ('' == at) {
		    	return -1;
		    } else if ('' == bt) {
		    	return 1;
		    }
		    if ('*' == at) {
		    	return -1;
		    } else if ('*' == bt) {
		    	return 1;
		    }
		    return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
		});
		options.appendTo($endpointSelect);
	}
	
	function enableOrDisableButtons() {
		var endpointName = $('#input-endpoint-name').val();
		if (endpointName) {
			$('#btn-confirm-save-endpoint').removeAttr('disabled');
			if (isEndpointExistent(endpointName) && !'*' == endpointName) {
				$('#btn-confirm-remove-endpoint').removeAttr('disabled');
			} else {
				$('#btn-confirm-remove-endpoint').attr('disabled', 'disabled');
			}
		} else {
			$('#btn-confirm-save-endpoint, #btn-confirm-remove-endpoint').attr('disabled', 'disabled');
		}
	}
	
	function isEndpointExistent(name) {
		return "undefined" != typeof endpointMap[getEndpointIdByName(name)];
	}
	
	function saveEndpoint() {
		var endpointData = createEndpointData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/endpoint/' + encodeURIComponent(endpointData.name),
            data: JSON.stringify(endpointData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if (!isEndpointExistent(endpointData.name)) {
        			$endpointSelect = $('#sel-endpoint');
        			$endpointSelect.append($('<option>').attr('value', endpointData.name).text(endpointData.name));
        			sortSelectOptions($endpointSelect);
        		}
        		endpointMap[endpointData.name] = endpointData;
        		$('#modal-endpoint-overwrite').modal('hide');
        		$('#endpoints_infoBox').text('Endpoint \'' + getEndpointNameById(endpointData.name) + '\' saved.').show('fast').delay(5000).hide('fast');
            }
        });    		
	}
	
	function removeEndpoint(endpointName) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/endpoint/' + encodeURIComponent(getEndpointIdByName(endpointName)),
            success: function(data) {
                if (!data) {
                    return;
                }
        		delete endpointMap[getEndpointIdByName(endpointName)];
        		$("#sel-endpoint > option").filter(function(i){
     		       return $(this).attr("value") == getEndpointIdByName(endpointName);
        		}).remove();
        		$('#modal-endpoint-remove').modal('hide');
        		$('#endpoints_infoBox').text('Endpoint \'' + endpointName + '\' removed.').show('fast').delay(5000).hide('fast');
            }
        });    		
	}
	
	function createEndpointData() {
		var endpointData = {
			name: $('#input-endpoint-name').val() == '*' ? 'default_configuration' : $('#input-endpoint-name').val(),
		}
		return endpointData;
	}
	
	function getEndpointIdByName(endpointName) {
		return endpointName == '*' ? 'default_configuration' : endpointName;
	}
	
	function getEndpointNameById(endpointId) {
		return endpointId == 'default_configuration' ? '*' : endpointId;
	}

	function resetValues() {
		$('#input-endpoint-name').val('');
		enableOrDisableButtons();
	}
}