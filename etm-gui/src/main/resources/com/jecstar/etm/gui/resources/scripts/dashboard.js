function loadDashboardPage() {
	var maxParts = 12;
	var graphMap = {};
	var dashboardMap = {};
	var keywords = [];
	var dragstatus;
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
	
	$('#sel-dashboard').change(function(event) {
		event.preventDefault();
		var dashboardData = dashboardMap[$(this).val()];
		if ('undefined' == typeof dashboardData) {
			resetSettings(true);
			return;
		}
		currentDashboard = dashboardData;
		showSettings(dashboardData);
		enableOrDisableButtons();
	});
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/dashboard/dashboards',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        $dashboardSelect = $('#sel-dashboard');
	        $.each(data.dashboards, function(index, dashboard) {
	        	$dashboardSelect.append($('<option>').attr('value', dashboard.name).text(dashboard.name));
	        	dashboardMap[dashboard.name] = dashboard;
	        });
	        sortSelectOptions($dashboardSelect)
	        $dashboardSelect.val('');
	    }
	});
	
	$('#input-dashboard-name').on('input', enableOrDisableButtons);
	
	$('#dashboard-name').click(function (event) {
		event.preventDefault();
		showSettings(currentDashboard);
	});

	
	$('#btn-confirm-save-dashboard').click(function (event) {
		if (!document.getElementById('dashboard-settings_form').checkValidity()) {
			return;
		}
		event.preventDefault();
		var dashboardName = $('#input-dashboard-name').val();
		var dashboardData = applyDashboardSettings();
		if (isDashboardExistent(dashboardName) && dashboardData.changed) {
			$('#overwrite-dashboard-name').text(dashboardName);
			$('#modal-dashboard-overwrite').modal();
		} else {
			delete dashboardData.changed;
			currentDashboard = dashboardData;
			$('#dashboard-container').show();
			buildDashboard(currentDashboard);
			$('#dashboard-settings').hide();
			
		}
	});
	
	$('#btn-save-dashboard').click(function (event) {
		event.preventDefault();
		var dashboardData = applyDashboardSettings();
		if (dashboardData.changed) {
			delete dashboardData.changed;
			currentDashboard = dashboardData;
			saveDashboard();
		}
		$('#modal-dashboard-overwrite').modal('hide');
		$('#dashboard-container').show();
		buildDashboard(currentDashboard);
		$('#dashboard-settings').hide();
	});
	
	$('#dashboard-container').on("mouseover", 'div[data-col-id]', function(event) {
		if (!dragstatus) {
			$(this).find("a[data-link-action='edit-graph']").removeClass('invisible');
			$(this).find("div[data-action='resize-graph']").removeClass('invisible');
			$(this).find(".card").addClass('selectedColumn');
			if ('false' == $(this).attr('data-col-bordered')) {
				$(this).find(".card").removeClass('noBorder');
			}
		}
	});

	$('#dashboard-container').on("mouseout", 'div[data-col-id]', function(event) {
		if (!dragstatus) {
			$(this).find("a[data-link-action='edit-graph']").addClass('invisible');
			$(this).find("div[data-action='resize-graph']").addClass('invisible');
			$(this).find(".card").removeClass('selectedColumn');
			if ('false' == $(this).attr('data-col-bordered')) {
				$(this).find(".card").addClass('noBorder');
			}
		}
	});
	
	$('#dashboard-container').on("mousedown", 'div[data-action="resize-graph"]', function(event) {
		dragstatus = {
			x: event.pageX, 
			y: event.pageY, 
			id: $(this).parent().parent().attr('data-col-id'),
			columnWidth: Math.round($('div[data-column-template-id="1"]').outerWidth()),
			columnPartsLeft: maxParts
		};
		for (rowIx=0; rowIx < currentDashboard.rows.length; rowIx++) {
			for (colIx=0; colIx < currentDashboard.rows[rowIx].cols.length; colIx++) {
				if (currentDashboard.rows[rowIx].cols[colIx].id == $(this).parent().parent().attr('data-col-id')) {
					dragstatus.row = currentDashboard.rows[rowIx];
					dragstatus.col = dragstatus.row.cols[colIx];
				}
			}
		}
		$.each(dragstatus.row.cols, function(index, col) {
			dragstatus.columnPartsLeft -= col.parts;
		});
	});

	$(document).on("mousemove", function(event) {
		if (dragstatus) {
			var fontSize = parseInt($(":root").css("font-size"));
			var divToResize = $('div[data-col-id="' + dragstatus.id + '"]');
			
			var heightPx = event.pageY - parseInt(divToResize.offset().top, 10);
			var widthPx = event.pageX - parseInt(divToResize.offset().left, 10);
			
			// Resize the width
			var parts = Math.round(widthPx / dragstatus.columnWidth);
			if (parts < 1) {
				parts = 1;
			}
			if (parts > dragstatus.columnPartsLeft + dragstatus.col.parts) {
				parts = dragstatus.columnPartsLeft + dragstatus.col.parts;
			}
			if (dragstatus.col.parts != parts) {
				dragstatus.columnPartsLeft -= (parts - dragstatus.col.parts);
				divToResize.removeClass(function (index, className) {
				    return (className.match (/(^|\s)col-lg-\S+/g) || []).join(' ');
				}).addClass("col-lg-" + parts);
				dragstatus.col.parts = parts;
			}
				
			// Resize the height
			var heightRem = Math.round(heightPx / fontSize);
			if (heightRem < 1) {
				heightRem = 1;
			}
			if (heightRem > 50) {
				heightRem = 50;
			}
			if (dragstatus.row.height != heightRem) {
				dragstatus.row.height = heightRem;
				divToResize.parent().height(heightRem + 'rem');
			}
		}
	});

	$(document).on("mouseup", function(event) {
		if (dragstatus) {
			$.each(dragstatus.row.cols, function(index, col) {
				if (col.chart) {
					col.chart.update();
				}
			});
			var resizedDiv =  $('div[data-col-id="' + dragstatus.id + '"]');
			var yLowest = parseInt(resizedDiv.offset().top, 10);
			var yHighest = yLowest + resizedDiv.height();
			var xLowest = parseInt(resizedDiv.offset().left, 10);
			var xHighest = xLowest + resizedDiv.width();
			if (event.pageY < yLowest || event.pageY > yHighest || event.pageX < xLowest || event.pageX > xHighest) {
				// Mouseup outside of div. fire the mouseout event manually
				dragstatus = null;
				resizedDiv.mouseout();
			}
			saveDashboard();
			dragstatus = null;
		}
	});
	
	$('#btn-confirm-remove-dashboard').click(function(event) {
		event.preventDefault();
		$('#remove-dashboard-name').text($('#input-dashboard-name').val());
        $('#modal-dashboard-remove').modal();
	});

	$('#btn-remove-dashboard').click(function(event) {
		removeDashboard($('#input-dashboard-name').val());
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
		var graphData = graphMap[currentGraph.graph];
		if ('undefined' !== typeof graphData) {
			if (graphData.line) {
				currentGraph.interpolation = $('#sel-graph-interpolation').val();
			} else {
				delete currentGraph.interpolation;
			}			
		}
		$("div[data-col-id='" + currentGraph.id + "']").replaceWith(createCell(currentGraph));
		saveDashboard();
		$('#modal-graph-settings').modal('hide');
	});	

	$('#sel-graph').on("change", function(event) {
		event.preventDefault();
		var graphData = graphMap[$(this).val()];
		if ('undefined' !== typeof graphData) {
			currentGraph.data_source = graphData.data_source;
			$('#input-graph-query').val(graphData.query);
			if (graphData.line) {
				$('#row-graph-interpolation').show();
				if (currentGraph.interpolation) {
					$('#sel-graph-interpolation').val(currentGraph.interpolation);
				} else {
					$('#sel-graph-interpolation').val('linear');
				}
			} else {
				$('#row-graph-interpolation').hide();
			}
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
	
	showSettings();
	
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
	
	function resetSettings(addEmptyRow) {
		$('#dashboard-settings-columns').empty();
		$('#dashboard-settings_form').trigger("reset");
		$('#dashboard-settings-columns').append(
		    $('<div>').addClass('row').append(
		    	$('<div>').addClass('col-sm-5 font-weight-bold').text('Columns'), 
		    	$('<div>').addClass('col-sm-5 font-weight-bold').text('Height'), 
		    	$('<div>').addClass('col-sm-2 font-weight-bold')
		        	.append($('<a href="#">').text('Add row').attr('data-row-action', 'row-add')	
		        )        
		    )
		);			
		if (addEmptyRow) {
			$('#dashboard-settings-columns').append(createColumnSettingsRow());
		}
		enableOrDisableButtons();
	}
	
	function showSettings(dashboardData) {
		resetSettings(!dashboardData);
		if (dashboardData) {
			$('#input-dashboard-name').val(dashboardData.name);
			if (dashboardData.rows) {
				$.each(dashboardData.rows, function(ix, row) {
					$('#dashboard-settings-columns').append(createColumnSettingsRow(row));
				});
				updateRowActions('#dashboard-settings-columns .actionRow');
			}
		}
		enableOrDisableButtons();
		$('#dashboard-container').hide();
		$('#dashboard-settings').show();
	}
	
	function applyDashboardSettings() {
		var dashboardData = {
			changed: false
		};
		if (!currentDashboard) {
			dashboardData.changed = true;
		}
		dashboardData.name = $('#input-dashboard-name').val();
		var oldRows = currentDashboard ? currentDashboard.rows : null;
		dashboardData.rows = [];
		$('#dashboard-settings-columns > div.fieldConfigurationRow').each(function (rowIx, row) {
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
				jsonRow.height = height;
			}  
			if (oldRow && oldRow.height != jsonRow.height) {
				dashboardData.changed = true;
			}
			if (oldRow && oldRow.cols && oldRow.cols.length == nrOfCols) {
				// Number of columns in row not changed.
				jsonRow.cols = oldRow.cols;
			} else {
				dashboardData.changed = true;
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
			dashboardData.rows.push(jsonRow);
		});
		if (!dashboardData.changed) {
			if (oldRows && oldRows.length != dashboardData.rows.length) {
				dashboardData.changed = true;
			}
			if (currentDashboard.name != dashboardData.name) {
				dashboardData.changed = true;
			}
			
		}
		return dashboardData;
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
		
		$('#row-graph-interpolation').hide();
		if (currentGraph.graph) {
			graphData = graphMap[currentGraph.graph];
			if ('undefined' !== graphData && graphData.line) {
				$('#row-graph-interpolation').show();
				if (currentGraph.interpolation) {
					$('#sel-graph-interpolation').val(currentGraph.interpolation);
				} else {
					$('#sel-graph-interpolation').val('linear');
				}
			}
		}
		$('#modal-graph-settings').modal();
	}

	
	function createCell(col) {
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
			graphData = $.extend(true, {}, graphData);
			if (col.query) {
				graphData.query = col.query;
			}
			if (col.interpolation) {
				graphData.interpolation = col.interpolation;
			}
			updateChart(graphData, col, card);
			if (col.refresh_rate) {
				col.interval = setInterval( function() { updateChart(graphData, col, card); }, col.refresh_rate * 1000 );
			}
		}
		// Bottom right resize icon
		card.append($('<div>').addClass('invisible').attr('data-action', 'resize-graph').attr('style', 'position:absolute;bottom:0px;right:0.5rem;margin:0;cursor:se-resize;').append($('<span>').addClass('fa fa-angle-right').attr('style', '-webkit-transform: rotate(45deg); -moz-transform: rotate(45deg); -ms-transform: rotate(45deg); -o-transform: rotate(45deg); transform: rotate(45deg);')));
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
    		        	var formatter = d3.locale(data.d3_formatter);
    		        	var numberFormatter = formatter.numberFormat(graphData.bar.y_axis.format ? graphData.bar.y_axis.format : ',f');
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
    		        	var formatter = d3.locale(data.d3_formatter);
    		        	var numberFormatter = formatter.numberFormat(graphData.line.y_axis.format ? graphData.line.y_axis.format : ',f');
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
	        		    	col.chart.xAxis.tickFormat(function(d,s) { 
	       		            	if (d < 0 || d >= col.chartData[0].values.length) {
	       		            		return '';
	       		            	}; 
	       		            	return col.chartData[0].values[d].label;
	       		            });
	        		    	col.chart.interpolate(graphData.interpolation);
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
    		        	var formatter = d3.locale(data.d3_formatter);
	        			var numberFormatter = formatter.numberFormat(graphData.stacked_area.y_axis.format ? graphData.stacked_area.y_axis.format : ',f');
	        		    nv.addGraph(function() {
	        		    	col.chart = nv.models.stackedAreaChart()
	        		            .useInteractiveGuideline(true)
	        		            .duration(250)
	        		            .showControls(true)
	        		            .clipEdge(true);
	        		            ;
	        		        col.chartData = formatLineData(data.data);
	        		        col.chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
	        		    	col.chart.xAxis.tickFormat(function(d,s) { 
	       		            	if (d < 0 || d >= col.chartData[0].values.length) {
	       		            		return '';
	       		            	}; 
	       		            	return col.chartData[0].values[d].label;
	       		            });
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
	
	function buildDashboard(dashboardData) {
		$('#dashboard-name').text(dashboardData.name);
		var graphContainer = $('#graph-container').empty();
		graphContainer.append($('<div>').attr('id', 'resize-template-row').addClass('row')
			.append(
				$('<div>').attr('data-column-template-id', '1').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '2').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '3').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '4').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '5').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '6').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '7').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '8').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '9').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '10').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '11').addClass('col-lg-1'),
				$('<div>').attr('data-column-template-id', '12').addClass('col-lg-1')
			)
		);
		
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
	
	function enableOrDisableButtons() {
		if (document.getElementById('dashboard-settings_form').checkValidity()) {
			$('#btn-confirm-save-dashboard').removeAttr('disabled');
		} else {
			$('#btn-confirm-save-dashboard').attr('disabled', 'disabled');
		}
		var dashboardName = $('#input-dashboard-name').val();
		if (dashboardName && isDashboardExistent(dashboardName)) {
			$('#btn-confirm-remove-dashboard').removeAttr('disabled');
		} else {
			$('#btn-confirm-remove-dashboard').attr('disabled', 'disabled');
		}
	}
	
	function isDashboardExistent(name) {
		return "undefined" != typeof dashboardMap[name];
	}
	
	function saveDashboard() {
		var dashboardData = createDashboardData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/dashboard/dashboard/' + encodeURIComponent(dashboardData.name),
            data: JSON.stringify(dashboardData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if (!isDashboardExistent(dashboardData.name)) {
        			$dashboardSelect = $('#sel-dashboard');
        			$dashboardSelect.append($('<option>').attr('value', dashboardData.name).text(dashboardData.name));
        			sortSelectOptions($dashboardSelect);
        		}
        		dashboardMap[dashboardData.name] = dashboardData;
        		$('#dashboard-settings_infoBox').text('Dashboard \'' + dashboardData.name + '\' saved.').show('fast').delay(5000).hide('fast');
        		enableOrDisableButtons();
            }
        });   		
	}
	
	function createDashboardData() {
		var dashboardData = $.extend(true, {}, currentDashboard);
		for (rowIx=0; rowIx < dashboardData.rows.length; rowIx++) {
			for (colIx=0; colIx < dashboardData.rows[rowIx].cols.length; colIx++) {
				delete dashboardData.rows[rowIx].cols[colIx].chart;
				delete dashboardData.rows[rowIx].cols[colIx].chartData;
				delete dashboardData.rows[rowIx].cols[colIx].interval;
			}
		}		
		return dashboardData;
	}
	
	function removeDashboard(dashboardName) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/dashboard/dashboard/' + encodeURIComponent(dashboardName),
            success: function(data) {
                if (!data) {
                    return;
                }
        		delete dashboardMap[dashboardName];
        		$("#sel-dashboard > option").filter(function(i){
     		       return $(this).attr("value") == dashboardName;
        		}).remove();
        		$('#dashboard-settings_infoBox').text('Dashboard \'' + dashboardName + '\' removed.').show('fast').delay(5000).hide('fast');
        		enableOrDisableButtons();
            }
        }).always(function () {
        	$('#modal-dashboard-remove').modal('hide');
        });    		
	}
	
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