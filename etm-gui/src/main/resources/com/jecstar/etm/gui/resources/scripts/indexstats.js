function buildIndexStatsPage() {
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/indicesstats',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        setStatisticsData(data)
	    }
	});
	
	function setStatisticsData(data) {
		$("#text-total-documents").text(data.totals.document_count_as_string);
		$("#text-total-size").text(data.totals.size_in_bytes_as_string);

		data.indices.sort(function(a, b) {
				    if (a.name == b.name) { return 0; }
				    if (a.name > b.name) { 
				    	return -1; 
				    } else {
				        return 1;
				    }
		});
		formatter = d3.locale(data.d3_formatter);
		numberFormatter = formatter.numberFormat(',f');
	    nv.addGraph(function() {
	        var chart = nv.models.discreteBarChart()
	            .x(function(d) { return d.label })
	            .y(function(d) { return d.value })
	            .valueFormat(numberFormatter)
	            .staggerLabels(true)
	            .wrapLabels(true)
	            .showValues(true)
	            .duration(250)
	            ;
	        chart.yAxis.tickFormat(function(d) {return numberFormatter(d)})
	        d3.select('#count_chart svg')
	            .datum(getBarChartDataForCounts(data.indices))
	            .call(chart);
	        nv.utils.windowResize(chart.update);
	        return chart;
	    });
	    nv.addGraph(function() {
	        var chart = nv.models.discreteBarChart()
	            .x(function(d) { return d.label })
	            .y(function(d) { return d.value })
	            .valueFormat(numberFormatter)
	            .staggerLabels(true)
	            .wrapLabels(true)
	            .showValues(true)
	            .duration(250)
	            ;
	        chart.yAxis.tickFormat(function(d) {return numberFormatter(d)})
	        d3.select('#size_chart svg')
	            .datum(getBarChartDataForSizes(data.indices))
	            .call(chart);
	        nv.utils.windowResize(chart.update);
	        return chart;
	    });
	}
	
	function getBarChartDataForCounts(indices) {
		documentsPerIndex = [
	        {
	            key: "Documents per index",
	            values: []
	        }
	    ];
		$.each(indices, function(index, value){
			documentsPerIndex[0].values.push({ label: value.name.substring(10), value: value.document_count})
		});
		return documentsPerIndex;
	}
	
	function getBarChartDataForSizes(indices) {
		sizePerIndex = [
	        {
	            key: "Size per index",
	            values: []
	        }
	    ];
		$.each(indices, function(index, value){
			sizePerIndex[0].values.push({ label: value.name.substring(10), value: value.size_in_bytes})
		});
		return sizePerIndex;
	}
}