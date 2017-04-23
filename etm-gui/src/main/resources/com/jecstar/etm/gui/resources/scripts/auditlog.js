function buildAuditLogPage() {
	var currentQuery = {
	    results_per_page: 50,
	    sort_field: 'handling_time',
	    sort_order: 'desc',
	    timestamp: null,
	    current_ix: 0,
	    current_query: null,
	};
	var queryInProgress = false;
	
	$.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/audit/keywords/etm_audit_all',
        success: function(data) {
            if (!data || !data.keywords) {
                return;
            }
            $('#input-query-string')
            .on('input', function( event ) {
                if ($(this).val()) {
                	$('#btn-search').removeAttr("disabled");
                } else {
                	$('#btn-search').attr('disabled', 'disabled');
                }
            })
            .bind('keydown', function( event ) {
                if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                    event.stopPropagation();
                } else if ( event.keyCode === $.ui.keyCode.ENTER && !$(this).autocomplete('instance').menu.active && $(this).val()) {
        			startNewQuery();
                }
            })
            .autocompleteFieldQuery(
            	{
            		queryKeywords: data.keywords
            	}
            );
        }
   });

   $('#btn-search').click(function() {
	   startNewQuery();
   });
   
   $('#result_card').on('click', '#search_result_table thead th', function(event) {
	   event.preventDefault();
       sortResultTable($(this).attr('data-event-field'));
   });
   
   $('#result_card').on('click', '#search_result_table tfoot a', function(event) {
       event.preventDefault();
       var query = createQuery(true);
       query.start_ix = currentQuery.current_ix + currentQuery.results_per_page; 
       executeQuery(query, true);
   });
   
   $(window).scroll(function(event) {
	   var $showMoreLink = $('#lnk_show_more');
	   if (isInViewport($showMoreLink)) {
		   $showMoreLink.click();
	   }
   });    
    
   function enableOrDisableSearchButton() {
		if (!$('#input-query-string').val()) {
			$('#btn-search').attr('disabled', 'disabled');
		} else {
			$('#btn-search').removeAttr("disabled");
		}
	}

	function startNewQuery() {
		currentQuery.timestamp = new Date().getTime();
		var queryParams = createQuery(false);
		queryParams.start_ix = 0;
		executeQuery(queryParams);
	}

	function createQuery(reusePreviousQueryString) {
		if (!reusePreviousQueryString) {
			if (!$('#input-query-string').val()) {
				return null;
			}
			currentQuery.current_query = $('#input-query-string').val();
		} else if (!currentQuery.current_query) {
			return null;
		}
		var query = {
			query : currentQuery.current_query,
			start_ix : currentQuery.current_ix,
			max_results: currentQuery.results_per_page,
            sort_field: currentQuery.sort_field,
            sort_order: currentQuery.sort_order,
			timestamp : currentQuery.timestamp
		};
		return query;
	}
	
    function executeQuery(queryParameters, appendToCurrent) {
        if (!queryParameters || queryInProgress) {
            return;
        }
        queryInProgress = true;
        $('#search-result-card').show();
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: '../rest/audit/query',
            data: JSON.stringify(queryParameters),
            success: function(data) {
                if (!data) {
                    return;
                }
                currentQuery.current_ix = data.start_ix;
                if (appendToCurrent) {
                    var endIx = $('#search-stats').text().lastIndexOf(' ');
                    $('#search-stats').text($('#search-stats').text().substring(0, endIx + 1) + (data.end_ix + 1) + '.');
                    var $body = $('#search_result_table > tbody')
                    $(data.results).each(function (index, auditLog) {
                        $body.append(function () {
                            return createResultTableRow(auditLog, data.time_zone);
                        })
                    });
                    if (!data.has_more_results) {
                        var $body = $('#search_result_table > tfoot').remove();    
                    }
                } else {
                    $('#search-stats').text(':  Found ' + data.hits_as_string + ' audit logs in ' + data.query_time_as_string + 'ms. Showing audit logs ' + (data.start_ix + 1) + ' to ' + (data.end_ix + 1) + '.');
                    $('#result_card').empty();
                    if (data.hits === 0) {
                       $('#result_card').append($('<p>').text('No results found'));
                    } else {
                        var resultTable = $('<table id="search_result_table">');
                        $(resultTable)
                            .addClass('table table-hover table-sm')
                            .append(
                            	$('<thead>').append(
                            		$('<tr>').append(
                            			$('<th style="padding: 0.1rem; cursor: pointer;">').text('Hanlding time').attr('data-event-field', 'handling_time').addClass(getHeaderClass('handling_time')),
                            			$('<th style="padding: 0.1rem; cursor: pointer;">').text('Type').attr('data-event-field', '_type').addClass(getHeaderClass('_type')),
                            			$('<th style="padding: 0.1rem; cursor: pointer;">').text('Principal id').attr('data-event-field', 'principal_id').addClass(getHeaderClass('principal_id'))
                            		)
                            	)
                            )
                            .append(function () {
                                if (data.has_more_results) {
                                    return $('<tfoot>')
                                        .append($('<tr>')
                                            .append($('<td>').addClass('text-center').attr('colspan', '3')
                                                .append($('<a href="#">').attr('id', 'lnk_show_more').text('Show more'))
                                            )
                                        )
                                }
                            })
                            .append(function() {
                                var $body = $('<tbody>')
                                $(data.results).each(function (index, auditLog) {
                                    $body.append(function () {
                                        return createResultTableRow(auditLog, data.time_zone);
                                    })
                                });
                                return $body;
                            });
                        $('#result_card').append($('<div>').addClass('table-responsive').append($(resultTable)));
                        if (!isInViewport($('#result_card'))) {
                            $('html,body').animate({scrollTop: $('#query-string').offset().top},'slow');
                        }
                    }
                }                
            },
            complete: function () {
            	queryInProgress = false;
            }
        });    
    }
    
    function createResultTableRow(auditLog, timeZone) {
        var $tr = $('<tr>');
        
        $tr.append(
        	$('<td style="padding: 0.1rem">').append(
	            $('<a>')
	            	.attr('href', '#')
	            	.attr('id', auditLog.id)
	            	.attr('data-event-type', auditLog.type)
	            	.text(moment.tz(auditLog.source.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'))
        	),
        	$('<td style="padding: 0.1rem">').text(auditLog.type),
        	$('<td style="padding: 0.1rem">').text(auditLog.source.principal_id)
        );
        return $tr;
    }

    function sortResultTable(fieldName) {
    	if (queryInProgress) {
    		return false;
    	}
        if (currentQuery.sort_field === fieldName) {
            if (currentQuery.sort_order === 'asc') {
            	currentQuery.sort_order = 'desc';
            } else {
            	currentQuery.sort_order = 'asc';
            }
        } else {
        	currentQuery.sort_field = fieldName;
        }
        currentQuery.current_ix = 0;
        executeQuery(createQuery(true));
    }
    
    function getHeaderClass(fieldName) {
    	if (currentQuery.sort_field !== fieldName) {
    		return '';
    	}
        if (currentQuery.sort_order === 'asc') {
        	return 'headerSortAsc';
        } else {
        	return 'headerSortDesc';
        }
    }
    
    function isInViewport(elem) {
    	if (!$(elem).is(':visible')) {
    		return false;
    	}
        var docViewTop = $(window).scrollTop();
        var docViewBottom = docViewTop + $(window).height();
        var elemTop = $(elem).offset().top;
        var elemBottom = elemTop + $(elem).height();
        return docViewBottom > elemTop;
    }

}