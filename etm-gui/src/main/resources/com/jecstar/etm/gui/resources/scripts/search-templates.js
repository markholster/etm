let max_search_templates;
let timeZone;

$('#template-name').on('input', function() {
    if (!$(this).val()) {
        $('#btn-save-template').attr('disabled', 'disabled');
    } else {
        $('#btn-save-template').removeAttr('disabled');
    }
});

$('#btn-save-template').on('click', function () {
    const template = createTemplate();
    const current = $('#list-template-links > li > a').filter(function () {
        return $(this).text() === template.name;
    }).toArray();
    if (current.length !== 0) {
        $('#overwrite-template-name').text(template.name);
        $('#modal-template-overwrite').modal();
    } else {
        storeTemplate(template, false);
    }
});

$('#btn-overwrite-template').on('click', function () {
    storeTemplate(createTemplate(), true);
});

$('#btn-remove-template').on('click', function (event) {
    event.preventDefault();
    const templateName = $('#remove-template-name').text();
    $.ajax({
        type: 'DELETE',
        contentType: 'application/json',
        url: '../rest/search/templates/' + encodeURIComponent(templateName),
        cache: false,
        success: function() {
            commons.showNotification('Search template \'' + templateName + '\' removed.', 'success');
            $('#list-template-links > li > a').filter(function() {
                    return $(this).text() === templateName;
                }).parent().remove();
            validateMaxTemplates();
        }
    }).always(function () {
        commons.hideModals($('#modal-template-remove'));
    });
});


// Load the stores search templates.
$.ajax({
    type: 'GET',
    contentType: 'application/json',
    url: '../rest/search/userdata',
    cache: false,
    success: function(data) {
        if (!data) {
            return;
        }
        max_search_templates = data.max_search_templates;
        if (data.search_templates) {
            $.each(data.search_templates, function(index, template){
                $('#list-template-links').append(
                    $('<li>').append(
                        $('<a href="#">').on('click', function (event) {
                           event.preventDefault();
                            setValuesFromTemplate(template)
                        }).text(template.name),
                        $('<a href="#" class="fa fa-times text-danger float-right">').on('click', function (event) {
                           event.preventDefault();
                           askRemoveTemplate(template)
                        })
                    )
                );
            });
            validateMaxTemplates();
        }

        if (data.default_search_range) {
            $('#query-string-from').val('now-' + (data.default_search_range / 1000) + 's');
            $('#query-string-till').val('now')
        }

        if (data.search_history) {
            data.search_history.reverse();
            $.each(data.search_history, function (index, query) {
                $('#list-search-history-links').append(
                    $('<li>').append(
                        $('<a href="#">').on('click', function (event) {
                                event.preventDefault();
                            setValuesFromHistory(query)
                            })
                            .text(query.query)
                            .attr('title', query.query)
                    )
                );
            });
        }
        timeZone = data.timeZone;
    }
});

function validateMaxTemplates() {
	if ($('#list-template-links').children().length >= max_search_templates ) {
		$('#no-more-tenplates-allowed').show();
		$('#template-save-group').hide();
	} else {
		$('#no-more-tenplates-allowed').hide();
		$('#template-save-group').show();		
	}
}

function setValuesFromTemplate(template) {
    // the template and query object are exactly the same.
    setValuesFromHistory(template);
    $('#template-name').val(template.name).trigger('input');
}

