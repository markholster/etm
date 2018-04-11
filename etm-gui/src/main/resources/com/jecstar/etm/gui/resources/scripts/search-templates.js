var max_search_templates;

$('#template-name').on('input', function() {
    if (!$(this).val()) {
        $('#btn-save-template').attr('disabled', 'disabled');
    } else {
        $('#btn-save-template').removeAttr('disabled');
    }
});

$('#btn-save-template').click(function() {
    var template = createTemplate();
    var current = $('#list-template-links > li > a').filter(function() {
        return $(this).text() === template.name;
    }).toArray();
    if (current.length != 0) {
        $('#overwrite-template-name').text(template.name);
        $('#modal-template-overwrite').modal();
    } else {
        storeTemplate(template, false);
    }
});

$('#btn-overwrite-template').click(function() {
    hideModals($('#modal-template-overwrite'));
    storeTemplate(createTemplate(), true);
});

$('#btn-remove-template').click(function(event) {
    event.preventDefault();
    var templateName = $('#remove-template-name').text();
    $.ajax({
        type: 'DELETE',
        contentType: 'application/json',
        url: '../rest/search/templates/' + encodeURIComponent(templateName),
        cache: false,
        success: function() {
            $('#list-template-links > li > a').filter(function() {
                    return $(this).text() === templateName;
                }).parent().remove();
            validateMaxTemplates();
        }
    }).always(function () {
        hideModals($('#modal-template-remove'));
    });
});


// Load the stores search templates.
$.ajax({
    type: 'GET',
    contentType: 'application/json',
    url: '../rest/search/templates',
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
                        $('<a href="#">').click(function(event) {
                           event.preventDefault();
                           setValuesFromTemplate(template)
                        }).text(template.name),
                        $('<a href="#" class="fa fa-times pull-right text-danger">').click(function(event) {
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
    }
});

$.ajax({
    type: 'GET',
    contentType: 'application/json',
    url: '../rest/search/history',
    cache: false,
    success: function(data) {
        if (!data || !data.search_history) {
            return;
        }
        data.search_history.reverse();
        $.each(data.search_history, function(index, query){
            $('#list-search-history-links').append(
                $('<li>').append(
                    $('<a href="#">')
                    .click(function(event) {
                       event.preventDefault();
                       setValuesFromHistory(query)
                    })
                    .text(query.query)
                    .attr('title', query.query)
                )
            );
        });
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
        var momentValue = moment(query.start_time, 'x', true);
        if (momentValue.isValid()) {
            flatPickrStartDate.setDate(momentValue.toDate());
        } else {
            $('#query-string-from').val(query.start_time);
        }
    } else {
        flatPickrStartDate.setDate(null);
    }
    if (query.end_time) {
        var momentValue = moment(query.end_time, 'x', true);
        if (momentValue.isValid()) {
            flatPickrEndDate.setDate(momentValue.toDate());
        } else {
            $('#query-string-till').val(query.end_time);
        }
    } else {
        flatPickrEndDate.setDate(null);
    }
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
    var template = { 
        name: $('#template-name').val(),
        query: $('#query-string').val(),
        types: $('[id^=check-type-]:checked').map(function(){ return $(this).val(); }).get(),
        fields: tableLayout.fields,
        results_per_page: tableLayout.results_per_page,
        sort_field: tableLayout.sort_field,
        sort_order: tableLayout.sort_order,
        start_time: null,
        end_time: null
    };
    var dateValue = $('#query-string-from').val();
    if (dateValue) {
        var momentValue = moment(dateValue, moment.HTML5_FMT.DATETIME_LOCAL_SECONDS, true);
        if (momentValue.isValid()) {
            template.start_time = Number(momentValue.valueOf());
        } else {
            template.start_time = dateValue;
        }
    }
    var dateValue = $('#query-string-till').val();
    if (dateValue) {
        var momentValue = moment(dateValue, moment.HTML5_FMT.DATETIME_LOCAL_SECONDS, true);
        if (momentValue.isValid()) {
            template.end_time = Number(momentValue.valueOf());
        } else {
            template.end_time = dateValue;
        }
    }
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
                        $('<a href="#">').click(function(event) {
                           event.preventDefault();
                           setValuesFromTemplate(template)
                        }).text(template.name),
                        $('<a href="#" class="fa fa-times pull-right text-danger">').click(function(event) {
                           event.preventDefault();
                           askRemoveTemplate(template)
                        })
                    )
                );
                validateMaxTemplates();                
            }
        }
    });
}

function askRemoveTemplate(template) {
    $('#remove-template-name').text(template.name);
    $('#modal-template-remove').modal();
}
