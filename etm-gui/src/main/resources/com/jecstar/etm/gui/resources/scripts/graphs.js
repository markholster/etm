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
        
        addMetricsAggregatorsBlock($('#bar-y-axes'),'bar', 0);
        addBucketAggregatorsBlock($('#bar-x-axes'),'bar', 0)
        addMetricsAggregatorsBlock($('#number-fields'),'number', 0);
        
    	$('#graph_form :input').on('input', enableOrDisableButtons);
    	$('#graph_form :input').on('autocomplete:selected', enableOrDisableButtons);
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
	
	
	function addMetricsAggregatorsBlock(parent, graphType, ix) {
		$(parent).append(
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'sel-' + graphType + '-metric-aggregator-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Aggregator'),
				$('<div>').addClass('col-sm-9').append(
					$('<select>').attr('id', 'sel-' + graphType + '-metric-aggregator-' + ix).addClass('form-control form-control-sm custom-select')
					.change(function(event) {
						event.preventDefault();
						if ('count' == $(this).val()) {
							$('#input-' + graphType + '-metric-field-' + ix).removeAttr('required').parent().parent().hide();
						} else {
							$('#input-' + graphType + '-metric-field-' + ix).attr('required', 'required').parent().parent().show();
						}
						if ('percentile' == $(this).val()) {
							$('#input-' + graphType + '-metric-percentile-' + ix).attr('required', 'required').parent().parent().show();
						} else {
							$('#input-' + graphType + '-metric-percentile-' + ix).removeAttr('required').parent().parent().hide();
						}
						if ('percentile_rank' == $(this).val()) {
							$('#input-' + graphType + '-metric-percentile-rank-' + ix).attr('required', 'required').parent().parent().show();
						} else {
							$('#input-' + graphType + '-metric-percentile-rank-' + ix).removeAttr('required').parent().parent().hide();
						}
						enableOrDisableButtons();
					})
					.append(
						$('<option>').attr('value', 'average').text('Average'),
						$('<option>').attr('value', 'count').text('Count'),
						$('<option>').attr('value', 'max').text('Max'),
						$('<option>').attr('value', 'median').text('Median'),
						$('<option>').attr('value', 'min').text('Min'),
						$('<option>').attr('value', 'percentile').text('Percentile'),
						$('<option>').attr('value', 'percentile_rank').text('Percentile rank'),
						$('<option>').attr('value', 'sum').text('Sum'),
						$('<option>').attr('value', 'cardinality').text('Unique count')
					)
				)
			),
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'input-' + graphType + '-metric-field-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Field'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-metric-field-' + ix).attr('type', 'text').addClass('form-control form-control-sm')
					.bind('keydown', function( event ) {
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
			        			var agg = $('#sel-' + graphType + '-metric-aggregator-' + ix).val();
			        			if ('average' == agg || 'sum' == agg || 'median' == agg || 'percentile' == agg || 'percentile_rank' == agg) {
			        				return !keyword.number;
			        			} else if ('min' == agg || 'max' == agg) {
			        				return !keyword.number && !keyword.date;
			        			} else if ('cardinality' == agg) {
			        				return !keyword.number && !keyword.date && 'keyword' != keyword.type;
			        			} else {
			        				return true;
			        			}
			        		}
			        	}
			        )
				)
			),
			$('<div>').addClass('form-group row').attr('style', 'display: none;').append(
				$('<label>').attr('for', 'input-' + graphType + '-metric-percentile-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Percentile'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-metric-percentile-' + ix).attr('type', 'number').addClass('form-control form-control-sm').attr('step', 'any').attr('min', '0').attr('max', '100').attr('value', '95')
				)
			),
			$('<div>').addClass('form-group row').attr('style', 'display: none;').append(
				$('<label>').attr('for', 'input-' + graphType + '-metric-percentile-rank-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Rank'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-metric-percentile-rank-' + ix).attr('type', 'number').addClass('form-control form-control-sm').attr('step', 'any')
				)
			),
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'input-' + graphType + '-metric-label-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Label'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-metric-label-' + ix).attr('type', 'text').addClass('form-control form-control-sm')
				)
			)
		);
	}
	
	function getMetricsAggregatorsBlock(graphType, ix) {
		var metricData = {
			aggregator: $('#sel-' + graphType + '-metric-aggregator-' + ix).val(),
			label: $('#input-' + graphType + '-metric-label-' + ix).val() ? $('#input-' + graphType + '-metric-label-' + ix).val() : null
		};
		if ('count' != metricData.aggregator) {
			metricData.field = $('#input-' + graphType + '-metric-field-' + ix).val(),
			metricData.field_type = findFieldType($('#sel-data-source').val(), metricData.field);
		}
		if ('percentile' == metricData.aggregator) {
			metricData.percentile_data = Number($('#input-' + graphType + '-metric-percentile-' + ix).val());
		} else if ('percentile_rank' == metricData.aggregator) {
			metricData.percentile_data = Number($('#input-' + graphType + '-metric-percentile-rank-' + ix).val());
		}
		return metricData;
	}

	
	function addBucketAggregatorsBlock(parent, graphType, ix) {
		$(parent).append(
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'sel-' + graphType + '-bucket-aggregator-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Aggregator'),
				$('<div>').addClass('col-sm-9').append(
					$('<select>').attr('id', 'sel-' + graphType + '-bucket_aggregator-' + ix).addClass('form-control form-control-sm custom-select')
					.change(function(event) {
						event.preventDefault();
						if ('filter' == $(this).val()) {
							$('#input-' + graphType + '-bucket_field-' + ix).removeAttr('required').parent().parent().hide();
						} else {
							$('#input-' + graphType + '-bucket_field-' + ix).attr('required', 'required').parent().parent().show();
						}						
						enableOrDisableButtons();
					})
					.append(
						$('<option>').attr('value', 'date_histogram').text('Date histogram'),
						$('<option>').attr('value', 'date_range').text('Date range'),
						$('<option>').attr('value', 'filter').text('Filter'),
						$('<option>').attr('value', 'histogram').text('Histogram'),
						$('<option>').attr('value', 'range').text('Range'),
						$('<option>').attr('value', 'significant_term').text('Significant term'),
						$('<option>').attr('value', 'term').text('Term')
					)
				)
			),
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'input-' + graphType + '-bucket_field-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Field'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-bucket_field-' + ix).attr('type', 'text').addClass('form-control form-control-sm')
					.bind('keydown', function( event ) {
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
			        			var agg = $('#sel-' + graphType + '-bucket_aggregator-' + ix).val();
			        			if ('date_histogram' == agg || 'date_range' == agg) {
			        				return !keyword.date;
			        			} else if ('histogram' == agg || 'range' == agg) {
			        				return !keyword.number;
			        			} else if ('significant_term' == agg) {
				        			return 'keyword' != keyword.type;
			        			} else if ('term' == agg) {
			        				return !keyword.number && !keyword.date && 'keyword' != keyword.type;
			        			} else {
			        				return true;
			        			}
			        		}
			        	}
			        )
				)
			),
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'input-' + graphType + '-bucket_label-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Label'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-bucket_label-' + ix).attr('type', 'text').addClass('form-control form-control-sm')
				)
			)			
		)		
	}
	
	function getBucketAggregatorsBlock(graphType, ix) {
		var xAxisData = {
			aggregator: $('#sel-' + graphType + '-bucket_aggregator-' + ix).val(),
			label: $('#input-' + graphType + '-bucket_label-' + ix).val() ? $('#input-' + graphType + '-bucket_label-' + ix).val() : null
		};
		if ('filter' != xAxisData.aggregator) {
			xAxisData.field = $('#input-' + graphType + '-bucket_field-' + ix).val(),
			xAxisData.field_type = findFieldType($('#sel-data-source').val(), xAxisData.field);
		}
		return xAxisData;		
	}
	
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
        			formatter = d3.locale(data.d3_formatter);
        			numberFormatter = formatter.numberFormat(',f');
        		    nv.addGraph(function() {
        		        var chart = nv.models.multiBarChart()
        		            .x(function(d) { return d.label })
        		            .y(function(d) { return d.value })
        		            .reduceXTicks(true)
        		            .rotateLabels(0)
        		            .showControls(data.data.length > 1)
        		            .groupSpacing(0.1) 
        		            .duration(250)
        		            ;
        		        chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
//        		        chart.xAxis.tickFormat(d3.format(',f'));
        		        d3.select('#preview_box').append("svg").attr("style", "height: 20em;")
        		        	.datum(data.data)
        		        	.call(chart);
        		        	nv.utils.windowResize(chart.update);
        		        return chart;
        		    });
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
	
	function createGraphData() {
		var graphData = {
			name: $('#input-graph-name').val() ? $('#input-graph-name').val() : null,
			type: $('#sel-graph-type').val(),
			data_source: $('#sel-data-source').val(),
			query: $('#input-graph-query').val() ? $('#input-graph-query').val() : null
		};
		if ('bar' == graphData.type) {
			graphData.bar = {
				y_axis: {
					aggregators: []
				},
				x_axis: {
					aggregator: getBucketAggregatorsBlock(graphData.type, 0)
				}
			}
			graphData.bar.y_axis.aggregators.push(getMetricsAggregatorsBlock(graphData.type, 0));
		} else if ('line' == graphData.type) {
			
		} else if ('number' == graphData.type) {
			graphData.number = getMetricsAggregatorsBlock(graphData.type, 0);
		} else if ('pie' == graphData.type) {
			
		}
		return graphData;
	}
	
	function showBarFields() {
		hideLineFields();
		hideNumberFields();
		hidePieFields();
		$('[id^=input-bar-field-]').attr('required', 'required');
		$('[id^=input-bar-percentile-] :visible').attr('required', 'required');
		$('#bar-fields').show();
	}
	
	function hideBarFields() {
		$('[id^=input-bar-field-]').removeAttr('required');
		$('[id^=input-bar-percentile-] :visible').removeAttr('required');
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
		$('[id^=input-number-field-]').attr('required', 'required');
		$('[id^=input-number-percentile-] :visible').attr('required', 'required');
		$('#number-fields').show();
	}
	
	function hideNumberFields() {
		$('[id^=input-number-field-]').removeAttr('required');
		$('[id^=input-number-percentile-] :visible').removeAttr('required');
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