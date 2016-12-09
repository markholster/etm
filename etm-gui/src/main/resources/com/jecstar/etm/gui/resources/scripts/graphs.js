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
        			if ('average' == agg || 'sum' == agg || 'median' == agg || 'percentile' == agg || 'percentile_rank' == agg) {
        				return !keyword.number;
        			} else if ('min' == agg || 'max' == agg) {
        				return !keyword.number && !keyword.date;
        			} else if ('cardinality' == agg) {
        				return false;
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
		enableOrDisableButtons();
	});
	
	$('#sel-data-source').change(function(event) {
		$('#input-number-field').val('');
	});
	
	$('#sel-number-aggregator').change(function(event) {
		event.preventDefault();
		if ('count' == $(this).val()) {
			$('#input-number-field').removeAttr('required').parent().parent().hide();
		} else {
			$('#input-number-field').attr('required', 'required').parent().parent().show();
		}
		if ('percentile' == $(this).val()) {
			$('#input-number-percentile').attr('required', 'required').parent().parent().show();
		} else {
			$('#input-number-percentile').removeAttr('required').parent().parent().hide();
		}
		if ('percentile_rank' == $(this).val()) {
			$('#input-number-percentile-rank').attr('required', 'required').parent().parent().show();
		} else {
			$('#input-number-percentile-rank').removeAttr('required').parent().parent().hide();
		}
		enableOrDisableButtons();
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
	
	$('#btn-preview-graph').click(function(event) {
		event.preventDefault();
		preview();
	});

	$('#btn-remove-graph').click(function(event) {
		removeGraph($('#input-graph-name').val());
	});
	
	$('#graph_form :input').on('input', enableOrDisableButtons);
	$('#graph_form :input').on('autocomplete:selected', enableOrDisableButtons);
	
	function enableOrDisableButtons() {
		//  First remove the required constraints to check if all other fields are valid.
		$('#input-graph-name').removeAttr('required');
		if (document.getElementById('graph_form').checkValidity()) {
			// All input fields seem valid so we can generate a preview.
			$('#btn-preview-graph').removeAttr('disabled');
		} else {
			$('#btn-preview-graph').attr('disabled', 'disabled');
		}
		// Now reset the required constraints and validate the form again.
		$('#input-graph-name').attr('required', 'required');
		if (document.getElementById('graph_form').checkValidity()) {
			$('#btn-confirm-save-graph').removeAttr('disabled');
		} else {
			$('#btn-confirm-save-graph').attr('disabled', 'disabled');
		}
		var graphName = $('#input-graph-name').val();
		if (graphName && isGraphExistent(graphName)) {
			$('#btn-confirm-remove-graph').removeAttr('disabled');
		} else {
			$('#btn-confirm-remove-graph').attr('disabled', 'disabled');
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
	
	function preview() {
		var graphData = createGraphData();
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: '../rest/dashboard/graphdata',
            data: JSON.stringify(graphData),
            success: function(data) {
                if (!data) {
                    return;
                }
                $('#preview_box').empty();
        		if ('bar' == data.type) {
        		} else if ('line' == data.type) {
        		} else if ('number' == data.type) {
        			$('#preview_box').append($('<h1>').text(data.value_as_string),$('<h4>').text(data.label));
        		} else if ('pie' == data.type) {
        		}
            }
        });
	}
	
	function resetValues() {
		document.getElementById('graph_form').reset();
		enableOrDisableButtons();
	}
	
	function setValuesFromData(graphData) {
	}
	
	function createGraphData() {
		
		function findFieldType(indexName, fieldName) {
			var fieldType = null;
			$.each(keywords, function(index, keywordGroup) {
				if (indexName == keywordGroup.index) {
					var result = $.grep(keywordGroup.keywords, function(e){ return e.name == fieldName; });
					if (result.length >= 1) {
						fieldType = result[0].type;
						return true;
					}
				}
			});
			return fieldType;
		}
		
		var graphData = {
			name: $('#input-graph-name').val() ? $('#input-graph-name').val() : null,
			type: $('#sel-graph-type').val(),
			data_source: $('#sel-data-source').val(),
			query: $('#input-graph-query').val() ? $('#input-graph-query').val() : null
		};
		if ('bar' == graphData.type) {

		} else if ('line' == graphData.type) {
			
		} else if ('number' == graphData.type) {
			graphData.number = {
				aggregator: $('#sel-number-aggregator').val(),
				label: $('#input-number-label').val() ? $('#input-number-label').val() : null
			};
			if ('count' != graphData.number.aggregator) {
				graphData.number.field = $('#input-number-field').val(),
				graphData.number.field_type = findFieldType(graphData.data_source, graphData.number.field);
			}
			if ('percentile' == graphData.number.aggregator) {
				graphData.number.percentile_data = Number($('#input-number-percentile').val());
			} else if ('percentile_rank' == graphData.number.aggregator) {
				graphData.number.percentile_data = Number($('#input-number-percentile-rank').val());
			}
			
		} else if ('pie' == graphData.type) {
			
		}
		return graphData;
	}
	
	function showBarFields() {
		hideLineFields();
		hideNumberFields();
		hidePieFields();
		$('#bar-fields').show();
	}
	
	function hideBarFields() {
		$('#bar-fields').hide();
	}
	
	function showLineFields() {
		hideBarFields();
		hideNumberFields();
		hidePieFields();
		$('#line-fields').show();
	}
	
	function hideLineFields() {
		$('#line-fields').show();
	}
	
	function showNumberFields() {
		hideBarFields();
		hideLineFields();
		hidePieFields();
		$('#input-number-field').attr('required', 'required');
		$('#number-fields').show();
	}
	
	function hideNumberFields() {
		$('#input-number-field').removeAttr('required');
		$('#number-fields').hide();
	}
	
	function showPieFields() {
		hideBarFields();
		hideLineFields();
		hideNumberFields();
		$('#pie-fields').show();
	}
	
	function hidePieFields() {
		$('#pie-fields').hide();
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