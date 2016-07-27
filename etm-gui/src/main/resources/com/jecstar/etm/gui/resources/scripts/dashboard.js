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
							name: 'avg_size',
							type: 'avg',
							field: 'payload_length'
								
						},
						label: 'Average size'
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
      	    .datum(createData(config, data))
      	    .call(chart);

      	  nv.utils.windowResize(function() { chart.update() });
      	  return chart;
      });	
        
      function createData(config, data) {
    	  var result = [];
    	  var xBuckets = data[config.x_axis.agg.name].buckets;
    	  var xSubAggs = config.x_axis.agg.aggs;
    	  
    	  $.each(xBuckets, function(xBucketIx, xBucket) {
    		  var x = xBucket.key;
    		  var seriesName = config.y_axis.agg.name;
    		  if ("undefined" !== typeof xSubAggs) {
    			  var xSubAgg = xSubAggs[0];
    			  var xSubBuckets = xBucket[xSubAgg.name].buckets;
    			  $.each(xSubBuckets, function(xSubBucketIx, xSubBucket) {
    				  seriesName = xSubBucket.key;
    				  var y = "count" == config.y_axis.agg.name ? xSubBucket.doc_count : xSubBucket[config.y_axis.agg.name].value;
    				  addToResult(result, seriesName, x, y);
    			  });
    		  } else {
    			  var y = "count" == config.y_axis.agg.name ? xBucket.doc_count : xBucket[config.y_axis.agg.name].value;
    			  addToResult(result, seriesName, x, y);
    		  }
    	  });
    	  return result;
      }
      
      function addToResult(result, serieName, x, y) {
		  var serie = $.grep(result, function(n,i) {
			  return n.key === serieName;
		  });
		  if ("undefined" !== typeof serie && serie.length > 0) {
			  serie[0].values.push([x, y]);
		  } else {
			  result.push({
				 key: serieName,
				 values: [[x,y]]
			  });
		  }
      }
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