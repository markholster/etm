/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you 
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

'use strict';
// The additional search parameters in the search card.
const additionalSearchParameters = {
    // The fields that should be shown on the search card.
    params: []
};

// The search result layout.
const searchResultLayout = {
    fields: [
        {name: 'Timestamp', field: 'timestamp', format: 'ISOTIMESTAMP', array: 'LOWEST', link: true},
        {name: 'Name', field: 'name', format: 'PLAIN', array: 'FIRST', link: false}
    ],
    results_per_page: 50,
    sort_field: 'timestamp',
    sort_order: 'DESC',
    timestamp: null,
    current_ix: 0,
    current_query: null,
    getFieldNames: function () {
        return this.fields.map(function (item, index) {
            return item.name;
        })
    },
    getInternalFieldNames: function () {
        return this.fields.map(function (item, index) {
            return item.field;
        })
    },
    getValueFromSearchResult: function (searchResult, resultColumn, timeZone) {
        let values = [];
        let result = '';
        if ('_id' === resultColumn.field) {
            values.push(searchResult.id);
        } else {
            searchResultLayout.retrieveValuesFromSource(searchResult.source, resultColumn.field, values);
        }
        if (values.length === 0) {
            result = ''
        } else if (values.length === 1) {
            result = searchResultLayout.formatValue(values[0], resultColumn, timeZone);
        } else {
            // When changed also change the QueryExporter.java with the same functionality.
            if ('FIRST' === (resultColumn.array)) {
                result = searchResultLayout.formatValue(values[0], resultColumn, timeZone);
            } else if ('LAST' === (resultColumn.array)) {
                result = searchResultLayout.formatValue(values[values.length - 1], resultColumn, timeZone);
            } else if ('LOWEST' === (resultColumn.array)) {
                result = searchResultLayout.formatValue(values.sort()[0], resultColumn, timeZone);
            } else if ('HIGHEST' === (resultColumn.array)) {
                result = searchResultLayout.formatValue(values.sort()[values.length - 1], resultColumn, timeZone);
            } else if ('ALL' === (resultColumn.array)) {
                result = $.map(values, function (item) {
                    searchResultLayout.formatValue(item, resultColumn, timeZone)
                }).join(", ");
            } else {
                result = searchResultLayout.formatValue(values[0], resultColumn, timeZone);
            }
        }
        if (resultColumn.link && (typeof result === "undefined" || result == null || (!commons.isNumeric(result) && result.trim().length === 0))) {
            result = '?';
        }
        return result;
    },
    retrieveValuesFromSource: function (source, field, values) {
        const currentObject = source;
        const fieldParts = field.split("\.");

        const container = currentObject[fieldParts[0]];
        if (container) {
            if (Array.isArray(container)) {
                $(container).each(function (i, v) {
                    searchResultLayout.retrieveValuesFromSource(v, fieldParts.slice(1).join('.'), values);
                });
            } else if (fieldParts.length === 1) {
                values.push(container);
            } else {
                searchResultLayout.retrieveValuesFromSource(container, fieldParts.slice(1).join('.'), values);
            }
        }
    },
    formatValue: function (value, resultColumn, timeZone) {
        // When changed also change the QueryExporter.java with the same functionality.
        if ('PLAIN' === (resultColumn.format)) {
            return value;
        } else if ('ISOUTCTIMESTAMP' === resultColumn.format) {
            if (value) {
                return moment.tz(value, 'UTC').format('YYYY-MM-DDTHH:mm:ss.SSSZ');
            } else {
                return '';
            }
        } else if ('ISOTIMESTAMP' === resultColumn.format) {
            if (value) {
                return moment.tz(value, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ');
            } else {
                return '';
            }
        }
        return value;
    }
};

let queryKeywords = null;
let maxNumberOfSearchTemplates;
let maxNumberOfHistoricalQueries;
let maxNumberOfEventsInDownload;
let timeZone;
let currentSelectedFile;
let queryInProgress;
let maxDownloads = 0;
const queryTemplates = [];
const queryHistory = [];

const flatpickrRangeStart = {
    dateFormat: "Y-m-dTH:i:S",
    enableTime: true,
    enableSeconds: true,
    time_24hr: true,
    allowInput: true,
    defaultHour: 0,
    defaultMinute: 0,
    clickOpens: false,
    wrap: true
};

const flatpickrRangeEnd = {
    dateFormat: "Y-m-dTH:i:S",
    enableTime: true,
    enableSeconds: true,
    time_24hr: true,
    allowInput: true,
    defaultHour: 23,
    defaultMinute: 59,
    clickOpens: false,
    wrap: true
};

const multiSelectOptions = {
    buttonContainer: '<div class="btn-group w-100" />',
    buttonClass: 'form-control-sm btn-light border w-100 text-left',
    nonSelectedText: '',
    maxHeight: 500,
    templates: {
        button: '<button type="button" class="multiselect" data-toggle="dropdown"><span class="multiselect-selected-text"></span> <i class="fas fa-sort float-right mt-1 mr-1"></i></button>'
    }
};

// =================================
// Event listeners
// =================================
$('body').on('click', 'a', function (event) {
    const $anchor = $(this);
    if ($anchor.attr('data-action') && ('export-query' !== $anchor.attr('data-action'))) {
        event.preventDefault();
    }
    if ('show-event' === $anchor.attr('data-action')) {
        $('#search_result_table div.event-selected').removeClass('event-selected');
        $(event.target).parent().parent().addClass('event-selected');
        showEvent($(this).parent().parent().attr('data-event-id'));
    } else if ('edit-additional-search-parameters' === $anchor.attr('data-action')) {
        editAdditionalSearchParameters();
    } else if ('edit-result-table' === $anchor.attr('data-action')) {
        editSearchResultTable();
    } else if ('export-query' === $anchor.attr('data-action')) {
        exportQuery();
    } else if ('import-query' === $anchor.attr('data-action')) {
        $('#modal-query-import').modal();
    } else if ('download-results' === $anchor.attr('data-action')) {
        $('#modal-download-results').modal();
    } else if ('show-more-results' === $anchor.attr('data-action')) {
        showMoreResults();
    } else if ('search-history-selection' === $anchor.attr('data-action')) {
        const ix = $('#list-search-history-links > li > a').index($anchor);
        if (ix >= 0) {
            setQuery(queryHistory[ix]);
        }
    } else if ('search-template-selection' === $anchor.attr('data-action')) {
        const ix = $('#list-template-links > li > a[data-action="search-template-selection"]').index($anchor);
        if (ix >= 0) {
            setQuery(queryTemplates[ix]);
        }
    } else if ('search-template-removal' === $anchor.attr('data-action')) {
        const ix = $('#list-template-links > li > a[data-action="search-template-removal"]').index($anchor);
        if (ix >= 0) {
            $('#remove-template-name').text(queryTemplates[ix].name);
            $('#modal-template-remove').modal();
        }
    }
});

$('#template-name').on('input', function () {
    if (!$(this).val()) {
        $('#btn-save-template').attr('disabled', 'disabled');
    } else {
        $('#btn-save-template').removeAttr('disabled');
    }
});

$('#query-string').on('input', function () {
    if (!$(this).val()) {
        $('#btn-search, #template-name, #btn-save-template').attr('disabled', 'disabled');
        $('#link-edit-table, #link-request-download').hide();
    } else {
        $('#btn-search, #template-name').removeAttr("disabled");
        $('#link-edit-table').show();
        if (maxNumberOfEventsInDownload && maxNumberOfEventsInDownload > 0) {
            $('#link-request-download').show();
        }
        $('#template-name').trigger('input');
    }
}).on('keydown', function (event) {
    if (event.keyCode === 13 && !event.shiftKey) {
        event.preventDefault();
        const $searchButton = $('#btn-search');
        if ($searchButton.is(':enabled')) {
            $searchButton.trigger('click');
        }
    }
});

$('#btn-search').on('click', function () {
    startNewQuery();
});

$('#btn-save-template').on('click', function () {
    const template = createQuery(true);
    delete template.result_layout.timestamp;
    template.result_layout.current_query = $('#query-string').val();
    template.result_layout.current_ix = 0;

    const current = $('#list-template-links > li > a').filter(function () {
        return $(this).text() === template.name;
    }).toArray();
    if (current.length !== 0) {
        $('#overwrite-template-name').text(template.name);
        $('#modal-template-overwrite').modal();
    } else {
        addSearchTemplate(template, true);
    }
});

$('#btn-overwrite-template').on('click', function () {
    const template = createQuery(true);
    delete template.result_layout.timestamp;
    template.result_layout.current_query = $('#query-string').val();
    template.result_layout.current_ix = 0;
    addSearchTemplate(template, true);
});

$('#btn-remove-template').on('click', function (event) {
    event.preventDefault();
    const templateName = $('#remove-template-name').text();
    $.ajax({
        type: 'DELETE',
        contentType: 'application/json',
        url: '../rest/search/templates/' + encodeURIComponent(templateName),
        cache: false,
        success: function () {
            commons.showNotification('Search template \'' + templateName + '\' removed.', 'success');
            let currentIx = -1;
            $.each(queryTemplates, function (index, availableTemplate) {
                if (templateName === availableTemplate.name) {
                    currentIx = index;
                    return false;
                }
            });

            $('#list-template-links').children().slice(currentIx, currentIx + 1).remove();
            queryTemplates.splice(currentIx, 1);
            validateMaxTemplates();
        }
    }).always(function () {
        commons.hideModals($('#modal-template-remove'));
    });
});

$('#query-import-file').on('change', function (event) {
    const files = event.target.files;
    if (files.length > 0) {
        currentSelectedFile = files[0];
    } else {
        currentSelectedFile = null;
    }
});

$('#btn-import-query').on('click', function (event) {
    event.preventDefault();
    if (currentSelectedFile) {
        const reader = new FileReader();
        reader.onload = function (e) {
            const queryData = JSON.parse(e.target.result);
            if ('undefined' == typeof queryData) {
                return;
            }
            setQuery(queryData);
        };
        reader.readAsText(currentSelectedFile);
    }
    commons.hideModals($('#modal-query-import'));
});

$('#btn-apply-additional-search-parameters').on('click', function (event) {
    event.preventDefault();
    additionalSearchParameters.params.splice(0, additionalSearchParameters.params.length);
    const $modal = $('#modal-additional-search-parameters');
    let $rows = $modal.find('.modal-body > #block-param-fields > div');
    $rows.each(function (index, row) {
        if (index === 0) {
            return true;
        }
        let $row = $(row);
        const params = {};
        params.id = $row.attr('data-row-id');
        $row.find('input, select').each(function (ix, item) {
            if (ix === 0) {
                params.name = $(item).val();
            } else if (ix === 1) {
                params.field = $(item).val();
            } else if (ix === 2) {
                params.type = $(item).val();
            } else if (ix === 3) {
                params.default_value = $(item).val();
            }
        });
        if (params.name && params.field) {
            additionalSearchParameters.params.push(params);
        }
    });
    $rows = $modal.find('.modal-body > #block-param-relations > div');
    $rows.each(function (index, row) {
        const $row = $(row);
        const forRowId = $row.attr('data-for-row');
        if (forRowId) {
            const params = $.grep(additionalSearchParameters.params, function (elem) {
                return forRowId === elem.id;
            })[0];
            params.relates_to = [];
            const references = $row.find('select').val();
            $.each(references, function (ix, ref) {
                const refIx = additionalSearchParameters.params.map(function (r) {
                    return r['id'];
                }).indexOf(ref);
                if (refIx >= 0) {
                    params.relates_to.push(refIx);
                }
            });
        }
    });
    additionalSearchParameters.params.forEach(function (p) {
        delete p.id
    });
    displayAdditionalSearchParameters();
    const clonedParams = $.extend(true, {}, additionalSearchParameters);
    $.each(clonedParams.params, function (index, params) {
        delete params['flatpickr'];
    });
    $.ajax({
        type: 'PUT',
        contentType: 'application/json',
        url: '../rest/search/additionalparameters',
        cache: false,
        data: JSON.stringify(clonedParams),
        success: function () {
            commons.showNotification('Additional search parameters saved.', 'success');
        }
    }).always(function () {
        commons.hideModals($modal);
    });
});

$('#btn-apply-table-settings').on('click', function () {
    let changed = false;
    if (searchResultLayout.sort_field !== $('#table-settings-sort-field').val()) {
        changed = true;
        searchResultLayout.sort_order = $('#table-settings-sort-field').val();
    }
    if (searchResultLayout.sort_order !== $('#table-settings-sort-order').val()) {
        changed = true;
        searchResultLayout.sort_order = $('#table-settings-sort-order').val();
    }
    if (commons.isNumeric($('#table-settings-results-per-page').val()) && searchResultLayout.results_per_page !== parseInt($('#table-settings-results-per-page').val())) {
        changed = true;
        searchResultLayout.results_per_page = parseInt($('#table-settings-results-per-page').val());
    }
    const fields = [];
    $('#table-settings-columns .fieldConfigurationRow').each(function (rowIndex, row) {
        const rowData = {name: '', field: '', format: 'PLAIN', array: 'FIRST', link: false};
        $(row).children().each(function (columnIndex, column) {
            if (0 === columnIndex) {
                rowData.name = $(column).find('input').val();
            } else if (1 === columnIndex) {
                rowData.field = $(column).find('input').val();
            } else if (2 === columnIndex) {
                rowData.format = $(column).find('select').val();
            } else if (3 === columnIndex) {
                rowData.array = $(column).find('select').val();
            } else if (4 === columnIndex) {
                rowData.link = $(column).find('input').prop('checked');
            }
        });
        if (rowData.name.trim().length > 0 && rowData.field.trim().length > 0) {
            fields.push(rowData);
        }
    });

    if (JSON.stringify(fields) !== JSON.stringify(searchResultLayout.fields)) {
        changed = true;
    }
    searchResultLayout.fields = fields;
    if (changed && $('#result_block').is(':visible')) {
        searchResultLayout.timestamp = new Date().getTime();
        searchResultLayout.current_ix = 0;
        $('#result_card').empty();
        executeQuery(createQuery(false));
    }
    commons.hideModals($('#modal-table-settings'));
});

$('#btn-download-results').on('click', function (event) {
    event.preventDefault();
    $('#modal-download-results').modal('hide');
    const q = createQuery(false);
    q.result_layout.current_ix = Number($('#input-download-start-row').val()) - 1;
    q.result_layout.results_per_page = Number($('#input-download-number-of-rows').val());
    q.fileType = $('#sel-download-type').val();
    q.includePayload = $('#sel-download-include-payload').val() === 'true';
    window.location.href = '../rest/search/download?q=' + encodeURIComponent(JSON.stringify(q));
});

$('#result_card').on('click', '#search_result_table .header-row div', function (event) {
    event.preventDefault();
    sortResultTable($(this).attr('data-event-field'));
}).on('mouseenter mouseleave', '#search_result_table .result-row', function () {
    $(this).toggleClass('bg-light').find("[data-link-type='filter-on-transaction']").toggleClass('invisible');
});

$('#block-additional-parameters').on('change', 'select,input', function (event) {
    event.preventDefault();
    updateAdditionSearchParametersDistinctData();
});

// =================================
// Functions
// =================================
/**
 * Build the search page. This is the main entry point for the page.
 */
function buildSearchPage() {
    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/search/userdata',
        cache: false,
        success: function (data) {
            if (!data) {
                return;
            }
            maxNumberOfSearchTemplates = data.max_number_of_search_templates;
            maxNumberOfHistoricalQueries = data.max_number_of_historical_queries;
            maxNumberOfEventsInDownload = data.max_number_of_events_in_download;
            timeZone = data.timeZone;
            // Set the autocomplete on the query field
            if (data.keywords) {
                queryKeywords = data.keywords;
                $('#query-string').bind('keydown', function (event) {
                    if (event.which === $.ui.keyCode.SPACE && event.ctrlKey && !$(this).autocomplete('instance').menu.active) {
                        $(this).autocomplete("search", $(this).val());
                    }
                }).autocompleteFieldQuery(
                    {
                        queryKeywords: queryKeywords,
                        allowJoins: true
                    }
                );
                $('#table-settings-sort-field').on('keydown', function (event) {
                    if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                        event.stopPropagation();
                    }
                }).autocompleteFieldQuery(
                    {
                        queryKeywords: queryKeywords,
                        mode: 'field'
                    });
            }
            // Add the additional query parameters
            if (data.additional_query_parameters) {
                additionalSearchParameters.params.push(...data.additional_query_parameters);
                displayAdditionalSearchParameters();
            }
            // Add the search history
            if (data.search_history) {
                $.each(data.search_history.reverse(), function (index, query) {
                    addQueryHistory(query);
                });
            }
            // Add the search templates
            if (data.search_templates) {
                $.each(data.search_templates, function (index, template) {
                    addSearchTemplate(template, false);
                });
            }
            // Set the max downloads
            if (maxNumberOfEventsInDownload) {
                $('#input-download-number-of-rows').attr('max', maxNumberOfEventsInDownload);
            }
        }
    });
}

