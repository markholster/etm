"use strict";
var maxParts = 12;
var currentDashboardNames;
var settingsListenersAdded = false;
var dashboardData;
var graphMap = {};
var keywords = [];
var pageContextRoot = '../rest/visualization/';
var contextRoot;

/**
*  Function that creates a new dashboard. An empty settings screen will be show.
*/
function newDashboardPage(groupName) {
    contextRoot = pageContextRoot;
    if (groupName) {
        contextRoot += encodeURIComponent(groupName) + '/'
    }
    addListeners();
    initialize(showSettings, false);
}

/**
*  Function that loads an existing dashboard.
*/
function loadDashboardPage(groupName, dashboardName, readonly) {
    contextRoot = pageContextRoot;
    if (groupName) {
        contextRoot += encodeURIComponent(groupName) + '/'
    }
//    currentDashboardNames = loadDashboardName(contextRoot);
    if (!readonly) {
        addListeners();
    }
    initialize(function () {
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: contextRoot + 'dashboard/' + encodeURIComponent(dashboardName),
            cache: false,
            success: function(data) {
               if (!data) {
                   return;
               }
               dashboardData = data;
               buildPageLayout(readonly);
            }
        })
    }, readonly);
}

function initialize(doneFunction, readonly) {
    if (readonly) {
        $.when(
            $.ajax({
                type: 'GET',
                contentType: 'application/json',
                url: contextRoot + 'graphs',
                cache: false,
                success: function(data) {
                    if (!data) {
                        return;
                    }
                    var $graphSelect = $('#sel-graph');
                    $.each(data.graphs, function(index, graph) {
                        $graphSelect.append($('<option>').attr('value', graph.name).text(graph.name));
                        graphMap[graph.name] = graph;
                    });
                    sortSelectOptions($graphSelect)
                    $graphSelect.val('');
                }
            }).done(function () {
                if (doneFunction) {
                    doneFunction();
                }
            })
        );
    } else {
        $('#input-graph-from').parent()
            .flatpickr({
                dateFormat: "Y-m-dTH:i:S",
                enableTime: true,
                enableSeconds: true,
                time_24hr: true,
                allowInput: true,
                defaultHour: 0,
                defaultMinute: 0,
                clickOpens: false,
                wrap: true
            });
        $('#input-graph-till').parent()
            .flatpickr({
                dateFormat: "Y-m-dTH:i:S",
                enableTime: true,
                enableSeconds: true,
                time_24hr: true,
                allowInput: true,
                defaultHour: 23,
                defaultMinute: 59,
                clickOpens: false,
                wrap: true
            });
        $.when(
            $.ajax({
                type: 'GET',
                contentType: 'application/json',
                url: contextRoot + 'keywords',
                cache: false,
                success: function(data) {
                    if (!data || !data.keywords) {
                        return;
                    }
                    keywords = data.keywords;
                }
            }),
            $.ajax({
                type: 'GET',
                contentType: 'application/json',
                url: contextRoot + 'graphs',
                cache: false,
                success: function(data) {
                    if (!data) {
                        return;
                    }
                    var $graphSelect = $('#sel-graph');
                    $.each(data.graphs, function(index, graph) {
                        $graphSelect.append($('<option>').attr('value', graph.name).text(graph.name));
                        graphMap[graph.name] = graph;
                    });
                    sortSelectOptions($graphSelect)
                    $graphSelect.val('');
                }
            }),
            $.ajax({
                type: 'GET',
                contentType: 'application/json',
                url: contextRoot + 'dashboards',
                cache: false,
                success: function(data) {
                    if (!data) {
                        return;
                    }
                    currentDashboardNames = data.dashboards;
                }
            }).done(function () {
                $('#input-graph-query').bind('keydown', function( event ) {
                    if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                        event.stopPropagation();
                    }
                }).autocompleteFieldQuery(
                    {
                        queryKeywords: keywords,
                        keywordIndexFilter: function(index) {
                            return index != graphMap[$('#sel-graph').val()].data_source;
                        }
                    }
                );
                if (doneFunction) {
                    doneFunction();
                }
            })
        );
    }
}

