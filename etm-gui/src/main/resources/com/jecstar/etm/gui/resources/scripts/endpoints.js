function buildEndpointPage() {
	var parserMap = {};
	var endpointMap = {};
	$parserSelect = $('<select>').addClass('form-control custom-select etm-expression-parser');
	$parserFieldSelect = $('<select>').addClass('form-control custom-select etm-parser-field');
	
	$('#sel-endpoint').change(function(event) {
		event.preventDefault();
		var endpointData = endpointMap[$(this).val()];
		if ('undefined' == typeof endpointData) {
			resetValues();
			return;
		}
		setValuesFromData(endpointData);
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
		        	parserMap[parser.name] = parser;
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
		        	if ('DEFAULT' == endpoint.enhancer.type) {
		        		$endpointSelect.append($('<option>').attr('value', endpoint.name).text(getEndpointNameById(endpoint.name)));
		        		endpointMap[endpoint.name] = endpoint;
		        	}
		        });
		        sortSelectOptions($endpointSelect)
		        $endpointSelect.val('');
		    }
		});
		
	});
	
    function removeParserRow(anchor) {
    	anchor.parent().parent().parent().remove();
    }
    
    function addParserRow(anchor) {
    	anchor.siblings('ol').append(createParserRow);
    }
    
    function removeField(anchor)  {
    	anchor.parent().parent().remove();
    }
    
    function createParserRow(parser) {
    	var parserRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
					$('<div>').addClass('input-group').append(
							$parserSelect.clone(true), 
							$('<span>').addClass('input-group-addon').append($('<a href="#">').addClass('fa fa-times text-danger').click(function (event) {event.preventDefault(); removeParserRow($(this));}))
					)
			);
    	if (parser) {
    		$(parserRow).find('.etm-expression-parser').val(parser.name)
    	}
    	return parserRow;
    }
    
	function createFieldExtractionRow(fieldData) {
		var inputKey = $('<input>').attr('type', 'text').attr('required', 'required').addClass('form-control etm-collection-key');
		var localParserFieldSelect =  $parserFieldSelect.clone(true).change(function (event) {
			event.preventDefault();
			if (endsWith($(this).val(), '.')) {
				$(inputKey).removeAttr('disabled');
			} else {
				$(inputKey).attr('disabled', 'disabled');
			}
		});
		var parserRow = $('<ol>');
		if (fieldData) {
			var options = $parserFieldSelect.children("option").map(function(){return $(this).attr("value");}).get();
			$.each(options, function (index, option) {
				if (fieldData.field == option) {
					localParserFieldSelect.val(option);
					localParserFieldSelect.trigger('change');
					return false;
				} else if (startsWith(fieldData.field, option)) {
					localParserFieldSelect.val(option);
					inputKey.val(fieldData.field.substring(option.length));
					return false;
				}
			});
			$.each(fieldData.parsers, function(index, parser) {
				parserRow.append(createParserRow(parser))				
			})
		} else {
			parserRow.append(createParserRow())
		}
		var card = $('<div>').addClass('card card-block etm-field-card');
		card.append(
				$('<fieldset>').addClass('form-group').append(
						$('<a href="#">').addClass('pull-right').text('Remove this field').click(function (event) {event.preventDefault(); removeField($(this))}),
						$('<label>').text('Field'),
						localParserFieldSelect
				),
				$('<fieldset>').addClass('form-group').append(
						$('<label>').text('Collection key'),
						inputKey
				),
				$('<fieldset>').addClass('form-group').append(
						$('<a href="#">').addClass('pull-right').text('Add parser').click(function (event) {event.preventDefault(); addParserRow($(this))}),
						$('<label>').text('Parsers'),
						parserRow
				)
		)
		return card;
	}
	
	
	function endsWith(text, textToEndWith) {
		return text.lastIndexOf(textToEndWith) == text.length - textToEndWith.length;
	}
	
	function startsWith(text, textToStartWith) {
		return text.indexOf(textToStartWith) == 0;
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
        		if (!isEndpointExistent(getEndpointNameById(endpointData.name))) {
        			$endpointSelect = $('#sel-endpoint');
        			$endpointSelect.append($('<option>').attr('value', endpointData.name).text(endpointData.name));
        			sortSelectOptions($endpointSelect);
        		}
        		endpointMap[endpointData.name] = endpointData;
        		$('#endpoints_infoBox').text('Endpoint \'' + getEndpointNameById(endpointData.name) + '\' saved.').show('fast').delay(5000).hide('fast');
        		enableOrDisableButtons();
            }
        }).always(function () {
        	$('#modal-endpoint-overwrite').modal('hide');
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
        		$('#endpoints_infoBox').text('Endpoint \'' + endpointName + '\' removed.').show('fast').delay(5000).hide('fast');
        		enableOrDisableButtons();
            }
        }).always(function () {
        	$('#modal-endpoint-remove').modal('hide');
        });    		
	}
	
	function createEndpointData() {
		var endpointData = {
			name: getEndpointIdByName($('#input-endpoint-name').val()),
			enhancer: {
				type: 'DEFAULT',
				enhance_payload_format: $('#sel-detect-payload-format').val() == 'true' ? true : false,
				fields: []		
			}
		}
		$('.etm-field-card').each(function (index, block) {
			var field = {
			    field: endsWith($(block).find('.etm-parser-field').val(), '.') ? $(block).find('.etm-parser-field').val() + $(block).find('.etm-collection-key').val() : $(block).find('.etm-parser-field').val(),
			    parsers: []
			}
			$(block).find('.etm-expression-parser').each(function (index, parser) {
				field.parsers.push(parserMap[$(parser).val()])
			});
			endpointData.enhancer.fields.push(field);
		}); 
		return endpointData;
	}
	
	function setValuesFromData(endpointData) {
		$('#field-columns').empty();
		$('#input-endpoint-name').val(getEndpointNameById(endpointData.name));
		if (endpointData.enhancer.enhance_payload_format) {
			$('#sel-detect-payload-format').val('true');
		} else {
			$('#sel-detect-payload-format').val('false');
		}
		$.each(endpointData.enhancer.fields, function (index, field) {
			$('#field-columns').append(createFieldExtractionRow(field));
		});
	}

	function resetValues() {
		$('#input-endpoint-name').val('');
		$('#sel-detect-payload-format').val('false');
		$('#field-columns').empty();
		enableOrDisableButtons();
	}
	
	function getEndpointIdByName(endpointName) {
		return endpointName == '*' ? 'default_configuration' : endpointName;
	}
	
	function getEndpointNameById(endpointId) {
		return endpointId == 'default_configuration' ? '*' : endpointId;
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