/**
 * Add a search template to the templates card.
 * @param template The template to add.
 * @param persist Whether or not the template needs to be persisted.
 */
function addSearchTemplate(template, persist) {
    let currentIx = -1;
    $.each(queryTemplates, function (index, availableTemplate) {
        if (template.name === availableTemplate.name) {
            currentIx = index;
            return false;
        }
    });
    if (currentIx === -1) {
        $('#list-template-links').append(
            $('<li>').append(
                $('<a href="#" data-action="search-template-selection">').text(template.name),
                $('<a href="#" data-action="search-template-removal" class="fa fa-times text-danger float-right">')
            )
        );
        queryTemplates.push(template);
        validateMaxTemplates();
    } else {
        queryTemplates[currentIx] = template;
    }
    if (persist) {
        $.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/search/templates/' + encodeURIComponent(template.name),
            cache: false,
            data: JSON.stringify(template),
            success: function () {
                commons.showNotification('Search template \'' + template.name + '\' saved.', 'success');
                $('#template-name').val('').trigger('input');
            }
        }).always(function () {
            commons.hideModals($('#modal-template-overwrite'));
        });
    }
}

/**
 * Checks if the max number of search templates is reached. If so, the user is warned no more templates can be added.
 */
function validateMaxTemplates() {
    if ($('#list-template-links').children().length >= maxNumberOfSearchTemplates) {
        $('#no-more-templates-allowed').show();
        $('#template-save-group').hide();
    } else {
        $('#no-more-templates-allowed').hide();
        $('#template-save-group').show();
    }
}

