function buildIndexStatsPage() {
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/indicesstats',
	    cache: false,
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        setStatisticsData(data)
	    }
	});

    function getLineChartDataForPerformance(indices) {
        var search = []
        var index = []
        $.each(indices, function (ix, value) {
            search.push(value.average_search_time ? value.average_search_time : 0);
            index.push(value.average_index_time ? value.average_index_time : 0);
        });
        return [
            {
                data: search,
                name: 'Search'
            },
            {
                data: index,
                name: 'Index'
            }
        ];
    }

    function setStatisticsData(data) {
        $("#text-total-events").text(data.totals.document_count_as_string);
        $("#text-total-size").text(data.totals.size_in_bytes_as_string);

        data.indices.sort(function (a, b) {
            if (a.name == b.name) {
                return 0;
            }
            if (a.name > b.name) {
                return -1;
            } else {
                return 1;
            }
        });
        Highcharts.setOptions({
            lang: {
                decimalPoint: data.locale.decimal,
                thousandsSep: data.locale.thousands
            }
        });

        Highcharts.chart('count_chart', {
            credits: {
                enabled: false
            },
            chart: {
                type: 'column',
                height: '20%'
            },
            legend: {
                enabled: false
            },
            title: {
                text: 'Event count per index'
            },
            plotOptions: {
                column: {
                    colorByPoint: true
                }
            },
            xAxis: {
                categories: $.map(data.indices, function (index) {
                    return index.name;
                })
            },
            yAxis: {
                title: {
                    text: 'Event count'
                }
            },
            series: [{
                name: 'Count',
                data: $.map(data.indices, function (index) {
                    return index.document_count;
                })
            }]
        });
        Highcharts.chart('performance_chart', {
            credits: {
                enabled: false
            },
            chart: {
                type: 'line',
                height: '20%'
            },
            legend: {
                enabled: true
            },
            title: {
                text: 'Performance averages in milliseconds'
            },
            xAxis: {
                categories: $.map(data.indices, function (index) {
                    return index.name;
                })
            },
            tooltip: {
                shared: true
            },
            yAxis: {
                title: {
                    text: 'Milliseconds'
                }
            },
            series: getLineChartDataForPerformance(data.indices)
        });
        Highcharts.setOptions({
            lang: {
                numericSymbols: ["KB", "MB", "GB", "TB", "PB", "EB"]
            }
        });
        Highcharts.chart('size_chart', {
            credits: {
                enabled: false
            },
            chart: {
                type: 'column',
                height: '20%'
            },
            legend: {
                enabled: false
            },
            title: {
                text: 'Size per index'
            },
            plotOptions: {
                column: {
                    colorByPoint: true
                }
            },
            xAxis: {
                categories: $.map(data.indices, function (index) {
                    return index.name;
                })
            },
            yAxis: {
                title: {
                    text: 'Index size'
                }
            },
            tooltip: {
                enabled: true
            },
            series: [{
                name: 'Size',
                data: $.map(data.indices, function (index) {
                    return index.size_in_bytes;
                })
            }]
        });
    }
}