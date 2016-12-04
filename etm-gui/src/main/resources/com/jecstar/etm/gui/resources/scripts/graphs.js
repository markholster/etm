function buildGraphsPage() {
	var graphMap = {};
	var keywords = [];
	$.when(
		$.ajax({
	        type: 'GET',
	        contentType: 'application/json',
	        url: '../rest/dashboard/keywords/etm_event_all',
	        success: function(data) {
	            if (!data || !data.keywords) {
	                return;
	            }
	            keywords = $.merge(keywords, data.keywords);
	        }
	    }),
	    $.ajax({
	        type: 'GET',
	        contentType: 'application/json',
	        url: '../rest/dashboard/keywords/etm_metrics_all',
	        success: function(data) {
	            if (!data || !data.keywords) {
	                return;
	            }
	            keywords = $.merge(keywords, data.keywords);
	        }
	    })
	).done(function () {
        $('#input-graph-query').bind('keydown', function( event ) {
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

        $('#input-number-field').bind('keydown', function( event ) {
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
        			var agg = $('#sel-number-aggregator').val();
        			if ('average' == agg || 'sum' == agg || 'median' == agg) {
        				return !keyword.number;
        			} else if ('min' == agg || 'max' == agg) {
        				return !keyword.number && !keyword.date;
        			} else {
        				return true;
        			}
        		}
        	}
        );

        
        
	});    
	
	
	$('#sel-graph').change(function(event) {
		event.preventDefault();
		var graphData = graphMap[$(this).val()];
		if ('undefined' == typeof graphData) {
			resetValues();
			return;
		}
		setValuesFromData(graphData);
		enableOrDisableButtons();
	});
	
	$('#sel-graph-type').change(function(event) {
		event.preventDefault();
		if ('bar' == $(this).val()) {
			showBarFields();
		} else if ('line' == $(this).val()) {
			showLineFields();
		} else if ('number' == $(this).val()) {
			showNumberFields();
		} else if ('pie' == $(this).val()) {
			showPieFields();
		}
		
	});
	
	$('#sel-data-source').change(function(event) {
		$('#input-number-field').val('');
	});
	
	$('#sel-number-aggregator').change(function(event) {
		event.preventDefault();
		if ('count' == $(this).val()) {
			$('#input-number-field').parent().parent().hide();
		} else {
			$('#input-number-field').parent().parent().show();
		}
	});
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/dashboard/graphs',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        $graphSelect = $('#sel-graph');
	        $.each(data.graphs, function(index, graph) {
	        	$graphSelect.append($('<option>').attr('value', graph.name).text(graph.name));
	        	graphMap[graph.name] = graph;
	        });
	        sortSelectOptions($graphSelect)
	        $graphSelect.val('');
	    }
	});
	
	$('#btn-confirm-save-graph').click(function(event) {
		if (!document.getElementById('graph_form').checkValidity()) {
			return;
		}
		event.preventDefault();
		var graphName = $('#input-graph-name').val();
		if (isGraphExistent(graphName)) {
			$('#overwrite-graph-name').text(graphName);
			$('#modal-graph-overwrite').modal();
		} else {
			saveGraph();
		}
	});
	
	$('#btn-save-graph').click(function(event) {
		saveGraph();
	});
	
	$('#btn-confirm-remove-graph').click(function(event) {
		event.preventDefault();
		$('#remove-graph-name').text($('#input-graph-name').val());
        $('#modal-graph-remove').modal();
	});	

	$('#btn-remove-graph').click(function(event) {
		removeGraph($('#input-graph-name').val());
	});

	
	$('#input-graph-name').on('input', enableOrDisableButtons);
	
	function enableOrDisableButtons() {
		var graphName = $('#input-graph-name').val();
		if (graphName) {
			$('#btn-confirm-save-graph, #btn-preview-graph').removeAttr('disabled');
			if (isGraphExistent(graphName)) {
				$('#btn-confirm-remove-graph').removeAttr('disabled');
			} else {
				$('#btn-confirm-remove-graph').attr('disabled', 'disabled');
			}
		} else {
			$('#btn-confirm-save-graph, #btn-preview-graph, #btn-confirm-remove-graph').attr('disabled', 'disabled');
		}
	}
	
	function isGraphExistent(name) {
		return "undefined" != typeof graphMap[name];
	}
	
	function saveGraph() {
//		var endpointData = createEndpointData();
//		$.ajax({
//            type: 'PUT',
//            contentType: 'application/json',
//            url: '../rest/settings/endpoint/' + encodeURIComponent(endpointData.name),
//            data: JSON.stringify(endpointData),
//            success: function(data) {
//                if (!data) {
//                    return;
//                }
//        		if (!isEndpointExistent(getEndpointNameById(endpointData.name))) {
//        			$endpointSelect = $('#sel-endpoint');
//        			$endpointSelect.append($('<option>').attr('value', endpointData.name).text(endpointData.name));
//        			sortSelectOptions($endpointSelect);
//        		}
//        		endpointMap[endpointData.name] = endpointData;
//        		$('#endpoints_infoBox').text('Endpoint \'' + getEndpointNameById(endpointData.name) + '\' saved.').show('fast').delay(5000).hide('fast');
//        		enableOrDisableButtons();
//            }
//        }).always(function () {
//        	$('#modal-graph-overwrite').modal('hide');
//        });    		
	}
	
	function removeGraph(graphName) {
//		$.ajax({
//            type: 'DELETE',
//            contentType: 'application/json',
//            url: '../rest/settings/endpoint/' + encodeURIComponent(getEndpointIdByName(endpointName)),
//            success: function(data) {
//                if (!data) {
//                    return;
//                }
//        		delete endpointMap[getEndpointIdByName(endpointName)];
//        		$("#sel-endpoint > option").filter(function(i){
//     		       return $(this).attr("value") == getEndpointIdByName(endpointName);
//        		}).remove();
//        		$('#endpoints_infoBox').text('Endpoint \'' + endpointName + '\' removed.').show('fast').delay(5000).hide('fast');
//        		enableOrDisableButtons();
//            }
//        }).always(function () {
//        	$('#modal-graph-remove').modal('hide');
//        });    		
	}
	
	function resetValues() {
		document.getElementById("graph_form").reset();
		enableOrDisableButtons();
	}
	
	function setValuesFromData(graphData) {
	}
	
	function showBarFields() {
		$("#bar-fields").show();
		$("#line-fields").hide();
		$("#number-fields").hide();
		$("#pie-fields").hide();
	}
	
	function showLineFields() {
		$("#bar-fields").hide();
		$("#line-fields").show();
		$("#number-fields").hide();
		$("#pie-fields").hide();
	}
	
	function showNumberFields() {
		$("#bar-fields").hide();
		$("#line-fields").hide();
		$("#number-fields").show();
		$("#pie-fields").hide();
	}
	
	function showPieFields() {
		$("#bar-fields").hide();
		$("#line-fields").hide();
		$("#number-fields").hide();
		$("#pie-fields").show();
	}
	
	function sortSelectOptions($select) {
		var options = $select.children('option');
		options.detach().sort(function(a,b) {
		    var at = $(a).text();
		    var bt = $(b).text();         
		    return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
		});
		options.appendTo($select);
	}
}