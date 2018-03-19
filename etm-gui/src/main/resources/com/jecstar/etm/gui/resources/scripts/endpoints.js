function buildEndpointPage() {
	var parserMap = {};
	var endpointMap = {};
	var keywords;
	$parserExtractionSelect = $('<select>').addClass('form-control custom-select etm-expression-parser');
	$parserTransformationSelect = $('<select>').addClass('form-control custom-select etm-expression-parser');
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

	$('#endpoint_form').on('change', '.etm-transformation-card > div > div > div .etm-expression-parser',  function (event) {
	    event.preventDefault();
	    var parser = parserMap[$(this).val()];
	    var cardBody = $(this).parent().parent().parent();
        if ('xslt' == parser.type) {
            $(cardBody).children().slice(2).hide();
        } else if ('regex' == parser.type) {
            $(cardBody).children().slice(2).show();
        }
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
		        	$parserExtractionSelect.append($('<option>').attr('value', parser.name).text(parser.name));
		        	if (parser.capable_of_replacing) {
		        	    $parserTransformationSelect.append($('<option>').attr('value', parser.name).text(parser.name));
		        	}
		        	parserMap[parser.name] = parser;
		        });
		        sortSelectOptions($parserExtractionSelect);
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
		}),
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: '../rest/audit/keywords/etm_event_all',
            success: function(data) {
                if (!data || !data.keywords) {
                    return;
                }
                keywords = data.keywords;
            }
        })
	).done(function () {
		$('#link-add-extraction-field').click(function(event) {
			event.preventDefault();
			$('#field-extraction-columns').append(createFieldExtractionRow());
		});
        $('#link-add-transformation-field').click(function(event) {
            event.preventDefault();
            var row = createTransformationRow();
            $('#field-transformation-columns').append(row);
            $(row).find('.etm-expression-parser').trigger('change');
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

    // Add the autocomplete handling.
    $('#endpoint_form').on('keydown', "input[data-element-type='autocomplete-input']", function(event) {
        if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
            event.stopPropagation();
        }
    });
	
    function removeParserRow(anchor) {
    	anchor.parent().parent().parent().remove();
    }
    
    function addParserRow(anchor) {
    	anchor.siblings('ol').append(createParserRow);
    }
    
    function removeField(anchor)  {
    	anchor.parent().parent().parent().parent().remove();
    }
    
    function createParserRow(parser) {
    	var parserRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
            $('<div>').addClass('input-group mb-3').append(
                $parserExtractionSelect.clone(true),
                $('<div>').addClass('input-group-append').append(
                    $('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').click(function (event) {event.preventDefault(); removeParserRow($(this));})
                )
            )
		);
    	if (parser) {
    		$(parserRow).find('.etm-expression-parser').val(parser.name)
    	}
    	return parserRow;
    }

    function createTransformationRow(rowData) {
        var card = $('<div>').addClass('card card-block etm-transformation-card form-group');
        var localExtractionSelect = $parserTransformationSelect.clone(true);
        var replacementInput = $('<input>').addClass("form-control etm-replacement").attr('type', 'text');
        var replaceAllSelect = $('<select>').addClass('form-control custom-select etm-replace-all').append(
            $('<option>').attr('value', 'true').text('Yes'),
            $('<option>').attr('value', 'false').attr('selected', 'selected').text('No')
        );
        if (rowData) {
            localExtractionSelect.val(rowData.parser.name);
            replacementInput.val(rowData.replacement);
            replaceAllSelect.val(rowData.replace_all ? 'true' : 'false');
        }

		card.append(
		    $('<div>').addClass('card-body').append(
				$('<div>').addClass('form-group row').append(
					$('<div>').addClass('col-sm-12').append(
						$('<a href="#">').addClass('pull-right').text('Remove this transformation').click(function (event) {event.preventDefault(); removeField($(this))})
					)
				),
				$('<div>').addClass('form-group row').append(
					$('<label>').addClass('col-sm-3 col-form-label').text('Parser'),
					$('<div>').addClass('col-sm-9').append(localExtractionSelect)
				),
				$('<div>').addClass('form-group row').append(
					$('<label>').addClass('col-sm-3 col-form-label').text('Replacement'),
					$('<div>').addClass('col-sm-9').append(replacementInput)
				),
				$('<div>').addClass('form-group row').append(
				    $('<label>').addClass('col-sm-3 col-form-label').text('Replace all occurrences'),
				    $('<div>').addClass('col-sm-9').append(replaceAllSelect)
    			)
			)
		)
		return card;
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
		var writePolicySelect = $('<select>').addClass('form-control custom-select etm-writy-policy').append(
            $('<option>').attr('value', 'ALWAYS_OVERWRITE').text('Always overwrite'),
            $('<option>').attr('value', 'OVERWRITE_WHEN_FOUND').text('Overwrite when found'),
            $('<option>').attr('value', 'WHEN_EMPTY').text('When empty').attr('selected', 'selected')
		);
		var parsersSource = $('<input>')
		    .attr('type', 'text')
		    .attr('required', 'required')
		    .attr('data-element-type', 'autocomplete-input')
		    .addClass('form-control etm-parsers-source')
		    .val('payload')
		    .autocompleteFieldQuery(
                {
                    queryKeywords: keywords,
                    mode: 'field',
                    keywordFilter: function(index, group, keyword) {
                        return !('payload' === keyword.name || keyword.name.indexOf('metadata.') === 0);
                    }
                }
            )
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
			writePolicySelect.val(fieldData.write_policy ? fieldData.write_policy : 'WHEN_EMPTY');
			parsersSource.val(fieldData.parsers_source ? fieldData.parsers_source : 'payload');
			$.each(fieldData.parsers, function(index, parser) {
				parserRow.append(createParserRow(parser))				
			})
		} else {
			parserRow.append(createParserRow())
		}
		var card = $('<div>').addClass('card card-block etm-extraction-card form-group');
		card.append(
		    $('<div>').addClass('card-body').append(
				$('<div>').addClass('form-group row').append(
					$('<div>').addClass('col-sm-12').append(
						$('<a href="#">').addClass('pull-right').text('Remove this field').click(function (event) {event.preventDefault(); removeField($(this))})
					)
				),
				$('<div>').addClass('form-group row').append(
					$('<label>').addClass('col-sm-3 col-form-label').text('Field'),
					$('<div>').addClass('col-sm-9').append(localParserFieldSelect)
				),
				$('<div>').addClass('form-group row').append(
					$('<label>').addClass('col-sm-3 col-form-label').text('Collection key'),
					$('<div>').addClass('col-sm-9').append(inputKey)
				),
				$('<div>').addClass('form-group row').append(
					$('<label>').addClass('col-sm-3 col-form-label').text('Write policy'),
					$('<div>').addClass('col-sm-9').append(writePolicySelect)
				),
				$('<div>').addClass('form-group row').append(
                	$('<label>').addClass('col-sm-3 col-form-label').text('Parsers source'),
                	$('<div>').addClass('col-sm-9').append(parsersSource)
                ),
				$('<fieldset>').addClass('form-group').append(
					$('<a href="#">').addClass('pull-right').text('Add parser').click(function (event) {event.preventDefault(); addParserRow($(this))}),
					$('<label>').text('Parsers'),
					parserRow
				)
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
            hideModals($('#modal-endpoint-overwrite'));
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
            hideModals($('#modal-endpoint-remove'));
        });
	}
	
	function createEndpointData() {
		var endpointData = {
			name: getEndpointIdByName($('#input-endpoint-name').val()),
			enhancer: {
				type: 'DEFAULT',
				enhance_payload_format: $('#sel-detect-payload-format').val() == 'true' ? true : false,
				fields: [],
				transformations: []
			}
		}
		$('.etm-extraction-card').each(function (index, block) {
			var field = {
			    field: endsWith($(block).find('.etm-parser-field').val(), '.') ? $(block).find('.etm-parser-field').val() + $(block).find('.etm-collection-key').val() : $(block).find('.etm-parser-field').val(),
			    write_policy: $(block).find('.etm-writy-policy').val(),
			    parsers_source:	$(block).find('.etm-parsers-source').val(),
			    parsers: []
			}
			$(block).find('.etm-expression-parser').each(function (index, parser) {
				field.parsers.push(parserMap[$(parser).val()])
			});
			endpointData.enhancer.fields.push(field);
		});
		$('.etm-transformation-card').each(function (index, block) {
            var transformation = {
                parser: parserMap[$(block).find('.etm-expression-parser').val()],
            }
            if ('regex' == transformation.parser.type) {
                transformation.replacement = $(block).find('.etm-replacement').val() ? $(block).find('.etm-replacement').val() : null
                transformation.replace_all = $(block).find('.etm-replace-all').val() == 'true' ? true : false
            } else if ('xslt' == transformation.parser.type) {
                transformation.replacement = null
                transformation.replace_all = false
            }
            endpointData.enhancer.transformations.push(transformation);
        });
		return endpointData;
	}
	
	function setValuesFromData(endpointData) {
	    $('#field-transformation-columns').empty();
		$('#field-extraction-columns').empty();
		$('#input-endpoint-name').val(getEndpointNameById(endpointData.name));
		if (endpointData.enhancer.enhance_payload_format) {
			$('#sel-detect-payload-format').val('true');
		} else {
			$('#sel-detect-payload-format').val('false');
		}
		$.each(endpointData.enhancer.fields, function (index, field) {
			$('#field-extraction-columns').append(createFieldExtractionRow(field));
		});
        $.each(endpointData.enhancer.transformations, function (index, transformation) {
            var row = createTransformationRow(transformation);
            $('#field-transformation-columns').append(row);
            $(row).find('.etm-expression-parser').trigger('change');
        });
	}

	function resetValues() {
		$('#input-endpoint-name').val('');
		$('#sel-detect-payload-format').val('false');
		$('#field-transformation-columns').empty();
		$('#field-extraction-columns').empty();
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