/**
 * Register a query as a previous executed query.
 * @param query The query to register as historical query.
 */
function addQueryHistory(query) {
    if (!query.result_layout) {
        return true;
    }
    const $links = $('#list-search-history-links');
    let currentIx = -1;
    $.each(queryHistory, function (index, historicalQuery) {
        if (query.result_layout.current_query === historicalQuery.result_layout.current_query) {
            currentIx = index;
            return false;
        }
    });
    if (currentIx > -1) {
        queryHistory.splice(currentIx, 1);
        $links.children().eq(currentIx).remove();
    }
    const historyLength = queryHistory.unshift(query);
    $links.prepend(
        $('<li>').append(
            $('<a href="#" data-action="search-history-selection">')
                .text(query.result_layout.current_query)
                .attr('title', query.result_layout.current_query)
        )
    ).children().slice(maxNumberOfHistoricalQueries).remove();
    if (historyLength > maxNumberOfHistoricalQueries) {
        queryHistory.splice(maxNumberOfHistoricalQueries, queryHistory.length - maxNumberOfHistoricalQueries);
    }
}

/**
 * Restore the values of a query to the input elements of the screen.
 * @param query The query to restore.
 */
function setQuery(query) {
    searchResultLayout.fields = query.result_layout.fields;
    searchResultLayout.results_per_page = query.result_layout.results_per_page;
    searchResultLayout.sort_field = query.result_layout.sort_field;
    searchResultLayout.sort_order = query.result_layout.sort_order;
    searchResultLayout.current_query = query.result_layout.current_query;
    searchResultLayout.current_ix = 0;
    const $queryField = $('#query-string').val(searchResultLayout.current_query);

    $.each(query.additional_query_parameters, function (index, param) {
        const matchedParams = $.grep(additionalSearchParameters.params, function (item) {
            return item.field === param.field && item.type === param.type;
        });
        if (matchedParams && matchedParams.length > 0) {
            const paramIx = additionalSearchParameters.params.indexOf(matchedParams[0]);
            $('#block-additional-parameters').find('[data-param-ix="' + paramIx + '"]').val(param.value);
        } else if (param.value) {
            let currentQuery = $queryField.val();
            if (currentQuery) {
                currentQuery += ' AND ';
            }
            if ('RANGE_START' === param.type) {
                currentQuery += param.field + ':>=' + param.value;
            } else if ('RANGE_END' === param.type) {
                currentQuery += param.field + ':<=' + param.value;
            } else if ('SELECT_MULTI') {
                if (Array.isArray(param.value)) {
                    currentQuery += param.field + ': (' + param.value.map(function (p) {
                        if (isNaN(p)) {
                            return '"' + p + '"';
                        }
                        return p;
                    }).join(' OR ') + ")";
                } else {
                    currentQuery += param.field + ':"' + param.value + '"';
                }
            } else {
                currentQuery += param.field + ':' + param.value
            }

            $queryField.val(currentQuery);
        }
    });
    $('#block-additional-parameters select[multiple="multiple"]').multiselect('refresh');

    if (query.name) {
        $('#template-name').val(query.name).trigger('input');
    }
    $queryField.trigger('input');
}