/**
* Function that shows the settings page.
*/
function showSettings() {
    $('#dashboard-container').hide();

    // Add listeners
    if (!settingsListenersAdded) {
        settingsListenersAdded = true;
        $('#dashboard-settings').on('click', 'a[data-row-action]', function(event) {
            event.preventDefault();
            if ('row-up' === $(this).attr('data-row-action')) {
                moveRowUp($(this).parent().parent().parent().parent());
            } else if ('row-down' === $(this).attr('data-row-action')) {
                moveRowDown($(this).parent().parent().parent().parent());
            } else if ('row-remove' === $(this).attr('data-row-action')) {
                removeRow($(this).parent().parent().parent().parent());
            } else if ('row-add' === $(this).attr('data-row-action')) {
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

        $('#input-dashboard-name').on('input', enableOrDisableButtons);

        $('#btn-confirm-save-dashboard').click(function (event) {
            if (!document.getElementById('dashboard-settings_form').checkValidity()) {
                return;
            }
            event.preventDefault();
            var dashboardName = $('#input-dashboard-name').val();
            var newDashboardData = applyDashboardSettings(dashboardData);
            if (isDashboardExistent(dashboardName) && newDashboardData.changed) {
                $('#overwrite-dashboard-name').text(dashboardName);
                $('#modal-dashboard-overwrite').modal();
            } else {
                delete newDashboardData.changed;
                if (!currentDashboardNames) {
                    currentDashboardNames = [];
                }
                currentDashboardNames.push(dashboardName);
                dashboardData = newDashboardData;
                // TODO, toevoegen aan menu
                saveDashboard();
                buildPageLayout(false);
            }
        });

        $('#btn-save-dashboard').click(function (event) {
            event.preventDefault();
            var newDashboardData = applyDashboardSettings(dashboardData);
            if (newDashboardData.changed) {
                delete newDashboardData.changed;
                dashboardData = newDashboardData;
                saveDashboard();
            }
            hideModals($('#modal-dashboard-overwrite'));
            buildPageLayout(false);
        });
    }

    // Initialize settings page
    if (!dashboardData) {
        $('#btn-confirm-remove-dashboard').hide();
        resetSettings(true);
    } else {
        resetSettings(false);
        $('#input-dashboard-name').val(dashboardData.name);
        if (dashboardData.rows) {
            $.each(dashboardData.rows, function(ix, row) {
                $('#dashboard-settings-columns').append(createColumnSettingsRow(row));
            });
            updateRowActions('#dashboard-settings-columns .actionRow');
        }
        enableOrDisableButtons();
    }
    $('#dashboard-settings').show();
    $('#input-dashboard-name').focus();

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

    function applyDashboardSettings(currentDashboard) {

        var newDashboardData = {
            changed: false
        };
        if (!currentDashboard) {
            newDashboardData.changed = true;
        }
        newDashboardData.name = $('#input-dashboard-name').val();
        var oldRows = currentDashboard ? currentDashboard.rows : null;
        newDashboardData.rows = [];
        $('#dashboard-settings-columns > div.fieldConfigurationRow').each(function (rowIx, row) {
            var nrOfCols = Number($(row).find("input[name='row-cols']").val());
            var height = Number($(row).find("input[name='row-height']").val());
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
                newDashboardData.changed = true;
            }
            if (oldRow && oldRow.cols && oldRow.cols.length == nrOfCols) {
                // Number of columns in row not changed.
                jsonRow.cols = oldRow.cols;
            } else {
                newDashboardData.changed = true;
                var remainingParts = maxParts;
                for (var i=0; i< nrOfCols; i++) {
                    var parts = Math.ceil(remainingParts / (nrOfCols - i));
                    var column = {
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
            newDashboardData.rows.push(jsonRow);
        });
        if (!newDashboardData.changed) {
            if (oldRows && oldRows.length != newDashboardData.rows.length) {
                newDashboardData.changed = true;
            }
            if (currentDashboard.name != newDashboardData.name) {
                newDashboardData.changed = true;
            }

        }
        return newDashboardData;
    }
}

function buildPageLayout(readonly) {
    if (readonly) {
        $('#dashboard-name').text(dashboardData.name);
    } else {
        $('#dashboard-name').empty().append(
            $('<a>').attr('href', '#').attr('data-link-action', 'edit-dashboard').text(dashboardData.name)
        );
        $('#dashboard-name').click(function (event) {
            event.preventDefault();
            showSettings(dashboardData);
        });
    }
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
        var rowContainer = $('<div>').addClass('row pb-1').attr('data-row-id', row.id).attr('data-row-ix', rowIx).attr('style', 'height: ' + row.height + 'rem;');
        if (rowIx != 0) {
            $(rowContainer).addClass('pt-2');
        }
        $.each(row.cols, function (colIx, graph) {
            rowContainer.append(createCell(graph, readonly));
        });
        graphContainer.append(rowContainer);
    });
    $('#dashboard-settings').hide();
    $('#dashboard-container').show();
}

function isDashboardExistent(name) {
    return  'undefined' != typeof currentDashboardNames && $.inArray(name, currentDashboardNames) > -1;
}

/**
* Add resize and edit listeners to the dashboard page.
**/
function addListeners() {
    var dragStatus;

    $('#dashboard-container').on("mouseover", 'div[data-cell-id]', function(event) {
        if (!dragStatus) {
            $(this).find("a[data-link-action='edit-graph']").removeClass('invisible');
            $(this).find("div[data-action='resize-graph']").removeClass('invisible');
            $(this).find(".card").addClass('selectedColumn');
            if ('false' == $(this).attr('data-col-bordered')) {
                $(this).find(".card").removeClass('noBorder');
            }
        }
    });

    $('#dashboard-container').on("mouseout", 'div[data-cell-id]', function(event) {
        if (!dragStatus) {
            $(this).find("a[data-link-action='edit-graph']").addClass('invisible');
            $(this).find("div[data-action='resize-graph']").addClass('invisible');
            $(this).find(".card").removeClass('selectedColumn');
            if ('false' == $(this).attr('data-col-bordered')) {
                $(this).find(".card").addClass('noBorder');
            }
        }
    });

    $('#dashboard-container').on("mousedown", 'div[data-action="resize-graph"]', function(event) {
        dragStatus = {
            x: event.pageX,
            y: event.pageY,
            id: $(this).parent().parent().parent().attr('data-cell-id'),
            columnWidth: Math.round($('div[data-column-template-id="1"]').outerWidth()),
            columnPartsLeft: maxParts
        };
        for (var rowIx=0; rowIx < dashboardData.rows.length; rowIx++) {
            for (var colIx=0; colIx < dashboardData.rows[rowIx].cols.length; colIx++) {
                if (dashboardData.rows[rowIx].cols[colIx].id == dragStatus.id) {
                    dragStatus.row = dashboardData.rows[rowIx];
                    dragStatus.col = dragStatus.row.cols[colIx];
                    dragStatus.col.parts = dragStatus.row.cols[colIx].parts;
                }
            }
        }
        $.each(dragStatus.row.cols, function(index, col) {
            dragStatus.columnPartsLeft -= col.parts;
        });
    });

    $(document).on("mousemove", function(event) {
        if (dragStatus) {
            var fontSize = parseInt($(":root").css("font-size"));
            var divToResize = $('div[data-cell-id="' + dragStatus.id + '"]');

            var heightPx = event.pageY - parseInt(divToResize.offset().top, 10);
            var widthPx = event.pageX - parseInt(divToResize.offset().left, 10);

            // Resize the width
            var parts = Math.round(widthPx / dragStatus.columnWidth);
            if (parts < 1) {
                parts = 1;
            }
            if (parts > dragStatus.columnPartsLeft + dragStatus.col.parts) {
                parts = dragStatus.columnPartsLeft + dragStatus.col.parts;
            }
            if (dragStatus.col.parts != parts) {
                dragStatus.columnPartsLeft -= (parts - dragStatus.col.parts);
                divToResize.removeClass(function (index, className) {
                    return (className.match (/(^|\s)col-lg-\S+/g) || []).join(' ');
                }).addClass("col-lg-" + parts);
                dragStatus.col.parts = parts;
            }

            // Resize the height
            var heightRem = Math.round(heightPx / fontSize);
            if (heightRem < 1) {
                heightRem = 1;
            }
            if (heightRem > 50) {
                heightRem = 50;
            }
            if (dragStatus.row.height != heightRem) {
                dragStatus.row.height = heightRem;
                divToResize.parent().height(heightRem + 'rem');
            }
        }
    });

    $(document).on("mouseup", function(event) {
        if (dragStatus) {
            $.each(dragStatus.row.cols, function(index, col) {
                if (col.chart) {
                    col.chart.update();
                }
            });
            var resizedDiv =  $('div[data-cell-id="' + dragStatus.id + '"]');
            var yLowest = parseInt(resizedDiv.offset().top, 10);
            var yHighest = yLowest + resizedDiv.height();
            var xLowest = parseInt(resizedDiv.offset().left, 10);
            var xHighest = xLowest + resizedDiv.width();
            if (event.pageY < yLowest || event.pageY > yHighest || event.pageX < xLowest || event.pageX > xHighest) {
                // Mouseup outside of div. fire the mouseout event manually
                dragStatus = null;
                resizedDiv.mouseout();
            }
            saveDashboard();
            dragStatus = null;
        }
    });

    $('#dashboard-container').on("click", "a[data-link-action='edit-graph']", function(event) {
        event.preventDefault();
        editGraph($(this).parent().parent().parent().parent().attr('data-cell-id'));
    });

    $('#btn-apply-graph-settings').click(function (event) {
        var graphId = $('#input-graph-id').val();
        var graph;
        if (!document.getElementById('graph_form').checkValidity()) {
            return;
        }
        for (var rowIx=0; rowIx < dashboardData.rows.length; rowIx++) {
            for (var colIx=0; colIx < dashboardData.rows[rowIx].cols.length; colIx++) {
                if (dashboardData.rows[rowIx].cols[colIx].id == graphId) {
                    graph = dashboardData.rows[rowIx].cols[colIx];
                }
            }
        }

        graph.title = $("#input-graph-title").val();
        graph.bordered = $('#sel-graph-border').val() == 'true' ? true : false;
        graph.name = $('#sel-graph').val();
        graph.from = $('#input-graph-from').val() ? $('#input-graph-from').val() : null;
        graph.till = $('#input-graph-till').val() ? $('#input-graph-till').val() : null;
        graph.query = $('#input-graph-query').val();
        graph.refresh_rate = $('#input-refresh-rate').val() ? Number($('#input-refresh-rate').val()) : null;
        var graphData = graphMap[graph.name];
        if ('undefined' != typeof graphData) {
            if (graphData.line) {
                graph.interpolation = $('#sel-graph-interpolation').val();
            } else {
                delete graph.interpolation;
            }
        }

        saveDashboard(function () {
            $("div[data-cell-id='" + graph.id + "']").empty().replaceWith(createCell(graph, false));
        });
    });

    $('#sel-graph').on("change", function(event) {
        event.preventDefault();
        var graphData = graphMap[$(this).val()];
        if ('undefined' !== typeof graphData) {
            $('#input-graph-from').val(graphData.from);
            $('#input-graph-till').val(graphData.till);
            $('#input-graph-query').val(graphData.query);
            if (graphData.line) {
                $('#row-graph-interpolation').show();
                if (graphData.interpolation) {
                    $('#sel-graph-interpolation').val(graphData.interpolation);
                } else {
                    $('#sel-graph-interpolation').val('linear');
                }
            } else {
                $('#row-graph-interpolation').hide();
            }
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
}

function editGraph(cellId) {
    var graph = {};
    graph.id = cellId;
    for (var rowIx=0; rowIx < dashboardData.rows.length; rowIx++) {
        for (var colIx=0; colIx < dashboardData.rows[rowIx].cols.length; colIx++) {
            if (dashboardData.rows[rowIx].cols[colIx].id == cellId) {
                graph = dashboardData.rows[rowIx].cols[colIx];
            }
        }
    }
    $('#input-graph-id').val(graph.id);
    $('#input-graph-title').val(graph.title);
    $('#sel-graph-border').val(graph.bordered ? 'true' : 'false');
    $('#sel-graph').val(graph.name);
    $('#input-graph-from').val(graph.from);
    $('#input-graph-till').val(graph.till);
    $('#input-graph-query').val(graph.query);
    $('#input-refresh-rate').val(graph.refresh_rate);

    $('#row-graph-interpolation').hide();
    if (graph.name) {
        var graphData = graphMap[graph.name];
        if ('undefined' != graphData && graphData.line) {
            $('#row-graph-interpolation').show();
            if (graph.interpolation) {
                $('#sel-graph-interpolation').val(graph.interpolation);
            } else {
                $('#sel-graph-interpolation').val('linear');
            }
        }
    }
    $('#modal-graph-settings').modal();
}

function createCell(graph, readonly) {
    var $cellContainer = $("<div>").attr('data-cell-id', graph.id);
    var $card = $('<div>').addClass('card card-block').attr('style', 'height: 100%;');
    var $cardBody = $('<div>').addClass('card-body').attr('style', 'height: 100%;');
    var $cellTitle = $('<h5>').addClass('card-title').attr('style', 'margin-bottom: 0px;');

    $cardBody.append($cellTitle);
    if (!readonly) {
        $cardBody.append($('<div>').addClass('invisible').attr('data-action', 'resize-graph').attr('style', 'position:absolute;bottom:0px;right:0.5rem;margin:0;cursor:se-resize;').append($('<span>').addClass('fa fa-angle-right').attr('style', '-webkit-transform: rotate(45deg); -moz-transform: rotate(45deg); -ms-transform: rotate(45deg); -o-transform: rotate(45deg); transform: rotate(45deg);')));
    }
    $card.append($cardBody);
    $cellContainer.append($card);
    $cellContainer.attr('data-col-bordered', graph.bordered).addClass('col-lg-' + graph.parts).attr('style', 'height: 100%;');
    if (!graph.bordered) {
        $card.addClass('noBorder');
    }
    $cellTitle.text(graph.title);
    if (!readonly) {
        $cellTitle.append($('<a>').attr('href', '#').attr('data-link-action', 'edit-graph').addClass('fa fa-pencil-square-o pull-right invisible'));
    }
    if (graph.interval) {
    // Remove previous update if present.
        clearInterval(graph.interval);
        delete graph.interval;
    }
    delete graph.chart;
    delete graph.chartData;
    if (graph.name) {
        var graphData = graphMap[graph.name];
        if ('undefined' !== typeof graphData) {
            var clonedGraphData = $.extend(true, {}, graphData);
            if (graph.query) {
                clonedGraphData.query = graph.query;
            }
            if (graph.interpolation) {
                clonedGraphData.interpolation = graph.interpolation;
            }
            updateChart(clonedGraphData, graph, $cardBody);
            if (graph.refresh_rate) {
                graph.interval = setInterval( function() { updateChart(clonedGraphData, graph, $cardBody); }, graph.refresh_rate * 1000 );
            }
        }
    }
    return $cellContainer;
}

function saveDashboard(successFunction) {
    var backendData = createDashboardData();
    $.ajax({
        type: 'PUT',
        contentType: 'application/json',
        url: contextRoot + 'dashboard/' + encodeURIComponent(backendData.name),
        cache: false,
        data: JSON.stringify(backendData),
        success: function(data) {
            if (!data) {
                return;
            }
            $('#dashboard-settings_infoBox').text('Dashboard \'' + backendData.name + '\' saved.').show('fast').delay(5000).hide('fast');
            enableOrDisableButtons();
            if (successFunction) {
                successFunction();
            }
        }
    }).always(function () {
        hideModals([$('#modal-dashboard-overwrite'), $('#modal-graph-settings')]);
    });

    function createDashboardData() {
        var clonedDashboardData = $.extend(true, {}, dashboardData);
        for (var rowIx=0; rowIx < clonedDashboardData.rows.length; rowIx++) {
            for (var colIx=0; colIx < clonedDashboardData.rows[rowIx].cols.length; colIx++) {
                delete clonedDashboardData.rows[rowIx].cols[colIx].chart;
                delete clonedDashboardData.rows[rowIx].cols[colIx].chartData;
                delete clonedDashboardData.rows[rowIx].cols[colIx].interval;
            }
        }
        return clonedDashboardData;
    }
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

function updateChart(graphData, graph, cardBody) {
    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: contextRoot + 'graphdata/' + encodeURIComponent(dashboardData.name) + '/' + encodeURIComponent(graph.id),
        cache: false,
        success: function(data) {
            if (!data) {
                return;
            }
            if ('bar' === data.type) {
                data.data.sort(function(a, b){
                    if (a.key < b.key) return -1;
                    if (b.key < a.key) return 1;
                    return 0;
                });
                if (graph.chart && graph.chartData) {
                    graph.chartData.length = 0;
                    $.each(data.data, function(index, item) {
                        graph.chartData.push(item);
                    });
                    graph.chart.update();
                } else {
                    var formatter = d3.locale(data.d3_formatter);
                    var numberFormatter = formatter.numberFormat(graphData.bar.y_axis.format ? graphData.bar.y_axis.format : ',f');
                    nv.addGraph(function() {
                        graph.chart = nv.models.multiBarChart()
                            .x(function(d) { return d.label })
                            .y(function(d) { return d.value })
                            .staggerLabels(true)
                            .wrapLabels(true)
                            .showControls(true)
                            .groupSpacing(0.1)
                            .duration(250)
                            ;
                        graph.chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
                        graph.chart.margin({left: 75, bottom: 50, right: 50});
                        graph.chartData = data.data;
                        d3.selectAll("div[data-cell-id='" + graph.id + "'] > .card-block > .card-body").append("svg").attr("style", "height: 100%;")
                            .datum(graph.chartData)
                            .call(graph.chart);
                        nv.utils.windowResize(graph.chart.update);
                        return graph.chart;
                    });
                }
            } else if ('line' === data.type) {
                if (graph.chart && graph.chartData) {
                    graph.chartData.length = 0;
                    $.each(formatLineData(data.data), function(index, item) {
                        graph.chartData.push(item);
                    });
                    graph.chart.interpolate(graphData.interpolation);
                    graph.chart.update();
                } else {
                    var formatter = d3.locale(data.d3_formatter);
                    var numberFormatter = formatter.numberFormat(graphData.line.y_axis.format ? graphData.line.y_axis.format : ',f');
                    nv.addGraph(function() {
                        graph.chart = nv.models.lineChart()
                            .showYAxis(true)
                            .showXAxis(true)
                            .useInteractiveGuideline(true)
                            .showLegend(true)
                            .duration(250)
                            ;
                        graph.chartData = formatLineData(data.data);
                        graph.chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
                        graph.chart.xAxis.tickFormat(function(d,s) {
                            if (d < 0 || d >= graph.chartData[0].values.length) {
                                return '';
                            };
                            return graph.chartData[0].values[d].label;
                        });

                        graph.chart.interpolate(graphData.interpolation);
                        graph.chart.margin({left: 75, bottom: 50, right: 75});
                        d3.select("div[data-cell-id='" + graph.id + "'] > .card-block > .card-body").append("svg").attr("style", "height: 100%;")
                            .datum(graph.chartData)
                            .call(graph.chart);
                        nv.utils.windowResize(graph.chart.update);
                        return graph.chart;
                    });
                }
            } else if ('number' === data.type) {
                var $currentValue = $("div[data-cell-id='" + graph.id + "'] > .card-block > .card-body").find("h1[data-element-type='number-graph']");
                if ($currentValue.length) {
                    $currentValue.text(data.value_as_string);
                } else {
                    $(cardBody).append($('<h1>').attr('data-element-type', 'number-graph').text(data.value_as_string),$('<h4>').text(data.label));
                }
            } else if ('pie' === data.type) {
            } else if ('stacked_area' === data.type) {
                if (graph.chart && graph.chartData) {
                    graph.chartData.length = 0;
                    $.each(formatLineData(data.data), function(index, item) {
                        graph.chartData.push(item);
                    });
                    graph.chart.update();
                } else {
                    var formatter = d3.locale(data.d3_formatter);
                    var numberFormatter = formatter.numberFormat(graphData.stacked_area.y_axis.format ? graphData.stacked_area.y_axis.format : ',f');
                    nv.addGraph(function() {
                        graph.chart = nv.models.stackedAreaChart()
                            .useInteractiveGuideline(true)
                            .duration(250)
                            .showControls(true)
                            .clipEdge(true);
                            ;
                        graph.chartData = formatLineData(data.data);
                        graph.chart.yAxis.tickFormat(function(d) {return numberFormatter(d)});
                        graph.chart.xAxis.tickFormat(function(d,s) {
                            if (d < 0 || d >= graph.chartData[0].values.length) {
                                return '';
                            };
                            return graph.chartData[0].values[d].label;
                        });
                        graph.chart.margin({left: 75, bottom: 50, right: 50});
                        d3.selectAll("div[data-cell-id='" + graph.id + "'] > .card-block > .card-body").append("svg").attr("style", "height: 100%;")
                            .datum(graph.chartData)
                            .call(graph.chart);
                        nv.utils.windowResize(graph.chart.update);
                        return graph.chart;
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

function removeDashboard(dashboardName) {
    $.ajax({
        type: 'DELETE',
        contentType: 'application/json',
        url: contextRoot + 'dashboard/' + encodeURIComponent(dashboardName),
        cache: false,
        success: function(data) {
            if (!data) {
                return;
            }
            $('#dashboard-settings_infoBox').text('Dashboard \'' + dashboardName + '\' removed.').show('fast').delay(5000).hide('fast');
            enableOrDisableButtons();
        }
    }).always(function () {
        // TODO remove from menu.
        dashboardData = null;
        hideModals([$('#modal-dashboard-remove'), $('#modal-graph-settings')]);
        showSettings();
    });
}