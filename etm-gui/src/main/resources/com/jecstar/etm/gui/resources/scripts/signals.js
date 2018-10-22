function buildSignalsPage(groupName) {
    $notifierSelect = $('<select>').addClass('form-control form-control-sm custom-select custom-select-sm etm-notifier').attr('required', 'required');
    var contextRoot = '../rest/signal/';
    if (groupName) {
        contextRoot += encodeURIComponent(groupName) + '/';
        $('#block-email-all-group-members').show();
    }
	var signalMap = {};
	var keywords = [];
    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: contextRoot + 'contextdata',
        cache: false,
        success: function (data) {
            if (!data) {
                return;
            }
            if (data.signal_datasources) {
                $dsSelect = $('#sel-data-source');
                $.each(data.signal_datasources, function (index, datasource) {
                    $dsSelect.append($('<option>').attr('value', datasource).text(datasourceToText(datasource)));
                });
                sortSelectOptions($dsSelect);

                function datasourceToText(datasource) {
                    if ('etm_audit_all' === datasource) {
                        return 'Audits';
                    } else if ('etm_event_all' === datasource) {
                        return 'Events';
                    } else if ('etm_metrics_all' === datasource) {
                        return 'Metrics';
                    } else {
                        return datasource;
                    }
                }
            }
            if (data.notifiers) {
                $.each(data.notifiers, function (index, notifier) {
                    $notifierSelect.append(
                        $('<option>')
                            .attr('value', notifier.name)
                            .text(notifier.name)
                            .attr('data-type', notifier.type)
                    );
                });
                sortSelectOptions($notifierSelect)
                $('#lnk-add-notifier').trigger('click');
            }
        }
    });
    $.when(
        $.ajax({
	        type: 'GET',
	        contentType: 'application/json',
            url: contextRoot + 'keywords',
	        cache: false,
	        success: function(data) {
	            if (!data || !data.keywords) {
	                return;
	            }
	            keywords = $.merge(keywords, data.keywords);
	        }
        })
	).done(function () {
        $('#input-signal-query').bind('keydown', function( event ) {
            if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                event.stopPropagation();
            }
        }).autocompleteFieldQuery(
            {
                queryKeywords: keywords,
                keywordIndexFilter: function(index) {
                    return index != $('#sel-data-source').val();
                }
            }
        );

	    $('#input-signal-field').on('keydown', function(event) {
            if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                event.stopPropagation();
            }
        }).autocompleteFieldQuery(
            {
                queryKeywords: keywords,
                mode: 'field',
                keywordIndexFilter: function(index) {
                    return index != $('#sel-data-source').val();
                },
                keywordFilter: function(index, group, keyword) {
                    var agg = $('#sel-signal-when').val();
                    if ('AVERAGE' == agg || 'SUM' == agg || 'MEDIAN' == agg || 'PERCENTILE' == agg || 'PERCENTILE_RANK' == agg) {
                        return !keyword.number;
                    } else if ('MIN' == agg || 'MAX' == agg) {
                        return !keyword.number && !keyword.date;
                    } else if ('CARDINALITY' == agg) {
                        return !keyword.number && !keyword.date && 'text' != keyword.type;
                    } else {
                        return true;
                    }
                }
            }
        );
    });

    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: contextRoot + 'signals',
        cache: false,
        success: function(data) {
            if (!data) {
                return;
            }
            $signalSelect = $('#sel-signal');
            $.each(data.signals, function(index, signal) {
                $signalSelect.append($('<option>').attr('value', signal.name).text(signal.name));
                signalMap[signal.name] = signal;
            });
            sortSelectOptions($signalSelect)
            $signalSelect.val('');
        }
    });

    $('#signal_form').on('change', '.etm-notifier', showOrHideRecipients);
    $('#signal_form').on('input', 'input[data-required]', enableOrDisableButtons);
    $('#input-signal-field').on('autocomplete:selected', enableOrDisableButtons);

	$('#sel-signal-when').on('change', function(event) {
	    if ('COUNT' == $(this).val()) {
	        $('#input-signal-field').parent().parent().hide();
	    } else {
	        $('#input-signal-field').parent().parent().show();
	    }
	    enableOrDisableButtons();
	});

    $('#lnk-add-notifier').click(function(event) {
        event.preventDefault();
        $('#list-notifiers').append(createNotifierRow());
        showOrHideRecipients();
        enableOrDisableButtons();
    });

    $('#lnk-add-recipient').click(function(event) {
        event.preventDefault();
        $('#list-recipients').append(createRecipientRow());
    });

    $('#sel-signal').change(function(event) {
        event.preventDefault();
        var signalData = signalMap[$(this).val()];
        if ('undefined' == typeof signalData) {
            resetValues();
            return;
        }
        setValuesFromData(signalData);
        enableOrDisableButtons();
    });

    $('#input-signal-per-timespan, #input-signal-last-timespan, #input-signal-check-timespan').on('change', function(event) {
        var subtype = $(this).attr('id').substring(13, $(this).attr('id').indexOf('-timespan'));
        if ($(this).val() != 1) {
            $('#sel-signal-' + subtype + '-timeunit').children('#' + subtype + '-minute').text('Minutes')
            $('#sel-signal-' + subtype + '-timeunit').children('#' + subtype + '-hour').text('Hours')
            $('#sel-signal-' + subtype + '-timeunit').children('#' + subtype + '-day').text('Days')
        } else {
            $('#sel-signal-' + subtype + '-timeunit').children('#' + subtype + '-minute').text('Minute')
            $('#sel-signal-' + subtype + '-timeunit').children('#' + subtype + '-hour').text('Hour')
            $('#sel-signal-' + subtype + '-timeunit').children('#' + subtype + '-day').text('Day')
        }
    });

	$('#input-signal-exceeded-count').on('change', function(event) {
	    if ($(this).val() != 1) {
	        $('#label-exceeded-count-time').text('times');
	    } else {
	        $('#label-exceeded-count-time').text('time');
	    }
	});

    $('#btn-confirm-save-signal').click(function(event) {
        if (!document.getElementById('signal_form').checkValidity()) {
            return;
        }
        event.preventDefault();
        var signalName = $('#input-signal-name').val();
        if (isSignalExistent(signalName)) {
            $('#overwrite-signal-name').text(signalName);
            $('#modal-signal-overwrite').modal();
        } else {
            saveSignal();
        }
    });

    $('#btn-save-signal').click(function(event) {
        saveSignal();
    });

    $('#btn-confirm-remove-signal').click(function(event) {
        event.preventDefault();
        $('#remove-signal-name').text($('#input-signal-name').val());
        $('#modal-signal-remove').modal();
    });

    $('#btn-remove-signal').click(function(event) {
        removeSignal($('#input-signal-name').val());
    });

    $('#btn-visualize-signal').click(function(event) {
        event.preventDefault();
        visualize();
    });

    function resetValues() {
        document.getElementById('signal_form').reset();
        $('#list-notifiers').empty();
        $('#list-recipients').empty();
        $('#lnk-add-notifier').trigger('click');
        enableOrDisableButtons();
        showOrHideRecipients();
        $('#sel-signal-when, #input-signal-per-timespan, #input-signal-last-timespan, #input-signal-check-timespan').trigger('change');
    }

    function setValuesFromData(signalData) {
        $('#input-signal-name').val(signalData.name);
        $('#sel-data-source').val(signalData.data_source);
        $('#input-signal-query').val(signalData.query);
        $('#input-signal-check-timespan').val(signalData.interval)
        $('#sel-signal-check-timeunit').val(signalData.interval_timeunit)
        $('#list-notifiers').empty();
        if (signalData.notifiers) {
            $.each(signalData.notifiers, function(index, notifierName) {
                $('#list-notifiers').append(createNotifierRow(notifierName));
            });
            showOrHideRecipients();
        }
        $('#list-recipients').empty();
        if (signalData.email_recipients) {
            $('#sel-email-all-group-members').val(signalData.email_all_etm_group_members ? 'true' : 'false');
            $.each(signalData.email_recipients, function(index, email) {
                $('#list-recipients').append(createRecipientRow(email));
            });
        }
        $('#sel-signal-when').val(signalData.operation);
        $('#input-signal-per-timespan').val(signalData.cardinality);
        $('#sel-signal-per-timeunit').val(signalData.cardinality_timeunit);
        if (signalData.field) {
            $('#input-signal-field').val(signalData.field);
        }
        $('#input-signal-threshold').val(signalData.threshold);
        $('#sel-signal-is').val(signalData.comparison);
        $('#input-signal-exceeded-count').val(signalData.limit);
        $('#input-signal-last-timespan').val(signalData.timespan);
        $('#sel-signal-last-timeunit').val(signalData.timespan_timeunit);

        $('#sel-signal-when, #input-signal-per-timespan, #input-signal-last-timespan, #input-signal-check-timespan').trigger('change');
    }

    function showOrHideRecipients() {
        if ($('.etm-notifier option:selected[data-type="EMAIL"]').length > 0) {
            $('#block-recipients').show();
            if ($('#block-recipients').next('br').length == 0) {
                $('#block-recipients').after($('<br>'));
            }
        } else {
            $('#block-recipients').hide().next('br').remove();
        }
    }

    function enableOrDisableButtons() {
        // First check if we can show the visualize buttion
        // Remove all required constraints
        $('#signal_form input[data-required]').removeAttr('required');
        // Set the required constrains on the visible fields for visualization
        $('#signal_form :input[data-required-visualization]:visible').attr('required', 'required');
        if (document.getElementById('signal_form').checkValidity()) {
            $('#btn-visualize-signal').removeAttr('disabled');
        } else {
            $('#btn-visualize-signal').attr('disabled', 'disabled');
        }

        // Now set the required constrains on all visible (required) fields.
        $('#signal_form :input[data-required]:visible').attr('required', 'required');
        // Remove the required constrains on the hidden fields
        $('#signal_form :input[data-required]:hidden').removeAttr('required');

        var notifierCount = createSignalData().notifiers.length;

        if (notifierCount >= 1 && document.getElementById('signal_form').checkValidity()) {
            $('#btn-confirm-save-signal').removeAttr('disabled');
        } else {
            $('#btn-confirm-save-signal').attr('disabled', 'disabled');
        }
        var signalName = $('#input-signal-name').val();
        if (signalName && isSignalExistent(signalName)) {
            $('#btn-confirm-remove-signal').removeAttr('disabled');
        } else {
            $('#btn-confirm-remove-signal').attr('disabled', 'disabled');
        }
    }

    function createSignalData() {
		var signalData = {
			name: $('#input-signal-name').val() ? $('#input-signal-name').val() : null,
			data_source: $('#sel-data-source').val(),
			query: $('#input-signal-query').val() ? $('#input-signal-query').val() : null,
			interval: $('#input-signal-check-timespan').val() ? Number($('#input-signal-check-timespan').val()) : null,
			interval_timeunit: $('#sel-signal-check-timeunit').val() ? $('#sel-signal-check-timeunit').val() : null,

		    operation: $('#sel-signal-when').val(),
		    cardinality: $('#input-signal-per-timespan').val() ? Number($('#input-signal-per-timespan').val()) : null,
		    cardinality_timeunit: $('#sel-signal-per-timeunit').val(),
		    field: $('#input-signal-field').val() ? $('#input-signal-field').val() : null,
		    comparison: $('#sel-signal-is').val(),
		    threshold: $('#input-signal-threshold').val() ? Number($('#input-signal-threshold').val()) : null,

		    limit: $('#input-signal-exceeded-count').val() ? Number($('#input-signal-exceeded-count').val()) : null,
		    timespan: $('#input-signal-last-timespan').val() ? Number($('#input-signal-last-timespan').val()) : null,
		    timespan_timeunit: $('#sel-signal-last-timeunit').val(),

		    notifiers: [],
		    email_recipients: [],
		    email_all_etm_group_members: $('#sel-email-all-group-members').val() == 'true' ? true : false
		};

        $('.etm-notifier').each(function () {
            var notifierName = $(this).val();
            if (-1 == signalData.notifiers.indexOf(notifierName)) {
                signalData.notifiers.push(notifierName);
            }
        });

        if ($('.etm-notifier option:selected[data-type="EMAIL"]').length > 0) {
            $('.etm-recipient').each(function () {
                var email = $(this).val();
                if (email && -1 == signalData.email_recipients.indexOf(email)) {
                    signalData.email_recipients.push(email);
                }
            });
        }

		return signalData;
    }

    function createNotifierRow(notifierName) {
    	var notifierRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
			$('<div>').addClass('input-group').append(
				$notifierSelect.clone(true),
				$('<div>').addClass('input-group-append').append(
                    $('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').click(function (event) {
                        event.preventDefault();
                        removeRowFromList($(this));
                        showOrHideRecipients();
                        enableOrDisableButtons();
                    })
                )
			)
		);
    	if (notifierName) {
    		$(notifierRow).find('.etm-notifier').val(notifierName)
    	}
    	return notifierRow;
    }

    function createRecipientRow(email) {
    	var recipientRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
			$('<div>').addClass('input-group').append(
				$('<input>').addClass('form-control form-control-sm etm-recipient').attr('type', 'email').attr('placeholder', 'user@host.com'),
				$('<div>').addClass('input-group-append').append(
                    $('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').click(function (event) {
                        event.preventDefault();
                        removeRowFromList($(this));
                    })
                )
			)
		);
    	if (email) {
    		$(recipientRow).find('.etm-recipient').val(email)
    	}
    	return recipientRow;
    }

    function removeRowFromList(anchor) {
        anchor.parent().parent().parent().remove();
    }

    function saveSignal() {
        var signalData = createSignalData();
        $.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: contextRoot + 'signal/' + encodeURIComponent(signalData.name),
            cache: false,
            data: JSON.stringify(signalData),
            success: function(data) {
                if (!data) {
                    return;
                }
                if (!isSignalExistent(signalData.name)) {
                    $signalSelect = $('#sel-signal');
                    $signalSelect.append($('<option>').attr('value', signalData.name).text(signalData.name));
                    sortSelectOptions($signalSelect);
                }
                signalMap[signalData.name] = signalData;
                $('#signals_infoBox').text('Signal \'' + signalData.name + '\' saved.').show('fast').delay(5000).hide('fast');
                enableOrDisableButtons();
            }
        }).always(function () {
            hideModals($('#modal-signal-overwrite'));
        });
    }

    function removeSignal(signalName) {
        $.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: contextRoot + 'signal/' + encodeURIComponent(signalName),
            cache: false,
            success: function(data) {
                if (!data) {
                    return;
                }
                delete signalMap[signalName];
                $("#sel-signal > option").filter(function(i){
                   return $(this).attr("value") == signalName;
                }).remove();
                $('#signals_infoBox').text('Signal \'' + signalName + '\' removed.').show('fast').delay(5000).hide('fast');
                enableOrDisableButtons();
            }
        }).always(function () {
            hideModals($('#modal-signal-remove'));
        });
    }

    function visualize() {
		var signalData = createSignalData();
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: contextRoot + 'visualize',
            cache: false,
            data: JSON.stringify(signalData),
            success: function(data) {
                if (!data) {
                    return;
                }
                $('#preview_box').empty();
                formatter = d3.locale(data.d3_formatter);
                numberFormatter = formatter.numberFormat(',f');
                nv.addGraph(function() {
                    var i = 1
                    var chart = nv.models.lineChart()
                        .showYAxis(true)
                        .showXAxis(true)
                        .useInteractiveGuideline(true)
                        .showLegend(true)
                        .duration(250)
                        ;
                    chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
                    chart.xAxis.tickFormat(function(d,s) {
                        if (d < 0 || d >= data.data[0].values.length) {
                            return '';
                        };
                        return data.data[0].values[d].label;
                    });
                    chart.margin({left: 75, bottom: 50, right: 50});
                    d3.select('#preview_box').append('svg').attr('style', 'height: 20em;')
                        .datum(formatLineData(data.data))
                        .call(chart);

                    return chart;
                });
                if (data.exceeded_count && signalData.limit) {
                    if (data.exceeded_count >= signalData.limit) {
                        $('#preview_box').append($('<p>').addClass('text-danger').text('The configured threshold is exceeded ' + data.exceeded_count + ' times and tops the maximum of ' + signalData.limit + '! This would have triggered a notification.'));
                    } else if (data.exceeded_count > 0) {
                        $('#preview_box').append($('<p>').text('The configured threshold is exceeded ' + data.exceeded_count + ' times but stays within the maximum of ' + signalData.limit + '.'));
                    }
                }
        		$('html,body').animate({scrollTop: $("#preview_box").parent().parent().offset().top },'fast');
            }
        });

        function formatLineData(lineData) {
            var formattedData = [];
            $.each(lineData, function(index, serie) {
                var serieData = {
                    key: serie.key,
                    values: []
                };
                var thresholdData = {
                    key: 'Threshold',
                    values: []
                };

                $.each(serie.values, function(serieIndex, point) {
                    serieData.values.push(
                        {
                            x: serieIndex,
                            y: point.value
                        }
                    );
                    if ($('#input-signal-threshold').val()) {
                        thresholdData.values.push(
                            {
                                x: serieIndex,
                                y: Number($('#input-signal-threshold').val())
                            }
                        );
                    }
                });
                formattedData.push(serieData);
                if ($('#input-signal-threshold').val()) {
                    formattedData.push(thresholdData);
                }
            });
            formattedData.sort(function(a, b){
                if (a.key < b.key) return -1;
                if (b.key < a.key) return 1;
                return 0;
            });
            return formattedData;
        }
    }

    function isSignalExistent(signalName) {
        return "undefined" != typeof signalMap[signalName];
    }

}