/**
 * Create a query object from the current input elements of the screen.
 * @param named Whether or not the template should have the name element.
 */
function createQuery(named) {
    const query = {};
    query.result_layout = $.extend(true, {}, searchResultLayout);
    if (named) {
        query.name = $('#template-name').val();
    }
    const clonedParams = $.extend(true, {}, additionalSearchParameters);
    const $additionalParamsBlock = $('#block-additional-parameters');
    $.each(clonedParams.params, function (index, params) {
        delete params['flatpickr'];
        delete params['name'];
        delete params['default_value'];
        delete params['relates_to'];
        delete params['related_from'];
        const $field = $additionalParamsBlock.find('[data-param-ix="' + index + '"]');
        if ('' === $field.val()) {
            params.value = null;
        } else if ('number' === $field.attr('type')) {
            params.value = Number($field.val());
        } else {
            params.value = $field.val();
        }
    });
    query.additional_query_parameters = clonedParams.params;
    return query;
}

/**
 * Create a search template from the current input, and export the template to a file.
 */
function exportQuery() {
    const anchor = document.createElement('a');
    const queryData = createQuery(false);
    const blob = new Blob([JSON.stringify(queryData)], {'type': 'application/json'});
    anchor.href = window.URL.createObjectURL(blob);
    anchor.download = 'query-export.json';
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
}

/**
 * Show the additional search parameters modal.
 */
