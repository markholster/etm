var tableLayout = {
    fields: [
        {name: 'Timestamp', field: 'endpoints.writing_endpoint_handler.handling_time', format: 'isotimestamp', array: 'lowest', link: true }, 
        {name: 'Name', field: 'name', format: 'plain', array: 'first', link: false}
    ],
    results_per_page: 50,
    sort_field: 'endpoints.writing_endpoint_handler.handling_time',
    sort_order: 'desc',
    timestamp: null,
    current_ix: 0,
    current_query: null,
    getFieldNames: function () {
        return this.fields.map(function(item, index) {
            return item.name;
        })
    },
    getInternalFieldNames: function () {
        return this.fields.map(function(item, index) {
            return item.field;
        })
    },
    getValueFromSearchResult: function(searchResult, tableColumn, timeZone) {
        var fieldParts = tableColumn.field.split("\.");
        var values = [];
        var result = '';
        if ('_type' == tableColumn.field) {
        	values.push(searchResult.type);
        } else {
        	tableLayout.retrieveValuesFromSource(searchResult.source, fieldParts, values);
        }
        if (values.length == 0) {
            result = ''
        } else if (values.length == 1) {
            result = tableLayout.formatValue(values[0], tableColumn, timeZone);
        }
        // When changed also change the QueryExporter.java with the same functionality.
        if ('first' === (tableColumn.array)) {
            result = tableLayout.formatValue(values[0], tableColumn, timeZone);
        } else if ('last' === (tableColumn.array)) {
            result = tableLayout.formatValue(values[values.length -1], tableColumn, timeZone);
        } else if ('lowest' === (tableColumn.array)) {
            result = tableLayout.formatValue(values.sort()[0], tableColumn, timeZone);
        } else if ('highest' === (tableColumn.array)) {
            result = tableLayout.formatValue(values.sort()[values.length -1], tableColumn, timeZone);
        } else {
            result = tableLayout.formatValue(values[0], tableColumn, timeZone);
        }
        if (tableColumn.link && (typeof result === "undefined" || result == null || (!$.isNumeric(result) && result.trim().length == 0))) {
            result = '?';
        }
        return result;
    },
    retrieveValuesFromSource: function(source, fields, values) {
        var currentObject = source;
        $(fields).each(function (index, fieldName) {
            var container = currentObject[fieldName];
            if (container) {
                if (tableLayout.isArray(container)) {
                    $(container).each(function (i,v){
                        tableLayout.retrieveValuesFromSource(v, fields.slice(index + 1), values);
                    });
                } else {
                    currentObject = container;
                    if (index + 1 == fields.length) {
                        values.push(container);
                    }
                }
            } else {
                return true;
            }
        });
    },
    formatValue: function(value, tableColumn, timeZone) {
    	// When changed also change the QueryExporter.java with the same functionality.
        if ('plain' === (tableColumn.format)) {
            return value;
        } else if ('isoutctimestamp' === tableColumn.format) {
            if (value) {
                return moment.tz(value, 'UTC').format('YYYY-MM-DDTHH:mm:ss.SSSZ');
            } else {
                return ''; 
            }
        } else if ('isotimestamp' === tableColumn.format) {
            if (value) {
                return moment.tz(value, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ');
            } else {
                return ''; 
            }
        }
        return value;
    },
    isArray: function(fieldToTest) {
        return Object.prototype.toString.call(fieldToTest) === '[object Array]';
    },
}

$('#table-settings-sort-field').bind('keydown', function( event ) {
    if (event.keyCode === $.ui.keyCode.TAB && $(this).autocomplete('instance').menu.active) {
        event.preventDefault();
    } else if ( event.keyCode === $.ui.keyCode.DOWN && !$(this).autocomplete('instance').menu.active) {
        if ($(this).val() === '') {
            $(this).autocomplete( "search", "" );
        }
    } else if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
        event.stopPropagation();
    }
}).autocompleteFieldQuery(
{
	queryKeywords: queryKeywords, 
	mode: 'field',
	keywordGroupFilter: function(index, group) {
		return $('[id^=check-type-]:checked').map(function(){ return $(this).val(); }).get().indexOf(group) == -1;
    }
});
		

