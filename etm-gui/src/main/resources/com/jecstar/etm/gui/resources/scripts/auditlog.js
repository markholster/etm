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
//                    var $body = $('#search_result_table > tbody')
//                    $(data.results).each(function (index, searchResult) {
//                        $body.append(function () {
//                            return createResultTableRow(searchResult, data.time_zone);
//                        })
//                    });
//                    if (!data.has_more_results) {
//                        var $body = $('#search_result_table > tfoot').remove();    
//                    }
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
                            			$('<th style="padding: 0.1rem; cursor: pointer;">').text('Hanlding time').attr('data-event-field', 'handling_time').addClass('headerSortDesc'),
                            			$('<th style="padding: 0.1rem; cursor: pointer;">').text('Principal id').attr('data-event-field', 'principal_id')
                            		)
                            	)
                            )
                            .append(function () {
                                if (data.has_more_results) {
                                    return $('<tfoot>')
                                        .append($('<tr>')
                                            .append($('<td>').addClass('text-center').attr('colspan', $(tableLayout.fields).length)
                                                .append($('<a href="#">').attr('id', 'lnk_show_more').text('Show more'))
                                            )
                                        )
                                }
                            })
                            .append(function() {
                                var $body = $('<tbody>')
//                                $(data.results).each(function (index, searchResult) {
//                                    $body.append(function () {
//                                        return createResultTableRow(searchResult, data.time_zone);
//                                    })
//                                });
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