function editAdditionalSearchParameters() {
    const $modal = $('#modal-additional-search-parameters');
    $modal.find('.modal-body > #block-param-fields').empty().append(
        $('<div>').addClass('row mr-1 ml-1').append(
            $('<div>').addClass('col-sm-2 font-weight-bold').text('Name'),
            $('<div>').addClass('col-sm-5 font-weight-bold').text('Field'),
            $('<div>').addClass('col-sm-2 font-weight-bold').text('Type'),
            $('<div>').addClass('col-sm-2 font-weight-bold').text('Default value'),
            $('<div>').addClass('col-sm-1` font-weight-bold')
                .append($('<a href="#">').text('Add row')
                    .click(function (event) {
                        event.preventDefault();
                        $(this).parent().parent().parent().append(createRow);
                        updateRowActions();
                        updateRelationOptions();
                    })
                )
        )
    );
    $modal.find('.modal-body > #block-param-relations').empty().append(
        $('<div>').addClass('row mr-1 ml-1').append(
            $('<div>').addClass('col-sm-2 font-weight-bold').text('Name'),
            $('<div>').addClass('col-sm-5 font-weight-bold').text('Relates to')
        )
    );
    // Create the fields.
    $.each(additionalSearchParameters.params, function (index, params) {
        $modal.find('.modal-body > #block-param-fields').append(createRow(params));
        updateRelationOptions();
    });
    // Set the relates to values.
    $.each(additionalSearchParameters.params, function (index, params) {
        if (!params.relates_to || params.relates_to.length <= 0) {
            return true;
        }
        // Add 1 to the index because we want to skip the header.
        const rowId = $modal.find('.modal-body > #block-param-fields > div').eq(index + 1).attr('data-row-id');
        const selectedValues = $.grep($modal.find('.modal-body > #block-param-fields > div').toArray().slice(1), function (item, index) {
            return params.relates_to.indexOf(index) !== -1;
        }).map(function (r) {
            return $(r).attr('data-row-id')
        });
        const $select = $modal.find('.modal-body > #block-param-relations > [data-for-row="' + rowId + '"] select');
        $select.val(selectedValues);
        $select.multiselect('rebuild');
    });
    updateRowActions();
    $modal.modal();

    function removeRow(row) {
        const $row = $(row);
        $modal.find('.modal-body > #block-param-relations > [data-for-row="' + $row.attr('data-row-id') + '"]').remove();
        $row.remove();
        updateRowActions();
        updateRelationOptions();
    }

    function moveRowUp(row) {
        const $row = $(row);
        $row.after($row.prev());
        updateRowActions();
    }

    function moveRowDown(row) {
        const $row = $(row);
        $row.before($row.next());
        updateRowActions();
    }

    function createRow(data) {
        const $row = $('<div>').addClass('row mr-1 ml-1 mt-1').attr("data-row-id", commons.generateUUID());
        $row.append(
            $('<div>').addClass('col-sm-2').append($('<input>').attr('type', 'text').addClass('form-control form-control-sm').attr('placeholder', 'Name...')
                .change(function (event) {
                    event.preventDefault();
                    updateFieldName($row);
                })
            ),
            $('<div>').addClass('col-sm-5').append($('<input>').attr('type', 'text').addClass('form-control form-control-sm').attr('placeholder', 'Field...')
                .bind('keydown', function (event) {
                    if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                        event.stopPropagation();
                    }
                })
                .autocompleteFieldQuery(
                    {
                        queryKeywords: queryKeywords,
                        mode: 'field'
                    }
                )
            ),
            $('<div>').addClass('col-sm-2').append($('<select>').addClass('form-control form-control-sm custom-select custom-select-sm')
                .append($('<option>').attr('value', 'FREE_FORMAT').text('Free format'))
                .append($('<option>').attr('value', 'RANGE_START').text('Range start'))
                .append($('<option>').attr('value', 'RANGE_END').text('Range end'))
                .append($('<option>').attr('value', 'SELECT').text('Select'))
                .append($('<option>').attr('value', 'SELECT_MULTI').text('Select multi'))
            ).change(function (event) {
                event.preventDefault();
                updateRelationFields($row)
            }),
            $('<div>').addClass('col-sm-2').append($('<input>').attr('type', 'text').addClass('form-control form-control-sm').attr('placeholder', 'Default value...')),
            $('<div>').addClass('col-sm-1').append(
                $('<div>').addClass('row').append(
                    $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-up').click(function (event) {
                        event.preventDefault();
                        moveRowUp($row)
                    })),
                    $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-down').click(function (event) {
                        event.preventDefault();
                        moveRowDown($row)
                    })),
                    $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-times text-danger').click(function (event) {
                        event.preventDefault();
                        removeRow($row)
                    }))
                )
            )
        );
        if (data) {
            $row.find('input, select').each(function (index, item) {
                if (index === 0) {
                    $(item).val(data.name);
                } else if (index === 1) {
                    $(item).val(data.field);
                } else if (index === 2) {
                    $(item).val(data.type);
                    updateRelationFields($row);
                } else if (index === 3) {
                    $(item).val(data.default_value);
                }
            });
        }
        return $row;
    }

    function updateRowActions() {
        const $rows = $modal.find('.modal-body > div > div');
        $rows.each(function (index, row) {
            if (index === 0) {
                return true;
            }
            if ($rows.length >= 3) {
                if (index === 1) {
                    $(row).find('.fa-arrow-up').hide();
                } else {
                    $(row).find('.fa-arrow-up').show();
                }
                if (index >= $rows.length - 1) {
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

    function updateRelationFields(row) {
        const $row = $(row);
        const rowId = $row.attr('data-row-id');
        const type = $row.find('select').val();
        const name = $row.find('input').first().val();
        if ('SELECT' === type || 'SELECT_MULTI' === type) {
            if (!$modal.find('.modal-body > #block-param-relations > [data-for-row="' + rowId + '"]').length) {
                const $select = $('<select class="form-control form-control-sm custom-select custom-select-sm" multiple>');
                $modal.find('.modal-body > #block-param-relations').append(
                    $('<div>').addClass('row mr-1 ml-1 mt-1').attr('data-for-row', rowId).append(
                        $('<div>').addClass('col-sm-2').text(name),
                        $('<div>').addClass('col-sm-5 font-weight-bold').append($select)
                    )
                );
                $select.multiselect(multiSelectOptions);
                updateRelationOptions();
            }
        } else {
            $modal.find('.modal-body > #block-param-relations > [data-for-row="' + rowId + '"]').remove();
            updateRelationOptions();
        }
    }

    function updateRelationOptions() {
        const $selects = $modal.find('.modal-body > #block-param-relations select');
        if (!$selects.length) {
            return;
        }
        const allOptions = [];
        $.each($modal.find('.modal-body > #block-param-fields > div'), function (index, div) {
            const $div = $(div);
            const forRow = $div.attr('data-row-id');
            if (!forRow) {
                return true;
            }
            const forName = $div.find('input').first().val();
            allOptions.push($('<option>').attr('value', forRow).text(forName));
        });
        $.each($selects, function (index, select) {
            const $select = $(select);
            const rowId = $select.closest('[data-for-row]').attr('data-for-row');
            const values = $select.val();
            $select.empty();
            $.each(allOptions, function (index, option) {
                const $option = $(option);
                if (rowId !== $option.attr('value')) {
                    $select.append($option.clone());
                }
            });
            $select.val(values);
        });
        $selects.multiselect('rebuild');
    }

    function updateFieldName(row) {
        const $row = $(row);
        const rowId = $row.attr('data-row-id');
        const name = $row.find('input').first().val();
        $modal.find('.modal-body > #block-param-relations > [data-for-row="' + rowId + '"]').children('div').first().text(name);
        const $selects = $modal.find('.modal-body > #block-param-relations select');
        $selects.find('option[value="' + rowId + '"]').text(name);
        $selects.multiselect('rebuild');
    }
}

/**
 * Show the additional search parameters on the screen.
 */
function displayAdditionalSearchParameters() {
    const $block = $('#block-additional-parameters').empty();
    $('#block-additional-search-parameters-explanation').toggle(additionalSearchParameters.params.length === 0);
    let $currentRow;
    $.each(additionalSearchParameters.params, function (index, params) {
        if (!$currentRow || index % 2 === 0) {
            $currentRow = $('<div class="row mt-1">');
            $block.append($currentRow);
        }
        $currentRow.append(
            $('<div class="col-6" data-param-id="' + index + '">').append(
                $('<div class="form-group row">').append(
                    $('<div class="col-sm-3">').append($('<label class="col-form-label-sm">').text(params.name)),
                    $('<div class="col-sm">').append(function () {
                        const keyword = $.grep(queryKeywords[0].keywords, function (item) {
                            return item.name === params.field;
                        })[0];
                        if (!keyword) {
                            return $('<p class="text-warning">').text('Field \'' + params.field + '\' no longer exists');
                        } else if (keyword.date) {
                            const $div = $('<div class="input-group flatpickr">').append(
                                $('<input class="form-control form-control-sm" data-input>').attr('data-param-ix', index),
                                $('<div class="input-group-append">').append(
                                    $('<a class="btn btn-outline-secondary btn-sm form-control-sm" title="Toggle calendar" data-toggle>').append(
                                        $('<i class="fa fa-calendar">')
                                    )
                                )
                            );
                            if ('RANGE_END' === params.type) {
                                params.flatpickr = $div.flatpickr(flatpickrRangeEnd);
                            } else {
                                params.flatpickr = $div.flatpickr(flatpickrRangeStart);
                            }
                            if (params.default_value) {
                                $div.find('input').val(params.default_value);
                            }

                            return $div;
                        } else if ('SELECT' === params.type || 'SELECT_MULTI' === params.type) {
                            const $select = $('<select class="form-control form-control-sm custom-select custom-select-sm">').attr('data-param-ix', index);
                            if ('SELECT_MULTI' === params.type) {
                                $select.attr('multiple', 'multiple');
                            } else {
                                $select.append($('<option value="">').text(""));
                            }
                            if (params.default_value) {
                                if ('SELECT_MULTI' === params.type) {
                                    const values = params.default_value.split(',');
                                    $.each(values, function (ix, value) {
                                        $select.append($('<option value="' + value + '">').text(value));
                                    });
                                    $select.val(values);
                                } else {
                                    if (params.default_value) {
                                        $select.append($('<option value="' + params.default_value + '">').text(params.default_value));
                                        $select.val(params.default_value);
                                    }

                                }
                            }
                            return $select;
                        }
                        const $input = $('<input class="form-control form-control-sm">').val(params.default_value).attr('data-param-ix', index);
                        if (keyword.number) {
                            $input.attr('type', 'number').attr('step', 'any')
                        }
                        return $input;
                    })
                )
            )
        );
    });

    $('#block-additional-parameters select[multiple="multiple"]').multiselect(multiSelectOptions);
    updateAdditionSearchParametersDistinctData();
}

/**
 * Update the select boxes in the search card with their distinct values.
 */
function updateAdditionSearchParametersDistinctData() {
    const distinctData = createDistinctData();
    $.ajax({
        type: 'POST',
        contentType: 'application/json',
        url: '../rest/search/distinctvalues',
        cache: false,
        data: JSON.stringify(distinctData),
        success: function (data) {
            if (!data || !data.fields) {
                return;
            }
            $.each(data.fields, function (index, field) {
                const params = additionalSearchParameters.params[field.id];
                if ('SELECT' === params.type || 'SELECT_MULTI' === params.type) {
                    const $select = $('#block-additional-parameters select[data-param-ix="' + field.id + '"]');
                    $select.children().remove();
                    if ('SELECT_MULTI' !== params.type) {
                        $select.append($('<option value="">').text(""));
                    }
                    $.each(field.distinct_values, function (ix, option) {
                        $select.append($('<option value="' + option + '">').text(option));
                    });
                    $select.val(field.value);
                    if ('SELECT_MULTI' === params.type) {
                        $select.multiselect('rebuild');
                    }
                }
            });
        }
    });
}

/**
 * Create the distinct data for the search card select boxes.
 */
function createDistinctData() {
    const distinctData = {
        fields: []
    };
    $.each(additionalSearchParameters.params, function (index, params) {
        const $field = $('[data-param-ix="' + index + '"]');
        let value = '';
        if ('' === $field.val()) {
            value = null;
        } else if ('number' === $field.attr('type')) {
            value = Number($field.val());
        } else {
            value = $field.val();
        }
        distinctData.fields.push(
            {
                field: params.field,
                type: params.type,
                relates_to: params.relates_to,
                value: value,
            }
        );
    });
    return distinctData;
}

/**
 * Show the search result layout table
 */
function editSearchResultTable() {
    $('#table-settings-sort-field').val(searchResultLayout.sort_field);
    $('#table-settings-sort-order').val(searchResultLayout.sort_order);
    $('#table-settings-results-per-page').val(searchResultLayout.results_per_page);
    const $tableSettingsColumns = $('#table-settings-columns').empty();
    $tableSettingsColumns.append(
        $('<div>').addClass('row').append(
            $('<div>').addClass('col-sm-2 font-weight-bold').text('Name'),
            $('<div>').addClass('col-sm-3 font-weight-bold').text('Field'),
            $('<div>').addClass('col-sm-2 font-weight-bold').text('Format'),
            $('<div>').addClass('col-sm-2 font-weight-bold').text('Multiselect'),
            $('<div>').addClass('col-sm-1 font-weight-bold').text('Link'),
            $('<div>').addClass('col-sm-2 font-weight-bold')
                .append($('<a href="#">').text('Add row')
                    .attr('id', 'link-add-result-row')
                    .click(function (event) {
                        event.preventDefault();
                        $tableSettingsColumns.append(createRow());
                        updateRowActions();
                    })
                )
        )
    );
    $(searchResultLayout.fields).each(function (index, column) {
        $tableSettingsColumns.append(function () {
                return createRow(column);
            }
        )
    });
    updateRowActions();
    $('#modal-table-settings').modal();

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

    function createRow(columnData) {
        const checkboxId = commons.generateUUID();
        const row = $('<div>').addClass('row fieldConfigurationRow').attr('style', 'margin-top: 5px;');
        $(row).append(
            $('<div>').addClass('col-sm-2').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($('<input>').attr('type', 'text').addClass('form-control form-control-sm').attr('placeholder', 'Name')),
            $('<div>').addClass('col-sm-3').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($('<input>').attr('type', 'text').addClass('form-control form-control-sm').attr('placeholder', 'Field')
                .bind('keydown', function (event) {
                    if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                        event.stopPropagation();
                    }
                })
                .autocompleteFieldQuery(
                    {
                        queryKeywords: queryKeywords,
                        mode: 'field'
                    }
                )),
            $('<div>').addClass('col-sm-2').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($('<select>').addClass('form-control form-control-sm custom-select custom-select-sm')
                .append($('<option>').attr('value', 'PLAIN').text('Plain'))
                .append($('<option>').attr('value', 'ISOTIMESTAMP').text('ISO Timestamp'))
                .append($('<option>').attr('value', 'ISOUTCTIMESTAMP').text('ISO UTC Timestamp'))),
            $('<div>').addClass('col-sm-2').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($('<select>').addClass('form-control form-control-sm custom-select custom-select-sm')
                .append($('<option>').attr('value', 'LOWEST').text('Lowest'))
                .append($('<option>').attr('value', 'HIGHEST').text('Highest'))
                .append($('<option>').attr('value', 'FIRST').text('First'))
                .append($('<option>').attr('value', 'LAST').text('Last'))
                .append($('<option>').attr('value', 'ALL').text('All'))),
            $('<div>').addClass('col-sm-1').append(
                $('<div>').addClass('custom-control custom-checkbox').append(
                    $('<input>').attr('type', 'checkbox').addClass('form-control form-control-sm custom-control-input').attr('id', checkboxId),
                    $('<label>').addClass('custom-control-label').attr('for', checkboxId)
                )
            )
        );
        const actionDiv = $('<div>').addClass('col-sm-2').append(
            $('<div>').addClass('row actionRow').append(
                $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-up').click(function (event) {
                    event.preventDefault();
                    moveRowUp(row)
                })),
                $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-down').click(function (event) {
                    event.preventDefault();
                    moveRowDown(row)
                })),
                $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-times text-danger').click(function (event) {
                    event.preventDefault();
                    removeRow(row)
                }))
            )
        );
        $(row).append($(actionDiv));
        if (columnData) {
            $(row).children().each(function (index, column) {
                if (0 === index) {
                    $(column).find('input').val(columnData.name);
                } else if (1 === index) {
                    $(column).find('input').val(columnData.field);
                } else if (2 === index) {
                    $(column).find('select').val(columnData.format);
                } else if (3 === index) {
                    $(column).find('select').val(columnData.array);
                } else if (4 === index) {
                    $(column).find('input').prop('checked', columnData.link);
                }
            });
        }
        return row;
    }

    function updateRowActions() {
        $('#table-settings-columns .actionRow').each(function (index, row) {
            if ($tableSettingsColumns.children().length > 2) {
                if (index === 0) {
                    $(row).find('.fa-arrow-up').hide();
                } else {
                    $(row).find('.fa-arrow-up').show();
                }
                if (index >= $('#table-settings-columns').children().length - 2) {
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
}

/**
 * Sort the result table.
 * @param fieldName The field name to sort on.
 */
function sortResultTable(fieldName) {
    if (queryInProgress) {
        return false;
    }
    if (searchResultLayout.sort_field === fieldName) {
        if (searchResultLayout.sort_order === 'ASC') {
            searchResultLayout.sort_order = 'DESC';
        } else {
            searchResultLayout.sort_order = 'ASC';
        }
    } else {
        searchResultLayout.sort_field = fieldName;
    }
    searchResultLayout.timestamp = new Date().getTime();
    searchResultLayout.current_ix = 0;
    $('#result_card').empty();
    executeQuery(createQuery(false));
}

/**
 * Start a new query. This function will reset the query index start position and timestamp.
 */
function startNewQuery() {
    searchResultLayout.timestamp = new Date().getTime();
    searchResultLayout.current_query = $('#query-string').val();
    searchResultLayout.current_ix = 0;
    $('#result_card').empty();
    executeQuery(createQuery(false));
}

/**
 * Execute a query.
 * @param queryParameters The query parameters.
 * @param successCallback The function to be executed when te result table is appended with the received results.
 */
function executeQuery(queryParameters, successCallback) {
    if (!queryParameters || queryInProgress) {
        return;
    }
    queryInProgress = true;
    $('#result_block').show();
    $.ajax({
        type: 'POST',
        contentType: 'application/json',
        url: '../rest/search/query',
        cache: false,
        data: JSON.stringify(queryParameters),
        success: function (data) {
            if (!data) {
                return;
            }
            if (data.warning) {
                commons.showNotification('Query returned a warning: ' + data.warning, 'warning');
            }
            $('#lnk-export-query').show();
            maxDownloads = data.max_downloads;
            if (maxDownloads > 0) {
                $('#link-request-download').show();
                $('#input-download-number-of-rows').attr('max', maxDownloads);
            }
            searchResultLayout.current_ix = data.start_ix;
            const $searchStats = $('#search-stats');
            const hadPreviousResults = $('#result_card').children().length !== 0;
            if (hadPreviousResults) {
                const endIx = $searchStats.text().lastIndexOf(' ');
                $searchStats.text($searchStats.text().substring(0, endIx + 1) + (data.end_ix + 1) + '.');
            } else {
                if (data.hits === 0) {
                    $searchStats.text(':  No results found in ' + data.query_time_as_string + 'ms.');
                    $('#result_card').append($('<p>').text('No results found'));
                } else {
                    $searchStats.text(':  Found ' + data.hits_as_string + ' results in ' + data.query_time_as_string + 'ms. Showing results ' + (data.start_ix + 1) + ' to ' + (data.end_ix + 1) + '.');
                    const $resultTable = $('<div id="search_result_table">');
                    $resultTable.append(function () {
                        const $row = $('<div class="row header-row">');
                        $(searchResultLayout.fields).each(function (index, tableColumn) {
                            const $col = $('<div style="cursor: pointer;" class="col font-weight-bold">').text(tableColumn.name).attr('data-event-field', tableColumn.field);
                            if (tableColumn.field === searchResultLayout.sort_field) {
                                if ('ASC' === searchResultLayout.sort_order) {
                                    $col.addClass('headerSortAsc')
                                } else {
                                    $col.addClass('headerSortDesc')
                                }
                            }
                            $row.append($col)
                        });
                        $row.append($('<div class="col-1">'));
                        return $row;
                    });
                    $('#result_card').append($resultTable);
                }
            }

            const $results = $('#search_result_table');
            $(data.results).each(function (index, searchResult) {
                $results.append(function () {
                    return createResultTableRow(searchResult, data.time_zone);
                })
            });
            if (data.has_more_results) {
                $results.append(
                    $('<div class="row footer-row">')
                        .append($('<div class="col">').addClass('text-center').attr('colspan', $(searchResultLayout.fields).length)
                            .append(
                                $('<a href="#">')
                                    .attr('id', 'lnk_show_more')
                                    .attr('data-action', 'show-more-results')
                                    .text('Show more')
                            )
                        )
                )
            }
            if (!commons.isInViewport($('#result_card'))) {
                $('html,body').animate({scrollTop: $('#query-string').offset().top}, 'slow');
            }
            if (queryParameters.result_layout.current_ix === 0) {
                addQueryHistory(queryParameters);
            }
            if (successCallback) {
                successCallback();
            }
        },
        complete: function () {
            queryInProgress = false;
        }
    });

    function createResultTableRow(searchResult, timeZone) {
        const $row = $('<div class="row result-row">').attr('data-event-id', searchResult.id);
        $(searchResultLayout.fields).each(function (index, field) {
            const $col = $('<div class="col">');
            const fieldValue = searchResultLayout.getValueFromSearchResult(searchResult, field, timeZone);
            if (field.link) {
                $col.append(
                    $('<a>')
                        .attr('href', '?id=' + encodeURIComponent(searchResult.id))
                        .attr('data-action', 'show-event')
                        .attr('title', fieldValue)
                        .text(fieldValue)
                );
            } else {
                $col.append($('<span>').attr('title', fieldValue).text(fieldValue));
            }
            $row.append($col);
            if (index === searchResultLayout.fields.length - 1) {
                const transIds = [];
                if (searchResult.source.endpoints) {
                    $.each(searchResult.source.endpoints, function (eIx, endpoint) {
                        if (endpoint.endpoint_handlers) {
                            $.each(endpoint.endpoint_handlers, function (ehIx, endpointHandler) {
                                if (endpointHandler.transaction_id) {
                                    if (transIds.indexOf(endpointHandler.transaction_id) === -1) {
                                        transIds.push(endpointHandler.transaction_id);
                                    }
                                }
                            });
                        }
                    });
                }
                const $filterDiv = $('<div class="col-1">');
                if (transIds.length > 0) {
                    $filterDiv.append(
                        $('<a>')
                            .addClass('fa fa-filter float-right invisible')
                            .attr('data-link-type', 'filter-on-transaction')
                            .attr('title', "Filter on transaction")
                            .attr('href', '#')
                            .on('click', function (event) {
                                event.preventDefault();
                                const $queryField = $('#query-string')
                                let currentQuery = $queryField.val();
                                if (currentQuery) {
                                    currentQuery += ' AND ';
                                }
                                currentQuery += 'endpoints.endpoint_handlers.transaction_id: (' + transIds.map(function (p) {
                                    return '"' + p + '"';
                                }).join(' OR ') + ")";
                                $queryField.val(currentQuery).trigger('input');
                                $('#btn-search').trigger('click');
                            })
                    );
                }
                $row.append($filterDiv);
            }
        });
        return $row;
    }
}

/**
 * Load the next batch of matching results.
 * @param callback A callback function that will be called when the results are added to the result table.
 */
function showMoreResults(callback) {
    $('#search_result_table .footer-row').remove();
    searchResultLayout.current_ix += searchResultLayout.results_per_page;
    const query = createQuery(false);
    executeQuery(query, callback);
}

$(window).scroll(function () {
    const $showMoreLink = $('#lnk_show_more');
    if (commons.isInViewport($showMoreLink)) {
        $showMoreLink.click();
    }
});