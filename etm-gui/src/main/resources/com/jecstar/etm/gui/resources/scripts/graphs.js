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
        
        addMetricsAggregatorsBlock($('#bar-y-axis'),'bar', 0);
        addBucketAggregatorsBlock($('#bar-x-axis'),'bar', 0);
        addMetricsAggregatorsBlock($('#line-y-axis'),'line', 0);
        addBucketAggregatorsBlock($('#line-x-axis'),'line', 0);
        addMetricsAggregatorsBlock($('#number-fields'),'number', 0);
        addMetricsAggregatorsBlock($('#stacked_area-y-axis'),'stacked_area', 0);
        addBucketAggregatorsBlock($('#stacked_area-x-axis'),'stacked_area', 0);
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
		showFieldGroup($(this).val());
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
	
	// Check button state on every input
	$('#graph_form').on('input autocomplete:selected', ':input', enableOrDisableButtons);
	
	// Handle the addition of a metrics aggregator on y-axis.
	$('#graph_form').on('click', "a[data-element-type='add-y-axis-metrics-aggregator']", function(event) {
		event.preventDefault();
		var graphType = $(this).attr('data-graph-type');
		var highestIx = 0;
		$('[id^=sel-' + graphType + '-metrics-aggregator-]').each(function(index) {
			var currentIx = Number($(this).attr('id').split('-')[4]);
			if (currentIx > highestIx) {
				highestIx = currentIx;
			}
		})
		addMetricsAggregatorsBlock($('#' + graphType + '-y-axis'), graphType, highestIx + 1);
		updateBucketTermAggregatorsOrderBySelector(graphType);
	});
	
	// Handle the name change of an metric label name on the y-axis
	$('#graph_form').on('input', "input[data-element-type='metric-aggregator-label']", function(event) {
		event.preventDefault();
		var graphType = $(this).attr('data-graph-type');
		updateBucketTermAggregatorsOrderBySelector(graphType);
	});
	
	// Handle the addition of a sub aggregator on the x-axis
	$('#graph_form').on('click', "a[data-element-type='add-x-axis-bucket-aggregator']", function(event) {
		event.preventDefault();
		var graphType = $(this).attr('data-graph-type');
		addBucketAggregatorsBlock($('#' + graphType + '-x-axis'), graphType, 1);
		$(this).hide();
	});
	
	// Add the metric aggregator change handling.
	$('#graph_form').on('change', "select[data-element-type='metrics-aggregator-selector']", function(event) {
		event.preventDefault();
		var graphType = $(this).attr('data-graph-type');
		var ix = $(this).attr('data-aggregator-index');
		if ('count' == $(this).val()) {
			$('#input-' + graphType + '-metrics-field-' + ix).parent().parent().hide();
		} else {
			$('#input-' + graphType + '-metrics-field-' + ix).parent().parent().show();
		}
		if ('percentile' == $(this).val()) {
			$('#input-' + graphType + '-metrics-percentile-' + ix).parent().parent().show();
		} else {
			$('#input-' + graphType + '-metrics-percentile-' + ix).parent().parent().hide();
		}
		if ('percentile_rank' == $(this).val()) {
			$('#input-' + graphType + '-metrics-percentile-rank-' + ix).parent().parent().show();
		} else {
			$('#input-' + graphType + '-metrics-percentile-rank-' + ix).parent().parent().hide();
		}
		updateBucketTermAggregatorsOrderBySelector(graphType);
		enableOrDisableButtons();
	});
	
	// Add the bucket aggregator change handling.
	$('#graph_form').on('change', "select[data-element-type='bucket-aggregator-selector']", function(event) {
		event.preventDefault();
		var graphType = $(this).attr('data-graph-type');
		var ix = $(this).attr('data-aggregator-index');
		if ('filter' == $(this).val()) {
			$('#input-' + graphType + '-bucket-field-' + ix).parent().parent().hide();
		} else {
			$('#input-' + graphType + '-bucket-field-' + ix).parent().parent().show();
		}			
		if ('date_histogram' == $(this).val()) {
			$('#sel-' + graphType + '-bucket-date-interval-' + ix).parent().parent().show();
		} else {
			$('#sel-' + graphType + '-bucket-date-interval-' + ix).parent().parent().hide();
		}
		if ('histogram' == $(this).val()) {
			$('#input-' + graphType + '-bucket-histogram-interval-' + ix).parent().parent().show();
		} else {
			$('#input-' + graphType + '-bucket-histogram-interval-' + ix).parent().parent().hide();
		}
		if ('significant_term' == $(this).val()) {
			$('#sel-' + graphType + '-bucket-significant-term-top-' + ix).parent().parent().show();
		} else {
			$('#sel-' + graphType + '-bucket-significant-term-top-' + ix).parent().parent().hide();
		}
		if ('term' == $(this).val()) {
			$('#sel-' + graphType + '-bucket-term-order-by-' + ix).parent().parent().show();
			$('#sel-' + graphType + '-bucket-term-order-' + ix).parent().parent().show();
			$('#sel-' + graphType + '-bucket-term-top-' + ix).parent().parent().show();
		} else {
			$('#sel-' + graphType + '-bucket-term-order-by-' + ix).parent().parent().hide();
			$('#sel-' + graphType + '-bucket-term-order-' + ix).parent().parent().hide();
			$('#sel-' + graphType + '-bucket-term-top-' + ix).parent().parent().hide();
		}
		enableOrDisableButtons();
	});
	
	// Add the autocomplete handling.
	$('#graph_form').on('keydown', "input[data-element-type='autocomplete-input']", function(event) {
        if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
            event.stopPropagation();
        }
	});
	
	// Add the remove metrics aggregator handling.
	$('#graph_form').on('click', "a[data-element-type='remove-metrics-aggregator']", function(event) {
		event.preventDefault();
		// Remove till the <hr>
		$(this).prevUntil('hr').remove();
		// remove the <hr>
		$(this).prev().remove();
		// remove the <br>
		$(this).next().remove();
		// Remove the link itself.
		$(this).remove();
		// Remove the option form the term order by select.
		var graphType = $(this).attr('data-graph-type');
		updateBucketTermAggregatorsOrderBySelector(graphType);
		enableOrDisableButtons();
	});
	
	// Add the remove bucket aggregator handling.
	$('#graph_form').on('click', "a[data-element-type='remove-bucket-aggregator']", function(event) {
		event.preventDefault();
		// Remove till the <hr>
		$(this).prevUntil('hr').remove();
		// remove the <hr>
		$(this).prev().remove();
		// remove the <br>
		$(this).next().remove();
		// Enable the add sub aggregator link
		$(this).parent().prev().children('a[data-element-type="add-x-axis-bucket-aggregator"]').show();
		// Remove the link itself.
		$(this).remove();
		enableOrDisableButtons();
	});

	function addMetricsAggregatorsBlock(parent, graphType, ix, aggregatorData) {
		if (ix > 0)  {
			$(parent).append(
				$('<hr>').attr('style', 'border-top: dashed 1px;')
			);
		}
		$(parent).append(
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'sel-' + graphType + '-metrics-aggregator-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Aggregator'),
				$('<div>').addClass('col-sm-9').append(
					$('<select>').attr('id', 'sel-' + graphType + '-metrics-aggregator-' + ix)
					.attr('data-element-type', 'metrics-aggregator-selector')
					.attr('data-graph-type', graphType)
					.attr('data-aggregator-index', ix)
					.addClass('form-control form-control-sm custom-select')
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
				$('<label>').attr('for', 'input-' + graphType + '-metrics-field-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Field'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-metrics-field-' + ix).attr('type', 'text')
					.attr('data-required', 'required')
					.attr('data-element-type', 'autocomplete-input')
					.addClass('form-control form-control-sm')
			        .autocompleteFieldQuery(
			        	{
			        		queryKeywords: keywords,
			        		mode: 'field',
			        		keywordIndexFilter: function(index) {
			        			return index != $('#sel-data-source').val();
			        		},
			        		keywordFilter: function(index, group, keyword) {
			        			var agg = $('#sel-' + graphType + '-metrics-aggregator-' + ix).val();
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
				$('<label>').attr('for', 'input-' + graphType + '-metrics-percentile-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Percentile'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-metrics-percentile-' + ix).attr('type', 'number').attr('data-required', 'required').addClass('form-control form-control-sm').attr('step', 'any').attr('min', '0').attr('max', '100').attr('value', '95')
				)
			),
			$('<div>').addClass('form-group row').attr('style', 'display: none;').append(
				$('<label>').attr('for', 'input-' + graphType + '-metrics-percentile-rank-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Rank'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-metrics-percentile-rank-' + ix).attr('type', 'number').attr('data-required', 'required').addClass('form-control form-control-sm').attr('step', 'any')
				)
			),
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'input-' + graphType + '-metrics-label-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Label'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-metrics-label-' + ix)
					.attr('type', 'text')
					.attr('data-graph-type', graphType)
					.attr('data-element-type', 'metric-aggregator-label')
					.addClass('form-control form-control-sm')
				)
			)
		);
		if (ix > 0)  {
			$(parent).append(
				$('<a>').attr('href', '#').addClass('pull-right')
					.attr('data-element-type', 'remove-metrics-aggregator')
					.attr('data-graph-type', graphType)
					.text('Remove aggregator'),
				$('<br>')
			);
		}
		if (aggregatorData) {
			$('#sel-' + graphType + '-metrics-aggregator-' + ix).val(aggregatorData.aggregator).trigger('change');
			if (aggregatorData.label) {
				$('#input-' + graphType + '-metrics-label-' + ix).val(aggregatorData.label);
			}
			if ('count' != aggregatorData.aggregator) {
				$('#input-' + graphType + '-metrics-field-' + ix).val(aggregatorData.field);
			} 
			if ('percentile' == aggregatorData.aggregator) {
				$('#input-' + graphType + '-metrics-percentile-' + ix).val(aggregatorData.percentile_data);
			} else if ('percentile_rank' == aggregatorData.aggregator) {
				$('#input-' + graphType + '-metrics-percentile-rank-' + ix).val(aggregatorData.percentile_data);
			}
		}
		enableOrDisableButtons();
	}
	
	function getMetricsAggregatorsBlock(graphType, ix) {
		var aggregatorData = {
			id: 'metric_' + ix,
			aggregator: $('#sel-' + graphType + '-metrics-aggregator-' + ix).val(),
			label: $('#input-' + graphType + '-metrics-label-' + ix).val() ? $('#input-' + graphType + '-metrics-label-' + ix).val() : null
		};
		if ('count' != aggregatorData.aggregator) {
			aggregatorData.field = $('#input-' + graphType + '-metrics-field-' + ix).val(),
			aggregatorData.field_type = findFieldType($('#sel-data-source').val(), aggregatorData.field);
		}
		if ('percentile' == aggregatorData.aggregator) {
			aggregatorData.percentile_data = Number($('#input-' + graphType + '-metrics-percentile-' + ix).val());
		} else if ('percentile_rank' == aggregatorData.aggregator) {
			aggregatorData.percentile_data = Number($('#input-' + graphType + '-metrics-percentile-rank-' + ix).val());
		}
		return aggregatorData;
	}

	
	function addBucketAggregatorsBlock(parent, graphType, ix, aggregatorData) {
		if (ix > 0)  {
			$(parent).append(
				$('<hr>').attr('style', 'border-top: dashed 1px;')
			);
		}
		$(parent).append(
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'sel-' + graphType + '-bucket-aggregator-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Aggregator'),
				$('<div>').addClass('col-sm-9').append(
					$('<select>').attr('id', 'sel-' + graphType + '-bucket-aggregator-' + ix)
					.addClass('form-control form-control-sm custom-select')
					.attr('data-element-type', 'bucket-aggregator-selector')
					.attr('data-graph-type', graphType)
					.attr('data-aggregator-index', ix)
					.append(
						$('<option>').attr('value', 'date_histogram').text('Date histogram'),
						$('<option>').attr('value', 'histogram').text('Histogram'),
						$('<option>').attr('value', 'significant_term').text('Significant term'),
						$('<option>').attr('value', 'term').text('Term')
					)
				)
			),
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'input-' + graphType + '-bucket-field-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Field'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-bucket-field-' + ix)
					.attr('type', 'text')
					.attr('data-required', 'required')
					.attr('data-element-type', 'autocomplete-input')
					.addClass('form-control form-control-sm')
					.autocompleteFieldQuery(
			        	{
			        		queryKeywords: keywords,
			        		mode: 'field',
			        		keywordIndexFilter: function(index) {
			        			return index != $('#sel-data-source').val();
			        		},
			        		keywordFilter: function(index, group, keyword) {
			        			var agg = $('#sel-' + graphType + '-bucket-aggregator-' + ix).val();
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
			// The date interval field used for the Date histogram aggregator
			$('<div>').addClass('form-group row').append(
				$('<label>').attr('for', 'sel-' + graphType + '-bucket-date-interval-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Interval'),
				$('<div>').addClass('col-sm-9').append(
					$('<select>').attr('id', 'sel-' + graphType + '-bucket-date-interval-' + ix).addClass('form-control form-control-sm custom-select')
					.append(
						$('<option>').attr('value', 'seconds').text('Seconds'),
						$('<option>').attr('value', 'minutes').text('Minutes'),
						$('<option>').attr('value', 'hours').text('Hours'),
						$('<option>').attr('value', 'days').attr('selected', 'selected').text('Days'),
						$('<option>').attr('value', 'weeks').text('Weeks'),
						$('<option>').attr('value', 'months').text('Months'),
						$('<option>').attr('value', 'quarters').text('Quarters'),
						$('<option>').attr('value', 'years').text('Years')
					)
				)
			),
			// Histogram fields
			$('<div>').addClass('form-group row').attr('style', 'display: none;').append(
				$('<label>').attr('for', 'input-' + graphType + '-bucket-histogram-interval-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Interval'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'input-' + graphType + '-bucket-histogram-interval-' + ix)
					.attr('type', 'number')
					.attr('data-required', 'required')
					.addClass('form-control form-control-sm')
					.attr('step', 'any').attr('min', '1')
				)
			),
			// Significant term fields
			$('<div>').addClass('form-group row').attr('style', 'display: none;').append(
				$('<label>').attr('for', 'sel-' + graphType + '-bucket-significant-term-top-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Top'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'sel-' + graphType + '-bucket-significant-term-top-' + ix).addClass('form-control form-control-sm')
					.attr('type', 'number')
					.attr('data-required', 'required')
					.attr('min', '1').
					val(5)
				)
			),
			// Term fields
			$('<div>').addClass('form-group row').attr('style', 'display: none;').append(
				$('<label>').attr('for', 'sel-' + graphType + '-bucket-term-order-by-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Order by'),
				$('<div>').addClass('col-sm-9').append(
					$('<select>').attr('id', 'sel-' + graphType + '-bucket-term-order-by-' + ix).addClass('form-control form-control-sm custom-select')
					.append(function () {
						var optionArray = [];
						optionArray.push($('<option>').attr('value', 'term').text('Term'))
						$.each(getTermAggregatorsOrderByOptions(graphType), function(index, item) {
							optionArray.push(
								$('<option>').attr('value', item.id).text('Metric: ' + item.label)
							)
						})
						optionArray.sort(function(a,b) {
						    var at = $(a).text();
						    var bt = $(b).text();         
						    return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
						});
						return optionArray;
					})
					.val('term')
				)
			),
			$('<div>').addClass('form-group row').attr('style', 'display: none;').append(
				$('<label>').attr('for', 'sel-' + graphType + '-bucket-term-order-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Order'),
				$('<div>').addClass('col-sm-9').append(
					$('<select>').attr('id', 'sel-' + graphType + '-bucket-term-order-' + ix).addClass('form-control form-control-sm custom-select')
					.append(
						$('<option>').attr('value', 'asc').text('Ascending'),
						$('<option>').attr('value', 'desc').text('Descending')
					)
					.val('desc')
				)
			),
			$('<div>').addClass('form-group row').attr('style', 'display: none;').append(
				$('<label>').attr('for', 'sel-' + graphType + '-bucket-term-top-' + ix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Top'),
				$('<div>').addClass('col-sm-9').append(
					$('<input>').attr('id', 'sel-' + graphType + '-bucket-term-top-' + ix).addClass('form-control form-control-sm')
					.attr('type', 'number')
					.attr('data-required', 'required')
					.attr('min', '1').
					val(5)
				)
			)			
		)
		if (ix > 0)  {
			$(parent).append(
				$('<a>').attr('href', '#').addClass('pull-right').attr('data-element-type', 'remove-bucket-aggregator').text('Remove aggregator'),
				$('<br>')
			);
		}
		if (aggregatorData) {
			$('#sel-' + graphType + '-bucket-aggregator-' + ix).val(aggregatorData.aggregator).trigger('change');
			if ('filter' != aggregatorData.aggregator) {
				$('#input-' + graphType + '-bucket-field-' + ix).val(aggregatorData.field);
			}
			if ('date_histogram' == aggregatorData.aggregator) {
				$('#sel-' + graphType + '-bucket-date-interval-' + ix).val(aggregatorData.interval);
			} else if ('histogram' == aggregatorData.aggregator) {
				$('#input-' + graphType + '-bucket-histogram-interval-' + ix).val(aggregatorData.interval);
			} else if ('significant_term' == aggregatorData.aggregator) {
				$('#sel-' + graphType + '-bucket-significant-term-top-' + ix).val(aggregatorData.top);
			} else if ('term' == aggregatorData.aggregator) {
				$('#sel-' + graphType + '-bucket-term-order-by-' + ix).val(aggregatorData.order_by);
				$('#sel-' + graphType + '-bucket-term-order-' + ix).val(aggregatorData.order);
				$('#sel-' + graphType + '-bucket-term-top-' + ix).val(aggregatorData.top);
			}
		}
		enableOrDisableButtons();
	}
	
	function getBucketAggregatorsBlock(graphType, ix) {
		var aggregatorData = {
			aggregator: $('#sel-' + graphType + '-bucket-aggregator-' + ix).val(),
		};
		if ('filter' != aggregatorData.aggregator) {
			aggregatorData.field = $('#input-' + graphType + '-bucket-field-' + ix).val(),
			aggregatorData.field_type = findFieldType($('#sel-data-source').val(), aggregatorData.field);
		}
		if ('date_histogram' == aggregatorData.aggregator) {
			aggregatorData.interval = $('#sel-' + graphType + '-bucket-date-interval-' + ix).val();
		} else if ('histogram' == aggregatorData.aggregator) {
			aggregatorData.interval = Number($('#input-' + graphType + '-bucket-histogram-interval-' + ix).val());
		} else if ('significant_term' == aggregatorData.aggregator) {
			aggregatorData.top = Number($('#sel-' + graphType + '-bucket-significant-term-top-' + ix).val());
		} else if ('term' == aggregatorData.aggregator) {
			aggregatorData.order_by = $('#sel-' + graphType + '-bucket-term-order-by-' + ix).val();
			aggregatorData.order = $('#sel-' + graphType + '-bucket-term-order-' + ix).val();
			aggregatorData.top = Number($('#sel-' + graphType + '-bucket-term-top-' + ix).val());
		}
		return aggregatorData;		
	}
	
	function enableOrDisableButtons() {
		//  First remove the required constraints to check if all other fields are valid.
		$('#input-graph-name').removeAttr('required');
		$hiddenRequiredElements = $('#graph_form :input[data-required]:visible').attr('required', 'required');
		$hiddenRequiredElements = $('#graph_form :input[data-required]:hidden');
		// Remove required constraints on all hidden input fields.
		$hiddenRequiredElements.removeAttr('required');
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
	
	function updateBucketTermAggregatorsOrderBySelector(graphType) {
		// First create all the options 
		$('[id^=sel-' + graphType + '-bucket-term-order-by-]').each(function(index) {
			var currentValue = $(this).val();
			$element = $(this).empty();
			$.each(getTermAggregatorsOrderByOptions(graphType), function(index, item) {
				$element.append(
					$('<option>').attr('value', item.id).text('Metric: ' + item.label)
				)
			});
			$element.append($('<option>').attr('value', 'term').text('Term'));
			sortSelectOptions($(this));
			if ($(this).children("option[value='" + currentValue + "']").length) {
				$(this).val(currentValue);
			}
		});
	}
	
	function getTermAggregatorsOrderByOptions(graphType) {
		var options = [];
		$('#' + graphType + '-y-axis').find('[id^=sel-' + graphType + '-metrics-aggregator-]').each(function (index) {
			var ix = $(this).attr('id').split('-')[4];
			var label = $('#input-' + graphType + '-metrics-label-' + ix).val();
			if (!label) {
				label = $(this).children('option:selected').text();
			}
			options.push({ id: 'metric_' + ix, label: label});
		});		
		return options;
	}
	
	function isGraphExistent(name) {
		return "undefined" != typeof graphMap[name];
	}
	
	function saveGraph() {
		var graphData = createGraphData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/dashboard/graph/' + encodeURIComponent(graphData.name),
            data: JSON.stringify(graphData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if (!isGraphExistent(graphData.name)) {
        			$graphSelect = $('#sel-graph');
        			$graphSelect.append($('<option>').attr('value', graphData.name).text(graphData.name));
        			sortSelectOptions($graphSelect);
        		}
        		graphMap[graphData.name] = graphData;
        		$('#graphs_infoBox').text('Graph \'' + graphData.name + '\' saved.').show('fast').delay(5000).hide('fast');
        		enableOrDisableButtons();
            }
        }).always(function () {
        	$('#modal-graph-overwrite').modal('hide');
        });    		
	}
	
	function removeGraph(graphName) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/dashboard/graph/' + encodeURIComponent(graphName),
            success: function(data) {
                if (!data) {
                    return;
                }
        		delete graphMap[graphName];
        		$("#sel-graph > option").filter(function(i){
     		       return $(this).attr("value") == graphName;
        		}).remove();
        		$('#graphs_infoBox').text('Graph \'' + graphName + '\' removed.').show('fast').delay(5000).hide('fast');
        		enableOrDisableButtons();
            }
        }).always(function () {
        	$('#modal-graph-remove').modal('hide');
        });    		
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
        		            .staggerLabels(true)
        		            .wrapLabels(true)
        		            .rotateLabels(-90)
        		            .showControls(true)
        		            .groupSpacing(0.1) 
        		            .duration(250)
        		            ;
        		        chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
        		        if (data.data && data.data && data.data.length > 0) {
        		        	var caluclatedBottomMargin = Number(data.data[0].max_label_length) * 7;
        		        	chart.margin({bottom: caluclatedBottomMargin});
        		        }
        		        data.data.sort(function(a, b){
        				    if (a.key < b.key) return -1;
        				    if (b.key < a.key) return 1;
        				    return 0;
        				});

        		        d3.select('#preview_box').append("svg").attr("style", "height: 20em;")
        		        	.datum(data.data)
        		        	.call(chart);
        		        nv.utils.windowResize(chart.update);
        		        return chart;
        		    });
        		} else if ('line' == data.type) {
        			formatter = d3.locale(data.d3_formatter);
        			numberFormatter = formatter.numberFormat(',f');
        		    nv.addGraph(function() {
        		    	var i = 1
        		        var chart = nv.models.lineChart()
						    .showYAxis(true)
						    .showXAxis(true)       
        		            .useInteractiveGuideline(true)  
        		            .showLegend(true)
        		            .duration(250)
        		            ;
        		        chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
       		            chart.xAxis.tickFormat(function(d,s) {return data.data[0].values[d].label});
        		        d3.select('#preview_box').append("svg").attr("style", "height: 20em;")
        		        	.datum(formatLineData(data.data))
        		        	.call(chart);
        		        	nv.utils.windowResize(chart.update);
        		        return chart;
        		    });
        		} else if ('number' == data.type) {
        			$('#preview_box').append($('<h1>').text(data.value_as_string),$('<h4>').text(data.label));
        		} else if ('pie' == data.type) {
        		} else if ('stacked_area' == data.type) {
        			formatter = d3.locale(data.d3_formatter);
        			numberFormatter = formatter.numberFormat(',f');
        		    nv.addGraph(function() {
        		        var chart = nv.models.stackedAreaChart()
        		            .useInteractiveGuideline(true)
        		            .duration(250)
        		            .showControls(true)
        		            .clipEdge(true);
        		            ;
        		        chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
        		        chart.xAxis.tickFormat(function(d,s) {return data.data[0].values[d].label});
        		        d3.select('#preview_box').append("svg").attr("style", "height: 20em;")
        		        	.datum(formatLineData(data.data))
        		        	.call(chart);
        		        nv.utils.windowResize(chart.update);
        		        return chart;
        		    });
        		}
            }
        });
        
		function formatLineData(lineData) {
			var formattedData = [];
			$.each(lineData, function(index, serie) {
				var serieData = {
					key: serie.key,
					values: []
				};
				$.each(serie.values, function(serieIndex, point) {
					serieData.values.push(
						{
							x: serieIndex,
							y: point.value
						}
					);
				});
				formattedData.push(serieData);
			});
			formattedData.sort(function(a, b){
			    if (a.key < b.key) return -1;
			    if (b.key < a.key) return 1;
			    return 0;
			});
			return formattedData;
		}
	}
	
	function resetValues() {
		document.getElementById('graph_form').reset();
		$('#sel-graph-type').trigger("change");
		$('#' + type + '-x-axis').parent().find("a[data-element-type='add-x-axis-bucket-aggregator']").show();
		enableOrDisableButtons();
	}
	
	function setValuesFromData(graphData) {
		$('#input-graph-name').val(graphData.name);
		$('#sel-data-source').val(graphData.data_source);
		$('#sel-graph-type').val(graphData.type).trigger("change");
		$('#input-graph-query').val(graphData.query ? graphData.query : '*');
		if ('bar' == graphData.type) {
			setMultiBucketData(graphData.type, graphData.bar);
		} else if ('line' == graphData.type) {
			setMultiBucketData(graphData.type, graphData.line);
		} else if ('number' == graphData.type) {
			setNumberAggregatorData(graphData.number);
		} else if ('pie' == graphData.type) {
			
		} else if ('stacked_area' == graphData.type) {
			setMultiBucketData(graphData.type, graphData.stacked_area);
		}

		function setMultiBucketData(type, data) {
			$('#' + type + '-y-axis, #' + type + '-x-axis').empty();
			$('#' + type + '-x-axis').parent().find("a[data-element-type='add-x-axis-bucket-aggregator']").show();
			$.each(data.y_axis.aggregators, function(index, aggregator) {
				addMetricsAggregatorsBlock($('#' + type + '-y-axis'), type, index, aggregator);
			})
	        addBucketAggregatorsBlock($('#' + type + '-x-axis'), type, 0, data.x_axis.aggregator);
			if (data.x_axis.sub_aggregator) {
				$('#' + type + '-x-axis').parent().find("a[data-element-type='add-x-axis-bucket-aggregator']").hide();
				addBucketAggregatorsBlock($('#' + type + '-x-axis'), type, 1, data.x_axis.sub_aggregator);
			}
		}
		
		function setNumberAggregatorData(data) {
			$('#number-fields').empty();
			addMetricsAggregatorsBlock($('#number-fields'),'number', 0, data);
		}
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
			graphData.bar = createMultiBucketData(graphData.type);
		} else if ('line' == graphData.type) {
			graphData.line = createMultiBucketData(graphData.type);
		} else if ('number' == graphData.type) {
			graphData.number = getMetricsAggregatorsBlock(graphData.type, 0);
		} else if ('pie' == graphData.type) {
			
		} else if ('stacked_area' == graphData.type) {
			graphData.stacked_area = createMultiBucketData(graphData.type);
		}
		return graphData;
		
		function createMultiBucketData(type) {
			var data = {
				y_axis: {
					aggregators: []
				},
				x_axis: {
					aggregator: getBucketAggregatorsBlock(graphData.type, 0)
				}
			}
			$('[id^=sel-' + type + '-metrics-aggregator-]').each(function(index) {
				var barIx = $(this).attr('id').split('-')[4];
				data.y_axis.aggregators.push(getMetricsAggregatorsBlock(type, barIx));
			});			
			// Check for a sub aggregator on the x-axis
			if ($('[id=sel-' + type + '-bucket-aggregator-1]').length) {
				data.x_axis.sub_aggregator = getBucketAggregatorsBlock(type, 1);
			}
			return data;
		}  
	}
	
	function showFieldGroup(groupName) {
		$('div[data-field-group]').hide();
		$('div[data-field-group="' + groupName + '"]').show();
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