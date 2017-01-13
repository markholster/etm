function loadDashboard(name) {
	var maxParts = 12;
	var graphMap = {};
	var keywords = [];
	var currentDashboard;
	var currentGraph;

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
	    }),
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
        			return index != currentGraph.data_source;
        		}
        	}
        );
	});    
	
	$('#btn-save-dashboard-settings').click(function (event) {
		if (!document.getElementById('dashboard-settings_form').checkValidity()) {
			return;
		}
		event.preventDefault();
		
		if (!currentDashboard) {
			currentDashboard = {
			};
		}
		currentDashboard.name = $('#input-dashboard-name').val();
		var oldRows = currentDashboard.rows;
		currentDashboard.rows = [];
		$('#dashboard-settings-columns > div.fieldConfigurationRow').each(function (rowIx, row){
			var nrOfCols = $(row).find("input[name='row-cols']").val();
			var height = $(row).find("input[name='row-height']").val();
			var oldRow;
			var rowId = $(row).attr('data-row-id');
			if (oldRows && rowId) {
				// find the row that is edited.
				oldRow = $.grep(oldRows, function(n, i) {
					return n.id == rowId;
				})[0];
			}
			var jsonRow = {
					id: generateUUID(),
					height: height,
					cols: []
			}
			if (oldRow) {
				jsonRow.id = oldRow.id;
				jsonRow.height =  height
			} 
			if (oldRow && oldRow.cols && oldRow.cols.length == nrOfCols) {
				// Number of columns in row not changed.
				jsonRow.cols = oldRow.cols;
			} else {
				var remainingParts = maxParts;
				for (i=0; i< nrOfCols; i++) {
					var parts = Math.ceil(remainingParts / (nrOfCols - i));
					column = {
						id: generateUUID(),
						parts: parts,
						bordered: true
					}
					if (oldRow && oldRow.cols && oldRow.cols[i]) {
						column = oldRow.cols[i];
						column.parts = parts;
					}
					jsonRow.cols.push(column);
					remainingParts -= parts;
				}		
			}
			currentDashboard.rows.push(jsonRow);
		});
		
		$('#dashboard-settings').hide();
		$('#dashboard-container').show();
		buildPage(currentDashboard);
	});
	
	$('#dashboard-container').on("mouseover mouseout", 'div[data-col-id]', function(event) {
		$(this).find("a[data-link-action='edit-graph']").toggleClass('invisible');
		if ($(this).prev().length) {
			// A column on the left hand size is present
			$(this).find("div[data-resize-action='bottom-left']").toggleClass('invisible');
			if ($(this).parent().prev().length) {
				// A row above this row is present
				$(this).find("div[data-resize-action='top-left']").toggleClass('invisible');
			}
		}
		if ($(this).next().length) {
			// A column on the right hand size is present
			$(this).find("div[data-resize-action='bottom-right']").toggleClass('invisible');
			if ($(this).parent().prev().length) {
				// A row above this row is present
				$(this).find("div[data-resize-action='top-right']").toggleClass('invisible');
			}
		}
		$(this).find(".card").toggleClass('selectedColumn');
		if ('false' == $(this).attr('data-col-bordered')) {
			$(this).find(".card").toggleClass('noBorder');
		}
	});
	
	var dragstatus;
	$('#dashboard-container').on("mousedown", 'div[data-resize-action]', function(event) {
		dragstatus = {x: event.pageX, y: event.pageY, id: $(this).parent().parent().attr('data-col-id')};
	});

	$('#dashboard-container').on("mousemove", function(event) {
		if (dragstatus) {
			var divToResize = $('div[data-col-id="' + dragstatus.id + '"]');
			if ((parseInt(divToResize.offset().top, 10) + divToResize.height()) < event.pageY) {
				var currentRow;
				var currentCol; 
				for (rowIx=0; rowIx < currentDashboard.rows.length; rowIx++) {
					for (colIx=0; colIx < currentDashboard.rows[rowIx].cols.length; colIx++) {
						if (currentDashboard.rows[rowIx].cols[colIx].id == divToResize.attr('data-col-id')) {
							currentRow = currentDashboard.rows[rowIx];
							currentCol = currentRow.cols[colIx];
						}
					}
				}
				currentRow.height += 1;
				divToResize.parent().height(currentRow.height + 'rem');
			}
		}
	});

	$('#dashboard-container').on("mouseup", function(event) {
		dragstatus = null;
	});

	
	$('#dashboard-container').on("click", "a[data-link-action='edit-graph']", function(event) {
    	event.preventDefault();
    	editGraph($(this).parent().parent().parent().attr('data-col-id'));
	});
	
	$('#btn-apply-graph-settings').click(function (event) {
		if (!document.getElementById('graph_form').checkValidity()) {
			return;
		}
		currentGraph.title = $("#input-graph-title").val();
		currentGraph.bordered = $('#sel-graph-border').val() == 'true' ? true : false;
		currentGraph.graph = $('#sel-graph').val();
		currentGraph.query = $('#input-graph-query').val();
		currentGraph.refresh_rate = $('#input-refresh-rate').val() ? Number($('#input-refresh-rate').val()) : null;
		
		$("div[data-col-id='" + currentGraph.id + "']").replaceWith(createCell(currentGraph));
		
		// TODO update data via rest.
		$('#modal-graph-settings').modal('hide');
	});	

	$('#sel-graph').on("change", function(event) {
		event.preventDefault();
		var graphData = graphMap[$(this).val()];
		if ('undefined' !== typeof graphData) {
			currentGraph.data_source = graphData.data_source;
			$('#input-graph-query').val(graphData.query);
		}
	});
	
	$('#dashboard-settings').on('click', 'a[data-row-action]', function(event) {
		event.preventDefault();
		if ('row-up' == $(this).attr('data-row-action')) {
			moveRowUp($(this).parent().parent().parent().parent());
		} else if ('row-down' == $(this).attr('data-row-action')) {
			moveRowDown($(this).parent().parent().parent().parent());
		} else if ('row-remove' == $(this).attr('data-row-action')) {
			removeRow($(this).parent().parent().parent().parent());
		} else if ('row-add' == $(this).attr('data-row-action')) {
			$('#dashboard-settings-columns').append(createColumnSettingsRow());
			updateRowActions('#dashboard-settings-columns .actionRow');
		}

	    function moveRowUp(row) {
	        $(row).after($(row).prev());
	        updateRowActions('#dashboard-settings-columns .actionRow');
	    }

	    function moveRowDown(row) {
	        $(row).before($(row).next());
	        updateRowActions('#dashboard-settings-columns .actionRow');
	    }
	    
		function removeRow(row) {
	        $(row).remove();
	        updateRowActions('#dashboard-settings-columns .actionRow');
	    }
	});
	
	function updateRowActions(selector) {
        $(selector).each(function (index, row) {
            if ($('#dashboard-settings-columns').children().length > 2) {
                if (index == 0) {
                    $(row).find('.fa-arrow-up').hide();
                } else {
                    $(row).find('.fa-arrow-up').show();
                }
                if (index >= $('#dashboard-settings-columns').children().length -2) {
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
	
	function createColumnSettingsRow(rowData) {
        var row = $('<div>').addClass('row fieldConfigurationRow').attr('style', 'margin-top: 5px;');
        $(row).append(
            $('<div>').addClass('col-sm-5').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append(
            		$('<input>').attr('type', 'number').attr('min', '1').attr('max', '12').attr('name', 'row-cols').addClass('form-control form-control-sm').val(1)
            ),
            $('<div>').addClass('col-sm-5').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append(
            		$('<input>').attr('type', 'number').attr('min', '1').attr('max', '50').attr('name', 'row-height').addClass('form-control form-control-sm').val(16)
            )
        );
        var actionDiv = $('<div>').addClass('col-sm-2').append(
            $('<div>').addClass('row actionRow').append(
                $('<div>').addClass('col-sm-1').append($('<a href="#">').attr('data-row-action', 'row-up').addClass('fa fa-arrow-up')),
                $('<div>').addClass('col-sm-1').append($('<a href="#">').attr('data-row-action', 'row-down').addClass('fa fa-arrow-down')),
                $('<div>').addClass('col-sm-1').append($('<a href="#">').attr('data-row-action', 'row-remove').addClass('fa fa-times text-danger'))
            )
        );
        $(row).append($(actionDiv));
        if (rowData) {
        	$(row).attr('data-row-id', rowData.id);
        	$(row).children().each(function (index, child) {
                if (0 === index) {
                    $(child).find('input').val(rowData.cols.length);
                } else if (1 === index) {
                    $(child).find('input').val(rowData.height);
                } 
            }); 
        }
        return row;        
    }
	
	function showSettings(dashboardData) {
		$('#dashboard-settings-columns').empty();
		$('#dashboard-settings-columns').append(
		    $('<div>').addClass('row').append(
		    	$('<div>').addClass('col-sm-5 font-weight-bold').text('Columns'), 
		    	$('<div>').addClass('col-sm-5 font-weight-bold').text('Height'), 
		    	$('<div>').addClass('col-sm-2 font-weight-bold')
		        	.append($('<a href="#">').text('Add row')
		        		.attr('data-row-action', 'row-add')	
		            )        
		        )
		    );			
		if (dashboardData) {
			$('#input-dashboard-name').val(dashboardData.name);
			if (dashboardData.rows) {
				$.each(dashboardData.rows, function(ix, row) {
					$('#dashboard-settings-columns').append(createColumnSettingsRow(row));
				});
				updateRowActions('#dashboard-settings-columns .actionRow');
			}
		} else {
			$('#input-dashboard-name').val('My New Dashboard');
			$('#dashboard-settings-columns').append(createColumnSettingsRow());
		}
		$('#dashboard-container').hide();
		$('#dashboard-settings').show();
	}

	function editGraph(cellId) {
		for (rowIx=0; rowIx < currentDashboard.rows.length; rowIx++) {
			for (colIx=0; colIx < currentDashboard.rows[rowIx].cols.length; colIx++) {
				if (currentDashboard.rows[rowIx].cols[colIx].id == cellId) {
					currentGraph = currentDashboard.rows[rowIx].cols[colIx];
				}
			}
		}
		
		$('#input-graph-title').val(currentGraph.title);
		$('#sel-graph-border').val(currentGraph.bordered ? 'true' : 'false');
		$('#sel-graph').val(currentGraph.graph);
		$('#input-graph-query').val(currentGraph.query);
		$('#input-refresh-rate').val(currentGraph.refresh_rate);
		$('#modal-graph-settings').modal();
	}

	
	function createCell(col) {
		// TODO, col.chart, col.chartData & col.interval moeten niet opgeslagen worden!!!
		col.chart = null;
		col.chartData = null;
		if (col.interval) {
			clearInterval(col.interval);
		}
		col.interval = null;
		var cellContainer = $('<div>').attr('data-col-id', col.id).attr('data-col-bordered', col.bordered).addClass('col-lg-' + col.parts).attr('style', 'height: 100%;');
		var card = $('<div>').addClass('card card-block').attr('style', 'height: 100%;');
		cellContainer.append(card);
		if (!col.bordered) {
			card.addClass('noBorder');
		}
		
		card.append(
		  $('<h5>').addClass('card-title').text(col.title).append(
		    $('<a>').attr('href', '#').attr('data-link-action', 'edit-graph').addClass('fa fa-pencil-square-o pull-right invisible')
		  )
		);
		
		var graphData = graphMap[col.graph];
		if ('undefined' !== typeof graphData) {
			if (col.query) {
				graphData = $.extend(true, {}, graphData);
				graphData.query = col.query;
			}
			updateChart(graphData, col, card);
			if (col.refresh_rate) {
				col.interval = setInterval( function() { updateChart(graphData, col, card); }, col.refresh_rate * 1000 );
			}
		}
		// Bottom right resize icon
		card.append($('<div>').addClass('invisible').attr('data-resize-action', 'bottom-right').attr('style', 'position:absolute;bottom:0px;right:0.5rem;margin:0;cursor:se-resize;').append($('<span>').addClass('fa fa-angle-right').attr('style', '-webkit-transform: rotate(45deg); -moz-transform: rotate(45deg); -ms-transform: rotate(45deg); -o-transform: rotate(45deg); transform: rotate(45deg);')));
		// Bottom left resize icon
		card.append($('<div>').addClass('invisible').attr('data-resize-action', 'bottom-left').attr('style', 'position:absolute;bottom:0px;left:0.5rem;margin:0;cursor:sw-resize;').append($('<span>').addClass('fa fa-angle-left').attr('style', '-webkit-transform: rotate(-45deg); -moz-transform: rotate(-45deg); -ms-transform: rotate(-45deg); -o-transform: rotate(-45deg); transform: rotate(-45deg);')));
		// Top right resize icon
		card.append($('<div>').addClass('invisible').attr('data-resize-action', 'top-right').attr('style', 'position:absolute;top:0px;right:0.5rem;margin:0;cursor:ne-resize;').append($('<span>').addClass('fa fa-angle-right').attr('style', '-webkit-transform: rotate(-45deg); -moz-transform: rotate(-45deg); -ms-transform: rotate(-45deg); -o-transform: rotate(-45deg); transform: rotate(-45deg);')));
		// Top left resize icon
		card.append($('<div>').addClass('invisible').attr('data-resize-action', 'top-left').attr('style', 'position:absolute;top:0px;left:0.5rem;margin:0;cursor:nw-resize;').append($('<span>').addClass('fa fa-angle-left').attr('style', '-webkit-transform: rotate(45deg); -moz-transform: rotate(45deg); -ms-transform: rotate(45deg); -o-transform: rotate(45deg); transform: rotate(45deg);')));
		
		return cellContainer;
	}
	
	function updateChart(graphData, col, card) {
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: '../rest/dashboard/graphdata',
            data: JSON.stringify(graphData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if ('bar' == data.type) {
    		        data.data.sort(function(a, b){
    				    if (a.key < b.key) return -1;
    				    if (b.key < a.key) return 1;
    				    return 0;
    				});
    		        if (col.chart && col.chartData) {
    		        	col.chartData.length = 0;
    		        	$.each(data.data, function(index, item) {
    		        		col.chartData.push(item);
    		        	});
    		        	col.chart.update();
    		        } else {
    		        	formatter = d3.locale(data.d3_formatter);
    		        	numberFormatter = formatter.numberFormat(',f');
	        		    nv.addGraph(function() {
	        		    	col.chart = nv.models.multiBarChart()
	        		            .x(function(d) { return d.label })
	        		            .y(function(d) { return d.value })
	        		            .staggerLabels(true)
	        		            .wrapLabels(true)
	        		            .showControls(true)
	        		            .groupSpacing(0.1) 
	        		            .duration(250)
	        		            ;
	        		    	col.chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
	        		    	col.chart.margin({left: 75, bottom: 50, right: 50});
	        		    	col.chartData = data.data;
	        		    	d3.selectAll($(card).toArray()).append("svg").attr("style", "height: 100%;")
	        		        	.datum(col.chartData)
	        		        	.call(col.chart);
	        		        nv.utils.windowResize(col.chart.update);
	        		        return col.chart;
	        		    });
    		        }
        		} else if ('line' == data.type) {
    		        if (col.chart && col.chartData) {
    		        	col.chartData.length = 0;
    		        	$.each(formatLineData(data.data), function(index, item) {
    		        		col.chartData.push(item);
    		        	});
    		        	col.chart.update();
    		        } else {
    		        	formatter = d3.locale(data.d3_formatter);
    		        	numberFormatter = formatter.numberFormat(',f');
	        		    nv.addGraph(function() {
	        		    	col.chart = nv.models.lineChart()
							    .showYAxis(true)
							    .showXAxis(true)       
	        		            .useInteractiveGuideline(true)  
	        		            .showLegend(true)
	        		            .duration(250)
	        		            ;
	        		    	col.chartData = formatLineData(data.data);
	        		    	col.chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
	        		    	col.chart.xAxis.tickFormat(function(d,s) {return col.chartData[0].values[d].label});
	        		    	col.chart.margin({left: 75, bottom: 50, right: 50});
	        		    	d3.selectAll($(card).toArray()).append("svg").attr("style", "height: 100%;")
	        		        	.datum(col.chartData)
	        		        	.call(col.chart);
	        		        nv.utils.windowResize(col.chart.update);
	        		        return col.chart;
	        		    });
    		        }
        		} else if ('number' == data.type) {
        			$currentValue = $(card).find("h1[data-element-type='number-graph']");
        			if ($currentValue.length) {
        				$currentValue.text(data.value_as_string);
        			} else {
        				$(card).append($('<h1>').attr('data-element-type', 'number-graph').text(data.value_as_string),$('<h4>').text(data.label));
        			}
        		} else if ('pie' == data.type) {
        		} else if ('stacked_area' == data.type) {
    		        if (col.chart && col.chartData) {
    		        	col.chartData.length = 0;
    		        	$.each(formatLineData(data.data), function(index, item) {
    		        		col.chartData.push(item);
    		        	});
    		        	col.chart.update();
    		        } else {
	        			formatter = d3.locale(data.d3_formatter);
	        			numberFormatter = formatter.numberFormat(',f');
	        		    nv.addGraph(function() {
	        		    	col.chart = nv.models.stackedAreaChart()
	        		            .useInteractiveGuideline(true)
	        		            .duration(250)
	        		            .showControls(true)
	        		            .clipEdge(true);
	        		            ;
	        		        col.chartData = formatLineData(data.data);
	        		        col.chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
	        		        col.chart.xAxis.tickFormat(function(d,s) {return col.chartData[0].values[d].label});
	        		        col.chart.margin({left: 75, bottom: 50, right: 50});
	        		        d3.selectAll($(card).toArray()).append("svg").attr("style", "height: 100%;")
	        		        	.datum(col.chartData)
	        		        	.call(col.chart);
	        		        nv.utils.windowResize(col.chart.update);
	        		        return col.chart;
	        		    });
    		        }
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
							y: point.value,
							label: point.label
							
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
	
	function sortSelectOptions($select) {
		var options = $select.children('option');
		options.detach().sort(function(a,b) {
		    var at = $(a).text();
		    var bt = $(b).text();         
		    return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
		});
		options.appendTo($select);
	}
	
// OLD STUFF
	
	
	
	$('#dashboard-name').click(function (event) {
		event.preventDefault();
		showSettings(currentDashboard);
	});
	
	if (name) {
		// TODO load dashboard via rest.
		currentDashboard = {
				name: 'My Dashboard',
				rows: [
				  { 
					id: generateUUID(),
					height: 16,
					cols: [
					  { 
						id: generateUUID(),
						parts: 6,
						title: 'Log types over time',
						bordered: true,
						showLegend: true,
						type: 'line',
						area: true,
						index: {
							name: 'etm_event_all',
							types: ['log'],
						},
						x_axis: { 
							agg: {
								name: 'logs',
								type: 'date-histogram',
								field: 'endpoints.writing_endpoint_handler.handling_time',
								interval: 'hour',
								aggs: [
								       {
								    	   name: 'levels',
								    	   type: 'terms',
								    	   field: 'log_level'
								       }
								]
							}
						},
						y_axis: {
							agg : {
								name: 'count',
								type: 'count',
									
							},
							label: 'Count'
						}					
					  }	
					]
				  }
				]	
		}
		buildPage(currentDashboard);
	} else {
		showSettings();
	}
	
	function buildPage(dashboardData) {
		$('#dashboard-name').text(dashboardData.name);
		var graphContainer = $('#graph-container').empty();
		$.each(dashboardData.rows, function(rowIx, row) {
			var rowContainer = $('<div>').addClass('row').attr('data-row-id', row.id).attr('style', 'height: ' + row.height + 'rem; padding-bottom: 15px;');
			if (rowIx != 0) {
				var oldStyle = $(rowContainer).attr('style');
				$(rowContainer).attr('style', oldStyle + ' padding-top: 15px;');
			}
			$.each(row.cols, function (colIx, col) {
				var cell = createCell(col);
				rowContainer.append(cell);
			});
			graphContainer.append(rowContainer);
		});
	}
	
	// TODO deze functie moet gebruikt worden tijdens het opslaan van een grafiek. Het ID moet dan geset worden naar een unieke waarde
	function generateUUID() {
    	var d = new Date().getTime();
	    if(window.performance && typeof window.performance.now === "function"){
	        d += performance.now(); //use high-precision timer if available
	    }
	    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
	        var r = (d + Math.random()*16)%16 | 0;
	        d = Math.floor(d/16);
	        return (c=='x' ? r : (r&0x3|0x8)).toString(16);
	    });
	    return uuid;
	}
}