$('#link-edit-table').click(function (event) {
    event.preventDefault();
    $('#table-settings-sort-field').val(tableLayout.sort_field);
    $('#table-settings-sort-order').val(tableLayout.sort_order);
    $('#table-settings-results-per-page').val(tableLayout.results_per_page);
    $('#table-settings-columns').empty();
    $('#table-settings-columns').append(
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
                        $('#table-settings-columns').append(createRow());
                        updateRowActions();
                    })
                )        
        )
    );
    $(tableLayout.fields).each(function(index, column) {
        $('#table-settings-columns').append(function () {
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
        var row = $('<div>').addClass('row fieldConfigurationRow').attr('style', 'margin-top: 5px;');
        $(row).append(
            $('<div>').addClass('col-sm-2').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($('<input>').attr('type', 'text').addClass('form-control form-control-sm').attr('placeholder', 'Name')),
            $('<div>').addClass('col-sm-3').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($('<input>').attr('type', 'text').addClass('form-control form-control-sm').attr('placeholder', 'Field')
                .bind('keydown', function( event ) {
                     if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                         event.stopPropagation();
                     }
                 })
                 .autocompleteFieldQuery(
                		 {
                		 	queryKeywords: queryKeywords, 
                		 	mode: 'field',
                			keywordGroupFilter: function(index, group) {
                				return $('[id^=check-type-]:checked').map(function(){ return $(this).val(); }).get().indexOf(group) == -1;
                		    }
                		 }
                )),
            $('<div>').addClass('col-sm-2').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($('<select>').addClass('form-control form-control-sm custom-select')
                .append($('<option>').attr('value', 'plain').text('Plain'))
                .append($('<option>').attr('value', 'isotimestamp').text('ISO Timestamp'))
                .append($('<option>').attr('value', 'isoutctimestamp').text('ISO UTC Timestamp'))),
            $('<div>').addClass('col-sm-2').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($('<select>').addClass('form-control form-control-sm custom-select')
                .append($('<option>').attr('value', 'lowest').text('Lowest'))
                .append($('<option>').attr('value', 'highest').text('Highest'))
                .append($('<option>').attr('value', 'first').text('First'))
                .append($('<option>').attr('value', 'last').text('Last'))),
            $('<div>').addClass('col-sm-1').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append($('<input>').attr('type', 'checkbox').addClass('form-control form-control-sm'))
        );
        var actionDiv = $('<div>').addClass('col-sm-2').append(
            $('<div>').addClass('row actionRow').append(
                $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-up').click(function (event) {event.preventDefault(); moveRowUp(row)})),
                $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-down').click(function (event) {event.preventDefault(); moveRowDown(row)})),
                $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-times text-danger').click(function (event) {event.preventDefault(); removeRow(row)}))
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
            if ($('#table-settings-columns').children().length > 2) {
                if (index == 0) {
                    $(row).find('.fa-arrow-up').hide();
                } else {
                    $(row).find('.fa-arrow-up').show();
                }
                if (index >= $('#table-settings-columns').children().length -2) {
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
});

$('#btn-apply-table-settings').click(function () {
    var changed = false;
    if (tableLayout.sort_field !== $('#table-settings-sort-field').val()) {
        changed = true;
        tableLayout.sort_field = $('#table-settings-sort-field').val();
    }
    if (tableLayout.sort_field !== $('#table-settings-sort-field').val()) {
        changed = true;
        tableLayout.sort_field = $('#table-settings-sort-field').val();
    }
    if ($.isNumeric($('#table-settings-results-per-page').val()) && tableLayout.results_per_page !== parseInt($('#table-settings-results-per-page').val())) {
        changed = true;
        tableLayout.results_per_page = parseInt($('#table-settings-results-per-page').val());
    }
    var fields = [];
    $('#table-settings-columns .fieldConfigurationRow').each(function (rowIndex, row) {
        var rowData = {name: '', field: '', format: 'plain', array: 'first', link: false};
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
    
    if (JSON.stringify(fields) != JSON.stringify(tableLayout.fields)) {
        changed = true;
    }
    tableLayout.fields = fields;
    if (changed) {
    	tableLayout.timestamp = new Date().getTime();
        executeQuery(createQuery(true));    
    }
    $('#modal-table-settings').modal('hide');
});

