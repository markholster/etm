function buildSignalsPage(groupName) {
    'use strict';
    const $notifierSelect = $('<select>').addClass('form-control form-control-sm custom-select custom-select-sm etm-notifier').attr('required', 'required');
    let contextRoot = '../rest/signal/';
    if (groupName) {
        contextRoot += encodeURIComponent(groupName) + '/';
        $('#block-email-all-group-members').show();
    }
    const signalMap = {};
    let keywords = [];
    let timeZone;
    const $page = $('body > .container-fluid');


    const originalFromValue = $('#input-signal-from').val();
    const originalTillValue = $('#input-signal-till').val();

    $('#input-signal-from').val('').parent()
        .flatpickr({
            dateFormat: "Y-m-dTH:i:S",
            enableTime: true,
            enableSeconds: true,
            time_24hr: true,
            allowInput: true,
            defaultHour: 0,
            defaultMinute: 0,
            clickOpens: false,
            wrap: true
        });
    $('#input-signal-till').val('').parent()
        .flatpickr({
            dateFormat: "Y-m-dTH:i:S",
            enableTime: true,
            enableSeconds: true,
            time_24hr: true,
            allowInput: true,
            defaultHour: 23,
            defaultMinute: 59,
            clickOpens: false,
            wrap: true
        });
    if (originalFromValue) {
        $('#input-signal-from').val(originalFromValue);
    }
    if (originalTillValue) {
        $('#input-signal-till').val(originalTillValue)
    }

    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: contextRoot + 'contextdata',
        cache: false,
        success: function (data) {
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

            if (!data) {
                return;
            }
            if (data.signal_datasources) {
                const $dsSelect = $('#sel-data-source');
                $.each(data.signal_datasources, function (index, datasource) {
                    $dsSelect.append($('<option>').attr('value', datasource).text(datasourceToText(datasource)));
                });
                commons.sortSelectOptions($dsSelect);

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
                commons.sortSelectOptions($notifierSelect);
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
        aggregators.initialize({
            keywords: keywords,
            enableOrDisableButtons: enableOrDisableButtons
        });
        $page.on('input autocomplete:selected', 'input', enableOrDisableButtons);
        $page.on('change', 'select', enableOrDisableButtons);
        $page.on('change', '.etm-notifier', showOrHideRecipients);

        $('#input-signal-query').on('keydown', function (event) {
            if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                event.stopPropagation();
            }
        }).autocompleteFieldQuery(
            {
                queryKeywords: keywords,
                keywordIndexFilter: function(index) {
                    return index !== $('#sel-data-source').val();
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
            const $signalSelect = $('#sel-signal');
            $.each(data.signals, function(index, signal) {
                $signalSelect.append($('<option>').attr('value', signal.name).text(signal.name));
                signalMap[signal.name] = signal;
            });
            commons.sortSelectOptions($signalSelect);
            $signalSelect.val('');
            timeZone = data.timeZone;
        }
    });


    $('#lnk-add-notifier').on('click', function (event) {
        event.preventDefault();
        $('#list-notifiers').append(createNotifierRow());
        showOrHideRecipients();
        enableOrDisableButtons();
    });

    $('#lnk-add-recipient').on('click', function (event) {
        event.preventDefault();
        $('#list-recipients').append(createRecipientRow());
    });

    $('#sel-signal').on('change', function (event) {
        event.preventDefault();
        const signalData = signalMap[$(this).val()];
        if ('undefined' == typeof signalData) {
            resetValues();
            return;
        }
        setValuesFromData(signalData);
        enableOrDisableButtons();
        showOrHideRecipients();
    });

    $('#input-signal-interval, #input-signal-cardinality').on('change', function () {
        const subtype = $(this).attr('id').substring(13);
        const $signalTimeunit = $('#sel-signal-' + subtype + '-timeunit');
        if (Number($(this).val()) !== 1) {
            $signalTimeunit.children('#' + subtype + '-minute').text('Minutes');
            $signalTimeunit.children('#' + subtype + '-hour').text('Hours');
            $signalTimeunit.children('#' + subtype + '-day').text('Days');
        } else {
            $signalTimeunit.children('#' + subtype + '-minute').text('Minute');
            $signalTimeunit.children('#' + subtype + '-hour').text('Hour');
            $signalTimeunit.children('#' + subtype + '-day').text('Day');
        }
    });

    $('#input-signal-max-frequency-of-exceedance').on('change', function () {
        if (Number($(this).val()) !== 1) {
	        $('#label-exceeded-count-time').text('times');
	    } else {
	        $('#label-exceeded-count-time').text('time');
	    }
	});

    $('#btn-confirm-save-signal').on('click', function (event) {
        event.preventDefault();
        const signalName = $('#input-signal-name').val();
        if (isSignalExistent(signalName)) {
            $('#overwrite-signal-name').text(signalName);
            $('#modal-signal-overwrite').modal();
        } else {
            saveSignal();
        }
    });

    $('#btn-save-signal').on('click', function () {
        saveSignal();
    });

    $('#btn-confirm-remove-signal').on('click', function (event) {
        event.preventDefault();
        $('#remove-signal-name').text($('#input-signal-name').val());
        $('#modal-signal-remove').modal();
    });

    $('#btn-remove-signal').on('click', function () {
        removeSignal($('#input-signal-name').val());
    });

    $('#btn-visualize-signal').on('click', function (event) {
        event.preventDefault();
        visualize();
    });

    function resetValues() {
        $('#input-signal-name').val('');
        $('#sel-signal-enabled').val('true');
        document.getElementById('form-data').reset();
        document.getElementById('form-threshold').reset();
        document.getElementById('form-notifications').reset();
        $('div[data-aggregator-level="0"] > div.bucket-aggregator-header').nextAll().remove();
        $('#list-notifiers').empty();
        $('#list-recipients').empty();
        $('#lnk-add-notifier').trigger('click');
        enableOrDisableButtons();
        showOrHideRecipients();
        $('#input-signal-interval, #input-signal-cardinality').trigger('change');
    }

    function setValuesFromData(signalData) {
        $('#input-signal-name').val(signalData.name);
        $('#sel-signal-enabled').val(signalData.enabled ? 'true' : 'false');
        $('#sel-data-source').val(signalData.data.data_source);

        let momentValue = moment(signalData.data.from, 'x', true);
        if (momentValue.isValid() && timeZone) {
            $('#input-signal-from').val(momentValue.tz(timeZone).format('YYYY-MM-DDTHH:mm:ss'));
        } else {
            $('#input-signal-from').val(signalData.data.from);
        }
        momentValue = moment(signalData.data.till, 'x', true);
        if (momentValue.isValid() && timeZone) {
            $('#input-signal-till').val(momentValue.tz(timeZone).format('YYYY-MM-DDTHH:mm:ss'));
        } else {
            $('#input-signal-till').val(signalData.data.from);
        }
        $('#input-signal-time-filter-field').val(signalData.data.time_filter_field);
        $('#input-signal-query').val(signalData.data.query);


        $('#sel-signal-comparison').val(signalData.threshold.comparison);
        $('#input-signal-threshold').val(signalData.threshold.value);
        $('#input-signal-cardinality').val(signalData.threshold.cardinality);
        $('#sel-signal-cardinality-timeunit').val(signalData.threshold.cardinality_timeunit);
        if (signalData.threshold.aggregators) {
            $('div[data-aggregator-level="0"] > div.bucket-aggregator-header').nextAll().remove();
            const $aggregatorContainer = $('div[data-aggregator-level="0"]');
            $.each(signalData.threshold.aggregators, function (index, aggregator) {
                if ('metrics' === aggregator.type) {
                    aggregators.addMetricsAggregator('threshold', $aggregatorContainer, 0, index, aggregator);
                } else if ('bucket' === aggregator.type) {
                    aggregators.addBucketAggregator('threshold', $aggregatorContainer, 1, index, aggregator);
                } else if ('pipeline' === aggregator.type) {
                    aggregators.addPipelineAggregator('threshold', $aggregatorContainer, 0, index, aggregator);
                }
            });
        }

        $('#input-signal-interval').val(signalData.notifications.interval);
        $('#sel-signal-interval-timeunit').val(signalData.notifications.interval_timeunit);
        $('#input-signal-max-frequency-of-exceedance').val(signalData.notifications.max_frequency_of_exceedance);
        $('#list-notifiers').empty();
        if (signalData.notifications.notifiers) {
            $.each(signalData.notifications.notifiers, function (index, notifierName) {
                $('#list-notifiers').append(createNotifierRow(notifierName));
            });
        }
        $('#list-recipients').empty();
        if (signalData.notifications.email_recipients) {
            $('#sel-email-all-group-members').val(signalData.notifications.email_all_etm_group_members ? 'true' : 'false');
            $.each(signalData.email_recipients, function(index, email) {
                $('#list-recipients').append(createRecipientRow(email));
            });
        }
        $('#input-signal-interval, #input-signal-cardinality, [id^=sel-bucket-aggregator-], [id^=sel-metrics-aggregator-], [id^=sel-pipeline-aggregator-]').trigger('change');
    }

    function showOrHideRecipients() {
        const $recipients = $('#block-recipients');
        if ($('.etm-notifier option:selected[data-type="EMAIL"]').length > 0) {
            $recipients.show();
            if ($recipients.next('br').length === 0) {
                $recipients.after($('<br>'));
            }
        } else {
            $recipients.hide().next('br').remove();
        }
    }

    function enableOrDisableButtons() {
        // First check if we can show the visualize button
        let valid = validateForm('form-data');
        valid = valid && validateForm('form-threshold');
        valid = valid && aggregators.validateNumberOfMetricsAndPipelines('form-threshold');
        if (valid) {
            $('#btn-visualize-signal').removeAttr('disabled');
        } else {
            $('#btn-visualize-signal').attr('disabled', 'disabled');
        }

        const $signalName = $('#input-signal-name');
        valid = valid && $signalName[0].checkValidity();
        valid = valid && validateForm('form-notifications');

        const notifierCount = createSignalData().notifications.notifiers.length;

        if (notifierCount >= 1 && valid) {
            $('#btn-confirm-save-signal').removeAttr('disabled');
        } else {
            $('#btn-confirm-save-signal').attr('disabled', 'disabled');
        }
        if ($signalName.val() && isSignalExistent($signalName.val())) {
            $('#btn-confirm-remove-signal').removeAttr('disabled');
        } else {
            $('#btn-confirm-remove-signal').attr('disabled', 'disabled');
        }

        function validateForm(formId) {
            const $form = $('#' + formId);
            $form.find('div.form-group').each(function () {
                if ($(this).css('display') === 'none') {
                    $(this).find('[data-required="required"]').removeAttr('required');
                }
            });

            let valid = false;
            if ($form[0].checkValidity()) {
                valid = true;
            }
            $form.find('[data-required]').attr('required', 'required');
            return valid;
        }
    }

    function createSignalData() {
        const signalName = $('#input-signal-name').val();
        const dataSource = $('#sel-data-source').val();
        const signalFrom = $('#input-signal-from').val();
        const signalTill = $('#input-signal-till').val();
        const signalTimeFilterField = $('#input-signal-time-filter-field').val();
        const signalQuery = $('#input-signal-query').val();
        const signalData = {
            name: signalName ? signalName : null,
            enabled: $('#sel-signal-enabled').val() === 'true',
            data: {
                data_source: dataSource,
                from: signalFrom ? signalFrom : null,
                till: signalTill ? signalTill : null,
                time_filter_field: signalTimeFilterField ? signalTimeFilterField : null,
                query: signalQuery ? signalQuery : null,
            },
            threshold: {
                comparison: $('#sel-signal-comparison').val(),
                value: Number($('#input-signal-threshold').val()),
                cardinality: Number($('#input-signal-cardinality').val()),
                cardinality_timeunit: $('#sel-signal-cardinality-timeunit').val(),
                aggregators: aggregators.createAggregatorData('threshold', $('#acc-collapse-threshold > .card-body > .aggregator-container-block'))
            },
            notifications: {
                interval: Number($('#input-signal-interval').val()),
                interval_timeunit: $('#sel-signal-interval-timeunit').val(),
                max_frequency_of_exceedance: Number($('#input-signal-max-frequency-of-exceedance').val()),
                notifiers: [],
                email_recipients: [],
                email_all_etm_group_members: $('#sel-email-all-group-members').val() === 'true'
            }
        };

        $('.etm-notifier').each(function () {
            const notifierName = $(this).val();
            if (-1 === signalData.notifications.notifiers.indexOf(notifierName)) {
                signalData.notifications.notifiers.push(notifierName);
            }
        });

        if ($('.etm-notifier option:selected[data-type="EMAIL"]').length > 0) {
            $('.etm-recipient').each(function () {
                const email = $(this).val();
                if (email && -1 === signalData.notifications.email_recipients.indexOf(email)) {
                    signalData.notifications.email_recipients.push(email);
                }
            });
        }
		return signalData;
    }

    function createNotifierRow(notifierName) {
        const notifierRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
			$('<div>').addClass('input-group').append(
				$notifierSelect.clone(true),
				$('<div>').addClass('input-group-append').append(
                    $('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').on('click', function (event) {
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
        const recipientRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
			$('<div>').addClass('input-group').append(
				$('<input>').addClass('form-control form-control-sm etm-recipient').attr('type', 'email').attr('placeholder', 'user@host.com'),
				$('<div>').addClass('input-group-append').append(
                    $('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').on('click', function (event) {
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
        const signalData = createSignalData();
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
                    const $signalSelect = $('#sel-signal');
                    $signalSelect.append($('<option>').attr('value', signalData.name).text(signalData.name));
                    commons.sortSelectOptions($signalSelect);
                }
                signalMap[signalData.name] = signalData;
                $('#signals_infoBox').text('Signal \'' + signalData.name + '\' saved.').show('fast').delay(5000).hide('fast');
                enableOrDisableButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-signal-overwrite'));
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
                $("#sel-signal > option").filter(function () {
                    return $(this).attr("value") === signalName;
                }).remove();
                $('#signals_infoBox').text('Signal \'' + signalName + '\' removed.').show('fast').delay(5000).hide('fast');
                enableOrDisableButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-signal-remove'));
        });
    }

    function visualize() {
        const signalData = createSignalData();
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: contextRoot + 'visualize',
            cache: false,
            data: JSON.stringify(signalData),
            success: function (response) {
                if (!response) {
                    return;
                }
                const $previewBox = $('#preview_box').empty();
                Highcharts.chart($previewBox.attr('id'), response.chart_config);

                if (response.exceeded_count && signalData.notifications.max_frequency_of_exceedance) {
                    if (response.exceeded_count >= signalData.notifications.max_frequency_of_exceedance) {
                        $previewBox.prepend($('<p>').addClass('text-danger').text('The configured threshold is exceeded ' + response.exceeded_count + ' times and tops the maximum of ' + signalData.notifications.max_frequency_of_exceedance + '! This would have triggered a notification.'));
                    } else if (response.exceeded_count > 0) {
                        $previewBox.prepend($('<p>').text('The configured threshold is exceeded ' + response.exceeded_count + ' times but stays within the maximum of ' + signalData.notifications.max_frequency_of_exceedance + '.'));
                    }
                }
                $('html,body').animate({scrollTop: $previewBox.parent().parent().offset().top}, 'fast');
            }
        });

    }

    function isSignalExistent(signalName) {
        return "undefined" != typeof signalMap[signalName];
    }
}