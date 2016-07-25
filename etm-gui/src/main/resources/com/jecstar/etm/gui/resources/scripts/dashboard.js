function loadDashboard(name) {
	var currentDashboard = {
			name: 'My Dashboard',
			rows: [
			  { height: 16,
				cols: [
				  { 
					id: 'chart1',
					parts: 6,
					title: 'Log types over time',
					bordered: true,
					showLegend: false,
					type: 'line',
					data: {
						index: 'etm_event_all',
						index_types: ['log'],
						agg: {
							name: 'Logs over time',
							type: 'date-histogram',
							field: 'endpoints.writing_endpoint_handler.handling_time',
							interval: 'day',
							aggs: [
							       {
							    	   name: 'Log levels',
							    	   type: 'term',
							    	   field: 'log_level'
							       }
							]
						}
					},
					elementSelector: 'Logs over time->Log levels',
					x_axis: { 
						selector: '$key',
//						label: 'Events over time'
					},
					y_axis: {
						selector: '$doc_count',
						label: 'Count'
					}					
				  }	  
				]
			  }
			]	
	}
	if (name) {
		// TODO load dashboard into currentDashboard
	}
	buildPage(currentDashboard);
	
	$('#lnk-edit-dashboard').click(function (event) {
		event.preventDefault();
		$('#input-dashboard-name').val(currentDashboard.name);
		$('#modal-dashboard-settings').modal();
	});
	
	$('#btn-apply-dashboard-settings').click(function (event) {
		currentDashboard.name = $('#input-dashboard-name').val();
		$('#modal-dashboard-settings').modal('hide');
		buildPage(currentDashboard);
	});
	
	function buildPage(board) {
		$('#dashboard-name').text(board.name);
		var graphContainer = $('#graph-container').empty();
		$.each(board.rows, function(rowIx, row) {
			var rowContainer = $('<div>').addClass('row').attr('style', 'height: ' + row.height + 'rem;');
			$.each(row.cols, function (colIx, col) {
				var colContainer = $('<div>').addClass('col-lg-' + col.parts).attr('style', 'height: 100%;');
				var svgContainer = colContainer;
				if (col.bordered) {
					var card = $('<div>').addClass('card card-block').attr('style', 'height: 100%;');
					svgContainer = card;
					colContainer.append(card);
				}
				svgContainer.append(
				  $('<h5>').addClass('card-title').text(col.title).append(
				    $('<i>').addClass('fa fa-pencil-square-o pull-right')
				  )
				);
				rowContainer.append(colContainer);
				// Now load the data.
			    $.ajax({
			        type: 'POST',
			        contentType: 'application/json',
			        url: '../rest/dashboard/chart',
			        data: JSON.stringify(col),
			        success: function(data) {
			            if (!data) {
			                return;
			            }
			            if ('line' == col.type) {
			            	renderLineChart(svgContainer, col, data);
			            }
			        }
			    });							
			});
			graphContainer.append(rowContainer);
		});
	}
	
	function renderLineChart(svgContainer, config, data) {
        nv.addGraph(function() {
      	  var chart = nv.models.lineChart()
            .x(function(d) { return d[0] })
            .y(function(d) { return d[1] })
      	  	.useInteractiveGuideline(false)
      	  	.showLegend(config.showLegend);

          chart.xAxis
            .axisLabel(config.x_axis.label)
            .tickFormat(function(d) { return d3.time.format('%Y-%m-%d')(new Date(d)) });
          chart.yAxis
          	.axisLabel(config.y_axis.label)

      	  d3.select(svgContainer.get(0))   
      	  	.append("svg")
      	    .datum(data)
      	    .call(chart);

      	  nv.utils.windowResize(function() { chart.update() });
      	  return chart;
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