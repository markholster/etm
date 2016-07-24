function loadDashboard(name) {
	var currentDashboard = {
			name: 'My Dashboard',
			rows: [
			  { height: 16,
				cols: [
				  { 
					id: '3523523',
					parts: 6,
					title: 'Test colom 1',
					bordered: true,
					data: {
						index: 'etm_event_all',
						type: 'line',
						agg: {
							name: 'Events over time',
							type: 'date-histogram',
							field: 'endpoints.writing_endpoint_handler.handling_time',
							interval: 'hour'
						},
						x_axis: { 
							selector: 'Events over time->$key',
							label: 'Events over time'
						},
						y_axis: {
							selector: 'Events over time->$doc_count',
							label: 'Count'
						}
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
				var colContainer = $('<div>').addClass('col-md-' + col.parts).attr('style', 'height: 100%;');
				var svgContainer = colContainer;
				var svg = $('<svg>').attr('id', col.id).attr('style', 'height: 100%; display: block;');
				if (col.bordered) {
					var card = $('<div>').addClass('card card-block').attr('style', 'height: 100%;');
					svgContainer = card;
					colContainer.append(card);
					svg.addClass('card-img');
				}
				svgContainer.append(
				  $('<h5>').addClass('card-title').text(col.title).append(
				    $('<i>').addClass('fa fa-pencil-square-o pull-right')
				  ),
				  svg
				);
				rowContainer.append(colContainer);
				// Now load the data.
			    $.ajax({
			        type: 'POST',
			        contentType: 'application/json',
			        url: '../rest/dashboard/chart',
			        data: JSON.stringify(col.data),
			        success: function(data) {
			            if (!data) {
			                return;
			            }
			            if ('line' == col.data.type) {
			            	renderLineChart(svg, col.data, data);
			            }
			        }
			    });
			});
			graphContainer.append(rowContainer);
		});
	}
	
	function renderLineChart(svg, config, data) {
        nv.addGraph(function() {
      	  var chart = nv.models.lineChart()
      	                .margin({left: 100})  //Adjust chart margins to give the x-axis some breathing room.
      	                .useInteractiveGuideline(true)  //We want nice looking tooltips and a guideline!
      	                .transitionDuration(350)  //how fast do you want the lines to transition?
      	                .showLegend(true)       //Show the legend, allowing users to turn on/off line series.
      	                .showYAxis(true)        //Show the y-axis
      	                .showXAxis(true)        //Show the x-axis
      	  ;

      	  chart.xAxis     //Chart x-axis settings
      	      .axisLabel(config.x_axis.label)
      	      .tickFormat(d3.format(',r'));

      	  chart.yAxis     //Chart y-axis settings
      	      .axisLabel(config.y_axis.label)
      	      .tickFormat(d3.format('.02f'));

      	  d3.select(svg.get(0))    //Select the <svg> element you want to render the chart in.   
      	      .datum(data)         //Populate the <svg> element with chart data...
      	      .call(chart);          //Finally, render the chart!

      	  //Update the chart when window resizes.
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