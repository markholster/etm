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
        $('#result_block').show();
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: '../rest/audit/query',
            data: JSON.stringify(queryParameters),
            success: function(data) {
                if (!data) {
                    return;
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