function setValuesFromHistory(query) {
    $('[id^=check-type-]').prop('checked', false);
    $.each(query.types, function(index, type){
        $('#check-type-' + type).prop('checked', true);    
    });
    $('#query-string').val(query.query);
    if (query.start_time) {
        const momentValue = moment(query.start_time, 'x', true);
        if (momentValue.isValid() && timeZone) {
            $('#query-string-from').val(momentValue.tz(timeZone).format('YYYY-MM-DDTHH:mm:ss'));
        } else {
            $('#query-string-from').val(query.start_time);
        }
    } else {
        flatPickrStartDate.setDate(null);
    }
    if (query.end_time) {
        const momentValue = moment(query.end_time, 'x', true);
        if (momentValue.isValid() && timeZone) {
            $('#query-string-till').val(momentValue.tz(timeZone).format('YYYY-MM-DDTHH:mm:ss'));
        } else {
            $('#query-string-till').val(query.end_time);
        }
    } else {
        flatPickrEndDate.setDate(null);
    }
    $('#query-string-time-filter-field').val(query.time_filter_field ? query.time_filter_field : 'timestamp');
    tableLayout.current_ix = 0;
    tableLayout.fields = query.fields;
    tableLayout.results_per_page = query.results_per_page;
    tableLayout.sort_field = query.sort_field;
    tableLayout.sort_order = query.sort_order;
    $('#query-string').removeAttr('disabled').trigger('input');
}

function updateHistory(query, max_size) {
	$('#list-search-history-links > li > a[title="' + query.query.replace(/(")/g, "\\$1" ) + '"]').parent().remove();
    $('#list-search-history-links').prepend(
            $('<li>').append(
                $('<a href="#">')
                .click(function(event) {
                   event.preventDefault();
                   setValuesFromHistory(query)
                })
                .text(query.query)
                .attr('title', query.query)
            )
     ).children().slice(max_size).remove();
    
}

function createTemplate() {
    const template = {
        name: $('#template-name').val(),
        query: $('#query-string').val(),
        types: $('[id^=check-type-]:checked').map(function(){ return $(this).val(); }).get(),
        fields: tableLayout.fields,
        results_per_page: tableLayout.results_per_page,
        sort_field: tableLayout.sort_field,
        sort_order: tableLayout.sort_order,
        start_time: null,
        end_time: null,
        time_filter_field: null
    };
    let dateValue = $('#query-string-from').val();
    if (dateValue) {
        const momentValue = moment(dateValue, moment.HTML5_FMT.DATETIME_LOCAL_SECONDS, true);
        if (momentValue.isValid()) {
            template.start_time = Number(momentValue.valueOf());
        } else {
            template.start_time = dateValue;
        }
    }
    dateValue = $('#query-string-till').val();
    if (dateValue) {
        const momentValue = moment(dateValue, moment.HTML5_FMT.DATETIME_LOCAL_SECONDS, true);
        if (momentValue.isValid()) {
            template.end_time = Number(momentValue.valueOf());
        } else {
            template.end_time = dateValue;
        }
    }
    template.time_filter_field = $('#query-string-time-filter-field').val();
    return template;
}

function storeTemplate(template, isOverwrite) {
    $.ajax({
        type: 'PUT',
        contentType: 'application/json',
        url: '../rest/search/templates/' + encodeURIComponent($('#template-name').val()),
        cache: false,
        data: JSON.stringify(template),
        success: function() {
            commons.showNotification('Search template \'' + template.name + '\' saved.', 'success');
            $('#template-name').val('');
            $('#btn-save-template').attr('disabled', 'disabled');
            if (isOverwrite) {
                $('#list-template-links > li > a').filter(function() {
                    return $(this).text() === template.name;
                }).unbind('click').click(function(event) {
                    event.preventDefault();
                    setValuesFromTemplate(template)
                });
            } else {
                $('#list-template-links').append(
                    $('<li>').append(
                        $('<a href="#">').on('click', function (event) {
                           event.preventDefault();
                           setValuesFromTemplate(template)
                        }).text(template.name),
                        $('<a href="#" class="fa fa-times float-right text-danger">').on('click', function (event) {
                           event.preventDefault();
                           askRemoveTemplate(template)
                        })
                    )
                );
                validateMaxTemplates();                
            }
        }
    }).always(function () {
        commons.hideModals($('#modal-template-overwrite'));
    });
}

function askRemoveTemplate(template) {
    $('#remove-template-name').text(template.name);
    $('#modal-template-remove').modal();
}
