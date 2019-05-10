function buildImportProfilesPage() {
    const parserMap = {};
    const importProfilesMap = {};
    let keywords;
    const $parserExtractionSelect = $('<select>').addClass('form-control custom-select etm-expression-parser');
    const $parserTransformationSelect = $('<select>').addClass('form-control custom-select etm-expression-parser');
    const $parserFieldSelect = $('<select>').addClass('form-control custom-select etm-parser-field');

    $('#sel-import-profile').change(function (event) {
		event.preventDefault();
        const importProfileData = importProfilesMap[$(this).val()];
        if ('undefined' == typeof importProfileData) {
			resetValues();
			return;
		}
        setValuesFromData(importProfileData);
		enableOrDisableButtons();
	});

    $('#import_profile_form').on('change', '.etm-transformation-card > div > div > div .etm-expression-parser', function (event) {
	    event.preventDefault();
        const parser = parserMap[$(this).val()];
        const cardBody = $(this).parent().parent().parent();
        if ('xslt' === parser.type) {
            $(cardBody).children().slice(2).hide();
        } else if ('regex' === parser.type) {
            $(cardBody).children().slice(2).show();
        }
	});

    $('#btn-confirm-save-import-profile').on('click', function (event) {
        if (!document.getElementById('import_profile_form').checkValidity()) {
			return;
		}
		event.preventDefault();
        const importProfileName = $('#input-import-profile-name').val();
        if (isImportProfileExistent(importProfileName)) {
            $('#overwrite-import-profile-name').text(importProfileName);
            $('#modal-import-profile-overwrite').modal();
		} else {
            saveImportProfile();
		}
	});

    $('#btn-save-import-profile').on('click', function (event) {
        event.preventDefault();
        saveImportProfile();
	});

    $('#btn-confirm-remove-import-profile').on('click', function (event) {
		event.preventDefault();
        $('#remove-import-profile-name').text($('#input-import-profile-name').val());
        $('#modal-import-profile-remove').modal();
    });

    $('#btn-remove-import-profile').on('click', function (event) {
        event.preventDefault();
        removeImportProfile($('#input-import-profile-name').val());
	});


    $('#input-import-profile-name').on('input', enableOrDisableButtons);
	
	$.when(
		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
		    url: '../rest/settings/parsers',
		    cache: false,
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
                commons.sortSelectOptions($parserExtractionSelect);
		    }
		}),
		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
		    url: '../rest/settings/parserfields',
		    cache: false,
		    success: function(data) {
		        if (!data) {
		            return;
		        }
		        $.each(data.parserfields, function(index, parserField) {
		        	$parserFieldSelect.append($('<option>').attr('value', parserField.name).text(parserField.name));
		        });
                commons.sortSelectOptions($parserFieldSelect);
		    }
		}),
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: '../rest/audit/keywords/etm_event_all',
            cache: false,
            success: function(data) {
                if (!data || !data.keywords) {
                    return;
                }
                keywords = data.keywords;
            }
        })
	).done(function () {
        $('#link-add-extraction-field').on('click', function (event) {
			event.preventDefault();
			$('#field-extraction-columns').append(createFieldExtractionRow());
		});
        $('#link-add-transformation-field').on('click', function (event) {
            event.preventDefault();
            const row = createTransformationRow();
            $('#field-transformation-columns').append(row);
            $(row).find('.etm-expression-parser').trigger('change');
        });

		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
            url: '../rest/settings/import_profiles',
		    cache: false,
		    success: function(data) {
		        if (!data) {
		            return;
		        }
                const $importProfileSelect = $('#sel-import-profile');
                $.each(data.import_profiles, function (index, import_profile) {
                    if ('DEFAULT' === import_profile.enhancer.type) {
                        $importProfileSelect.append($('<option>').attr('value', import_profile.name).text(getImportProfileNameById(import_profile.name)));
                        importProfilesMap[import_profile.name] = import_profile;
		        	}
		        });
                commons.sortSelectOptions($importProfileSelect);
                $importProfileSelect.val('');
		    }
		});
	});

    // Add the autocomplete handling.
    $('#import_profile_form').on('keydown', "input[data-element-type='autocomplete-input']", function (event) {
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
        const parserRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
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
        const card = $('<div>').addClass('card card-block etm-transformation-card form-group');
        const localExtractionSelect = $parserTransformationSelect.clone(true);
        const replacementInput = $('<input>').addClass("form-control etm-replacement").attr('type', 'text');
        const replaceAllSelect = $('<select>').addClass('form-control custom-select etm-replace-all').append(
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
                        $('<a href="#">').addClass('float-right').text('Remove this transformation').click(function (event) {
                            event.preventDefault();
                            removeField($(this))
                        })
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
        );
		return card;
    }

	function createFieldExtractionRow(fieldData) {
        const inputKey = $('<input>').attr('type', 'text').attr('required', 'required').addClass('form-control etm-collection-key');
        const localParserFieldSelect = $parserFieldSelect.clone(true).change(function (event) {
			event.preventDefault();
			if (endsWith($(this).val(), '.')) {
				$(inputKey).removeAttr('disabled');
			} else {
				$(inputKey).attr('disabled', 'disabled');
			}
		});
        const writePolicySelect = $('<select>').addClass('form-control custom-select etm-writy-policy').append(
            $('<option>').attr('value', 'ALWAYS_OVERWRITE').text('Always overwrite'),
            $('<option>').attr('value', 'OVERWRITE_WHEN_FOUND').text('Overwrite when found'),
            $('<option>').attr('value', 'WHEN_EMPTY').text('When empty').attr('selected', 'selected')
		);
        const parsersSource = $('<input>')
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
            );
        const parserRow = $('<ol>');
		if (fieldData) {
            const options = $parserFieldSelect.children("option").map(function () {
                return $(this).attr("value");
            }).get();
			$.each(options, function (index, option) {
                if (fieldData.field === option) {
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
        const card = $('<div>').addClass('card card-block etm-extraction-card form-group');
		card.append(
            $('<div>').addClass('card-body').append(
				$('<div>').addClass('form-group row').append(
					$('<div>').addClass('col-sm-12').append(
                        $('<a href="#">').addClass('float-right').text('Remove this field').click(function (event) {
                            event.preventDefault();
                            removeField($(this))
                        })
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
                    $('<a href="#">').addClass('float-right').text('Add parser').click(function (event) {
                        event.preventDefault();
                        addParserRow($(this))
                    }),
					$('<label>').text('Parsers'),
					parserRow
				)
			)
        );
		return card;
	}

	function endsWith(text, textToEndWith) {
        return text.lastIndexOf(textToEndWith) === text.length - textToEndWith.length;
	}
	
	function startsWith(text, textToStartWith) {
        return text.indexOf(textToStartWith) === 0;
	}

	function enableOrDisableButtons() {
        const importProfileName = $('#input-import-profile-name').val();
        if (importProfileName) {
            $('#btn-confirm-save-import-profile').removeAttr('disabled');
            if (isImportProfileExistent(importProfileName) && '*' !== importProfileName) {
                $('#btn-confirm-remove-import-profile').removeAttr('disabled');
			} else {
                $('#btn-confirm-remove-import-profile').attr('disabled', 'disabled');
			}
		} else {
            $('#btn-confirm-save-import-profile, #btn-confirm-remove-import-profile').attr('disabled', 'disabled');
		}
	}

    function isImportProfileExistent(name) {
        return "undefined" != typeof importProfilesMap[getImportProfileIdByName(name)];
    }

    function saveImportProfile() {
        const importProfileData = createImportProfileData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/import_profile/' + encodeURIComponent(importProfileData.name),
            cache: false,
            data: JSON.stringify(importProfileData),
            success: function(data) {
                if (!data) {
                    return;
                }
                if (!isImportProfileExistent(getImportProfileNameById(importProfileData.name))) {
                    const $importProfileSelect = $('#sel-import-profile');
                    $importProfileSelect.append($('<option>').attr('value', importProfileData.name).text(importProfileData.name));
                    commons.sortSelectOptions($importProfileSelect);
        		}
                importProfilesMap[importProfileData.name] = importProfileData;
                $('#import-profiles_infoBox').text('Import profile \'' + getImportProfileNameById(importProfileData.name) + '\' saved.').show('fast').delay(5000).hide('fast');
        		enableOrDisableButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-import-profile-overwrite'));
        });
	}

    function removeImportProfile(importProfileName) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/import_profile/' + encodeURIComponent(getImportProfileIdByName(importProfileName)),
            cache: false,
            success: function(data) {
                if (!data) {
                    return;
                }
                delete importProfilesMap[getImportProfileIdByName(importProfileName)];
                $("#sel-import-profile > option").filter(function () {
                    return $(this).attr("value") === getImportProfileIdByName(importProfileName);
        		}).remove();
                $('#import-profiles_infoBox').text('Import profile \'' + importProfileName + '\' removed.').show('fast').delay(5000).hide('fast');
        		enableOrDisableButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-import-profile-remove'));
        });
	}

    function createImportProfileData() {
        const importProfileData = {
            name: getImportProfileIdByName($('#input-import-profile-name').val()),
			enhancer: {
				type: 'DEFAULT',
                enhance_payload_format: $('#sel-detect-payload-format').val() === 'true',
				fields: [],
				transformations: []
			}
        };
		$('.etm-extraction-card').each(function (index, block) {
            const field = {
                field: endsWith($(block).find('.etm-parser-field').val(), '.') ? $(block).find('.etm-parser-field').val() + $(block).find('.etm-collection-key').val() : $(block).find('.etm-parser-field').val(),
                write_policy: $(block).find('.etm-writy-policy').val(),
                parsers_source: $(block).find('.etm-parsers-source').val(),
                parsers: []
            };
			$(block).find('.etm-expression-parser').each(function (index, parser) {
				field.parsers.push(parserMap[$(parser).val()])
			});
            importProfileData.enhancer.fields.push(field);
		});
		$('.etm-transformation-card').each(function (index, block) {
            const transformation = {
                parser: parserMap[$(block).find('.etm-expression-parser').val()],
            };
            if ('regex' === transformation.parser.type) {
                transformation.replacement = $(block).find('.etm-replacement').val() ? $(block).find('.etm-replacement').val() : null;
                transformation.replace_all = $(block).find('.etm-replace-all').val() === 'true';
            } else if ('xslt' === transformation.parser.type) {
                transformation.replacement = null;
                transformation.replace_all = false;
            }
            importProfileData.enhancer.transformations.push(transformation);
        });
        return importProfileData;
    }

    function setValuesFromData(importProfileData) {
	    $('#field-transformation-columns').empty();
		$('#field-extraction-columns').empty();
        $('#input-import-profile-name').val(getImportProfileNameById(importProfileData.name));
        if (importProfileData.enhancer.enhance_payload_format) {
			$('#sel-detect-payload-format').val('true');
		} else {
			$('#sel-detect-payload-format').val('false');
		}
        $.each(importProfileData.enhancer.fields, function (index, field) {
			$('#field-extraction-columns').append(createFieldExtractionRow(field));
		});
        $.each(importProfileData.enhancer.transformations, function (index, transformation) {
            const row = createTransformationRow(transformation);
            $('#field-transformation-columns').append(row);
            $(row).find('.etm-expression-parser').trigger('change');
        });
	}

	function resetValues() {
        $('#input-import-profile-name').val('');
		$('#sel-detect-payload-format').val('false');
		$('#field-transformation-columns').empty();
		$('#field-extraction-columns').empty();
		enableOrDisableButtons();
	}

    function getImportProfileIdByName(importProfileName) {
        return importProfileName === '*' ? 'default_configuration' : importProfileName;
    }

    function getImportProfileNameById(importProfileId) {
        return importProfileId === 'default_configuration' ? '*' : importProfileId;
	}
}