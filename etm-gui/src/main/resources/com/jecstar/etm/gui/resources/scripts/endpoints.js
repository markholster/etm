function buildEndpointPage() {
	var endpointMap = {};
	$parserSelect = $('<select>').addClass('form-control c-select');
	$parserFieldSelect = $('<select>').addClass('form-control c-select');
	
	$('#sel-endpoint').change(function(event) {
		event.preventDefault();
		var endpointData = endpointMap[$(this).val()];
		if ('undefined' == typeof endpointData) {
			resetValues();
			return;
		}
		$('#input-endpoint-name').val(getEndpointNameById(endpointData.name));
		if (endpointData.detect_payload_format) {
			$('#sel-detect-payload-format').val('true');
		} else {
			$('#sel-detect-payload-format').val('false');
		}
		
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
	
	$.when(
		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
		    url: '../rest/settings/parsers',
		    success: function(data) {
		        if (!data) {
		            return;
		        }
		        $.each(data.parsers, function(index, parser) {
		        	$parserSelect.append($('<option>').attr('value', parser.name).text(parser.name));
		        });
		        sortSelectOptions($parserSelect);
		    }
		}),
		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
		    url: '../rest/settings/parserfields',
		    success: function(data) {
		        if (!data) {
		            return;
		        }
		        $.each(data.parserfields, function(index, parserField) {
		        	$parserFieldSelect.append($('<option>').attr('value', parserField.name).text(parserField.name));
		        });
		        sortSelectOptions($parserFieldSelect);
		    }
		})		
	).done(function () {
		$('#link-add-field').click(function(event) {
			event.preventDefault();
			$('#field-columns').append(createFieldExtractionRow());
			updateRowActions();
		});
		
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
		
	});
	
    function removeRow(row) {
        $(row).remove();
        updateRowActions();
    }
    
    function moveRowUp(row) {
        $(row).after($(row).prev());
        updateRowActions();
    }

    function moveRowDown(row) {
        $(row).before($(row).next());
        updateRowActions();
    }
	
	function createFieldExtractionRow() {
		var row = $('<div>').addClass('row fieldConfigurationRow').attr('style', 'margin-top: 5px;');
		var inputKey = $('<input>').attr('type', 'text').addClass('form-control');
		var localParserFieldSelect =  $parserFieldSelect.clone(true).change(function (event) {
			event.preventDefault();
			if (endsWith($(this).val(), '.')) {
				$(inputKey).removeAttr('disabled');
			} else {
				$(inputKey).attr('disabled', 'disabled');
			}
		});
		row.append(
	        $('<div>').addClass('col-sm-2').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append(localParserFieldSelect),
	        $('<div>').addClass('col-sm-2').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append(inputKey),
	        $('<div>').addClass('col-sm-4').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($parserSelect.clone(true))
		);
		row.append($('<div>').addClass('col-sm-2'));
        var fieldActionDiv = $('<div>').addClass('col-sm-2').append(
                $('<div>').addClass('row actionRow').append(
                    $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-up').click(function (event) {event.preventDefault(); moveRowUp(row)})),
                    $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-down').click(function (event) {event.preventDefault(); moveRowDown(row)})),
                    $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-times text-danger').click(function (event) {event.preventDefault(); removeRow(row)}))
                )
            );
        row.append(fieldActionDiv);
		return row;
	}
	
    function updateRowActions() {
        $('#field-columns .actionRow').each(function (index, row) {
            if ($('#field-columns').children().size() > 2) {
                if (index == 0) {
                    $(row).find('.fa-arrow-up').hide();
                } else {
                    $(row).find('.fa-arrow-up').show();
                }
                if (index >= $('#field-columns').children().size() -2) {
                    $(row).find('.fa-arrow-down').hide();
                } else {
                    $(row).find('.fa-arrow-down').show();
                }
            } else {
                $(row).find('.fa-arrow-up').hide();
                $(row).find('.fa-arrow-down').hide();
            }
        });
    }
	
	function endsWith(text, textToEndWith) {
		return text.lastIndexOf(textToEndWith) == text.length - textToEndWith.length;
	}
	
	
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
			if (isEndpointExistent(endpointName) && '*' != endpointName) {
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
        		enableOrDisableButtons();
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
        		enableOrDisableButtons();
            }
        });    		
	}
	
	function createEndpointData() {
		var endpointData = {
			name: getEndpointIdByName($('#input-endpoint-name').val()),
			detect_payload_format: $('#sel-detect-payload-format').val() == 'true' ? true : false
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
		$('#sel-detect-payload-format').val('false');
		enableOrDisableButtons();
	}
	
	function sortSelectOptions($select) {
		var options = $select.children('option');
		options.detach().sort(function(a,b) {
		    var at = $(a).text();
		    var bt = $(b).text();         
		    return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
		});
		options.appendTo($select);
	}
	
}