var clipboards = [];

function buildAuditLogPage() {
	var currentQuery = {
	    results_per_page: 50,
	    sort_field: 'handling_time',
	    sort_order: 'desc',
	    timestamp: null,
	    current_ix: 0,
	    current_query: null,
	};
	$.each(clipboards, function(index, clipboard) {
		clipboard.destroy();
	}); 
	clipboards = [];
	var queryInProgress = false;
	
	$.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/audit/keywords/etm_audit_all',
        cache: false,
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
   
   $('#result_card').on('click', '#search_result_table tbody a', function(event) {
	   event.preventDefault();
       showEvent($(window).scrollTop(), $(this).attr('data-event-index'), $(this).attr('id'), $(this).parent().parent().parent().attr('data-user-timezone'));
       $('#search_result_table > tbody > tr.event-selected').removeClass('event-selected');
  	   $(event.target).parent().parent().addClass('event-selected');
   });
   
   $('#event-container').on('click', '#btn-back-to-results, #link-back-to-results', function(event) {
		event.preventDefault();
		$('#event-container').hide();
		$('#search-container').show();
		$('html,body').animate({scrollTop: Number($(this).attr('data-scroll-to'))},'fast');
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
            cache: false,
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
                    $('#result_card').empty();
                    if (data.hits === 0) {
                        $('#search-stats').text(':  No audit logs found in ' + data.query_time_as_string + 'ms.');
                        $('#result_card').append($('<p>').text('No results found'));
                    } else {
                        $('#search-stats').text(':  Found ' + data.hits_as_string + ' audit logs in ' + data.query_time_as_string + 'ms. Showing audit logs ' + (data.start_ix + 1) + ' to ' + (data.end_ix + 1) + '.');
                        var resultTable = $('<table id="search_result_table">');
                        $(resultTable)
                            .addClass('table table-hover table-sm')
                            .append(
                            	$('<thead>').append(
                            		$('<tr>').append(
                            			$('<th style="padding: 0.1rem; cursor: pointer;">').text('Hanlding time').attr('data-event-field', 'handling_time').addClass(getHeaderClass('handling_time')),
                            			$('<th style="padding: 0.1rem; cursor: pointer;">').text('Type').attr('data-event-field', 'object_type').addClass(getHeaderClass('object_type')),
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
                                var $body = $('<tbody>').attr('data-user-timezone', data.time_zone);
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
	            	.attr('data-event-index', auditLog.index)
	            	.text(moment.tz(auditLog.source.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'))
        	),
        	$('<td style="padding: 0.1rem">').text(auditLog.source.object_type),
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
    
    function showEvent(scrollTo, index, id, timeZone) {
    	$('#search-container').hide();
    	$('#btn-back-to-results, #link-back-to-results').attr('data-scroll-to', scrollTo);
    	$('#event-detail').empty();
    	$.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: '../rest/audit/' + encodeURIComponent(index) + '/' + encodeURIComponent(id),
            cache: false,
            success: function(data) {
                if (!data) {
                    return;
                }
                if ('login' === data.object_type) {
                	showLoginAuditLog(data, timeZone);
                } else if ('logout' === data.object_type) {
                	showLogoutAuditLog(data, timeZone);
                } else if ('getevent' === data.object_type) {
                    showGetEventAuditLog(data, timeZone);
                } else if ('search' === data.object_type) {
                	showSearchAuditLog(data, timeZone);
                }
            }
       });
       $('#event-container').show();
    }
    
    function showLoginAuditLog(data, timeZone) {
    	appendToContainerInRow($('#event-detail'), 'Handling time', moment.tz(data.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
    	appendToContainerInRow($('#event-detail'), 'Type', 'Login');
    	appendToContainerInRow($('#event-detail'), 'Principal id', data.principal_id);
    	appendToContainerInRow($('#event-detail'), 'Successful', data.success ? 'Yes' : 'No');
    }

    function showLogoutAuditLog(data, timeZone) {
    	appendToContainerInRow($('#event-detail'), 'Handling time', moment.tz(data.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
    	appendToContainerInRow($('#event-detail'), 'Type', 'Logout');
    	appendToContainerInRow($('#event-detail'), 'Principal id', data.principal_id);
    	appendToContainerInRow($('#event-detail'), 'Expired', data.expired ? 'Yes' : 'No');
    }

    
    function showGetEventAuditLog(data, timeZone) {
    	appendToContainerInRow($('#event-detail'), 'Handling time', moment.tz(data.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
    	appendToContainerInRow($('#event-detail'), 'Type', 'Get event');
    	appendToContainerInRow($('#event-detail'), 'Principal id', data.principal_id);
    	appendToContainerInRow($('#event-detail'), 'Found', data.found ? 'Yes' : 'No');
    	appendToContainerInRow($('#event-detail'), 'Event id', data.event_id);
    	appendToContainerInRow($('#event-detail'), 'Event name', data.event_name);
        appendToContainerInRow($('#event-detail'), 'Payload visible', data.payload_visible ? 'Yes' : 'No');
        appendToContainerInRow($('#event-detail'), 'Downloaded', data.downloaded ? 'Yes' : 'No');

    	if (data.correlated_events) {
        	$('#event-detail').append($('<br/>'));
	        $correlationTable = $('<table id="correlation-table">').addClass('table table-hover table-sm').append(
	        	$('<caption>').attr('style', 'caption-side: top;').text('Correlated events')
	        ).append(
	        	$('<thead>').append(
	        		$('<tr>').append(
	        			$('<th>').attr('style' ,'padding: 0.1rem;').text('Id'),
		        		$('<th>').attr('style' ,'padding: 0.1rem;').text('Type')
		        	)
		        )
	        ).append(function () {
		        $tbody = $('<tbody>');
		        $.each(data.correlated_events, function(index, event) {
		        	$tbody.append(
		        		$('<tr>').append(
		        			$('<td>').attr('style' ,'padding: 0.1rem;').text(event.event_id),
		        			$('<td>').attr('style' ,'padding: 0.1rem;').text('sql' == event.event_type ? 'SQL' : capitalize(event.event_type))
		        		)
		        	);	
		        });
		        return $tbody;
		    });
		    $('#event-detail').append($correlationTable);
    	}
    	
    	function capitalize(text) {
    		if (!text) {
    			return text;
    		}
    		return text.charAt(0).toUpperCase() + text.toLowerCase().slice(1);
    	}
    }
    
    function showSearchAuditLog(data, timeZone) {
    	appendToContainerInRow($('#event-detail'), 'Handling time', moment.tz(data.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
    	appendToContainerInRow($('#event-detail'), 'Type', 'Search');
    	appendToContainerInRow($('#event-detail'), 'Principal id', data.principal_id);
    	appendToContainerInRow($('#event-detail'), 'Number of results', data.number_of_results);
    	appendToContainerInRow($('#event-detail'), 'Query', data.user_query);
    	appendToContainerInRow($('#event-detail'), 'Query time', data.query_time);
    
    	$('#event-detail').append($('<br/>'));
    	$('#event-detail').append($('<label>').addClass('font-weight-bold form-control-static').text('Executed query on database'));
	    var executedQuery = $('<code>').text(vkbeautify.json(data.executed_query, 4));
	    $clipboard = $('<a>').attr('href', "#").addClass('small').text('Copy raw query to clipboard');
	    $('#event-detail').append(
	    		$('<div>').addClass('row').attr('style', 'background-color: #eceeef;').append(
	    				$('<div>').addClass('col-sm-12').append(
	    						$clipboard,
	    						$('<pre>').attr('style', 'white-space: pre-wrap;').append(
	    								executedQuery
	    						),
	    						$('<pre>').attr('style', 'display: none').text(data.executed_query)
	    						
	    				)
	    		)
	    );
	    clipboards.push(new Clipboard($clipboard[0], {
	        text: function(trigger) {
	            return $(trigger).next().next().text();
	        }
	    }));
	    if (typeof(Worker) !== "undefined") {
	    	var worker = new Worker('../scripts/highlight-worker.js');
	    	worker.onmessage = function(result) { 
	    		executedQuery.html(result.data);
	    	}
	    	worker.postMessage([executedQuery.text(), 'JSON']);
	    }
    }
    
	function appendToContainerInRow(container, name, value) {
		appendElementToContainerInRow(container, name, $('<p>').addClass('form-control-static').text(value));
	}
	
	function appendElementToContainerInRow(container, name, element) {
		var $container = $(container);
		var $row = $container.children(":last-child");
		if ($row.children().length > 0 && $row.children().length <= 2) {
			$row.append(
			    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold form-control-static').text(name)),
			    $('<div>').addClass('col-md-4').attr('style', 'word-wrap: break-word;').append(element)
			);
		} else {
			$container.append(
					$('<div>').addClass('row')
						.append(
						    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold form-control-static').text(name)),
						    $('<div>').addClass('col-md-4').attr('style', 'word-wrap: break-word;').append(element)
					)
			);
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