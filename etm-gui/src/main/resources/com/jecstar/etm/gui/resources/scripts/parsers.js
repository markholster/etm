function buildParserPage() {
	var parserMap = {};
	$('#input-parser-type').change(function() {
		$('#grp-fixed_position, #grp-fixed_value, #grp-jsonpath, #grp-xpath,#grp-xslt').hide();
		// Disable all input fields otherwise form validation will fail.
		$('#grp-fixed_position input, #grp-fixed_value input, #grp-jsonpath input, #grp-xpath input ,#grp-xslt textarea').attr('disabled', 'disabled');
		$('#grp-' +$(this).val() + ' input').removeAttr('disabled'); 
		$('#grp-' +$(this).val() + ' textarea').removeAttr('disabled');
		$('#grp-' +$(this).val()).show(); 
	});

	$('#sel-parser').change(function(event) {
		event.preventDefault();
		var parserData = parserMap[$(this).val()];
		if ('undefined' == typeof parserData) {
			resetValues();
			return;
		}
		
		$('#input-parser-name').val(parserData.name);
		$('#input-parser-type').val(parserData.type).change();
		if ('fixed_position' == parserData.type) {
			$('#input-fixed_position-line').val(parserData.line + 1)
			$('#input-fixed_position-start-ix').val(parserData.start_ix + 1);
			$('#input-fixed_position-end-ix').val(parserData.end_ix + 1)
		} else if ('fixed_value' == parserData.type) {
			$('#input-fixed-value').val(parserData.value);
		} else if ('jsonpath' == parserData.type) {
			$('#input-jsonpath-expression').val(parserData.expression);
		} else if ('xpath' == parserData.type) {
			$('#input-xpath-expression').val(parserData.expression);
		} else if ('xslt' == parserData.type) {
			$('#input-xslt-template').val(parserData.template);
		}
		enableOrDisableButtons();
	});
	
	$('#btn-confirm-save-parser').click(function(event) {
		if (!document.getElementById('parser_form').checkValidity()) {
			return;
		}
		event.preventDefault();
		var parserName = $('#input-parser-name').val();
		if (isParserExistent(parserName)) {
			$('#overwrite-parser-name').text(parserName);
			$('#modal-parser-overwrite').modal();
		} else {
			saveParser();
		}
	});
	
	$('#btn-save-parser').click(function(event) {
		saveParser();
	});
	
	$('#btn-confirm-remove-parser').click(function(event) {
		event.preventDefault();
		$('#remove-parser-name').text($('#input-parser-name').val());
        $('#modal-parser-remove').modal();
	});	

	$('#btn-remove-parser').click(function(event) {
		removeParser($('#input-parser-name').val());
	});

	
	$('#input-parser-name').on('input', enableOrDisableButtons);
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/parsers',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        $parserSelect = $('#sel-parser');
	        $.each(data.parsers, function(index, parser) {
	        	$parserSelect.append($('<option>').attr('value', parser.name).text(parser.name));
	        	parserMap[parser.name] = parser;
	        });
	        sortSelectOptions($parserSelect)
	        $parserSelect.val('');
	    }
	});
	
	function sortSelectOptions($parserSelect) {
		var options = $parserSelect.children('option');
		options.detach().sort(function(a,b) {
		    var at = $(a).text();
		    var bt = $(b).text();         
		    return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
		});
		options.appendTo($parserSelect);
	}
	
	function enableOrDisableButtons() {
		var parserName = $('#input-parser-name').val();
		if (parserName) {
			$('#btn-confirm-save-parser').removeAttr('disabled');
			if (isParserExistent(parserName)) {
				$('#btn-confirm-remove-parser').removeAttr('disabled');
			} else {
				$('#btn-confirm-remove-parser').attr('disabled', 'disabled');
			}
		} else {
			$('#btn-confirm-save-parser, #btn-confirm-remove-parser').attr('disabled', 'disabled');
		}
	}
	
	function isParserExistent(name) {
		return "undefined" != typeof parserMap[name];
	}
	
	function saveParser() {
		var parserData = createParserData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/parser/' + encodeURIComponent(parserData.name),
            data: JSON.stringify(parserData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if (!isParserExistent(parserData.name)) {
        			$parserSelect = $('#sel-parser');
        			$parserSelect.append($('<option>').attr('value', parserData.name).text(parserData.name));
        			sortSelectOptions($parserSelect);
        		}
        		parserMap[parserData.name] = parserData;
        		$('#parsers_infoBox').text('Parser \'' + parserData.name + '\' saved.').show('fast').delay(5000).hide('fast');
            }
        }).always(function () {
        	if ($('#modal-parser-overwrite').is(':visible')) {
        		$('#modal-parser-overwrite').modal('hide');
        	}
        });    		
	}
	
	function removeParser(parserName) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/parser/' + encodeURIComponent(parserName),
            success: function(data) {
                if (!data) {
                    return;
                }
        		delete parserMap[parserName];
        		$("#sel-parser > option").filter(function(i){
        		       return $(this).attr("value") == parserName;
        		}).remove();
        		$('#parsers_infoBox').text('Parser \'' + parserName + '\' removed.').show('fast').delay(5000).hide('fast');
            }
        }).always(function () {
        	if ($('#modal-parser-remove').is(':visible')) {
        		$('#modal-parser-remove').modal('hide');
        	}
        });    		
	}
	
	function createParserData() {
		var parserData = {
			name: $('#input-parser-name').val(),
			type: $('#input-parser-type').val()
		}
		if ('fixed_position' == parserData.type) {
			parserData['line'] = Number($('#input-fixed_position-line').val()) - 1;
			parserData['start_ix'] = Number($('#input-fixed_position-start-ix').val()) - 1;
			parserData['end_ix'] = Number($('#input-fixed_position-end-ix').val()) - 1;
		} else if ('fixed_value' == parserData.type) {
			parserData['value'] = $('#input-fixed-value').val();
		} else if ('jsonpath' == parserData.type) {
			parserData['expression'] = $('#input-jsonpath-expression').val();
		} else if ('xpath' == parserData.type) {
			parserData['expression'] = $('#input-xpath-expression').val();
		} else if ('xslt' == parserData.type) {
			parserData['template'] = $('#input-xslt-template').val();
		}
		return parserData;
	}

	function resetValues() {
		$('#input-parser-name').val('');
		$('#input-parser-type').val('fixed_position').change();
		$('#input-fixed_position-line').val(1)
		$('#input-fixed_position-start-ix').val(1);
		$('#input-fixed_position-end-ix').val(2)
		enableOrDisableButtons();
	}
}