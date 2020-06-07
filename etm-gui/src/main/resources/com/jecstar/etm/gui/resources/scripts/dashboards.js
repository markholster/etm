/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

"use strict";
const maxParts = 12;
let currentDashboardNames;
let dashboardData;
const graphContainerMap = {};
let keywords = [];
const pageContextRoot = '../rest/visualization/';
let contextRoot;

/**
 *  Function that creates a new dashboard. An empty settings screen will be show.
 */
function newDashboardPage(groupName) {
    $('#sub_visualizations').find('.active').removeClass('active');
    contextRoot = pageContextRoot;
    if (groupName) {
        contextRoot += encodeURIComponent(groupName) + '/';
        const $groupItem = $('#sub_visualizations > ul > li > a > p').filter(function () {
            return $(this).text() === groupName;
        });
        const $liItem = $groupItem.parent().removeClass('collapsed').attr('aria-expanded', 'true').parent();
        $liItem.children('div').addClass('show');
        $liItem.find('p:contains(New dashboard)').closest('li').addClass('active');
    } else {
        const $liMenu = $('#sub_vis_user').addClass('show').parent();
        if ($liMenu.length) {
            $liMenu.children('a').removeClass('collapsed').attr('aria-expanded', 'true');
            $liMenu.find('p:contains(New dashboard)').closest('li').addClass('active');
        } else {
            $('#sub_visualizations').find('p:contains(New dashboard)').closest('li').addClass('active');
        }
    }
    addListeners(false);
    initialize(showSettings, false);
}

/**
 *  Function that loads an existing dashboard.
 */
function loadDashboardPage(groupName, dashboardName, readonly) {
    $('#sub_visualizations').find('.active').removeClass('active');
    contextRoot = pageContextRoot;
    if (groupName) {
        contextRoot += encodeURIComponent(groupName) + '/';
        const $groupItem = $('#sub_visualizations > ul > li > a > p').filter(function () {
            return $(this).text() === groupName;
        });
        ;
        const $liItem = $groupItem.parent().removeClass('collapsed').attr('aria-expanded', 'true').parent();
        $liItem.children('div').addClass('show');
        $liItem.find('p:contains(' + dashboardName + ')').closest('li').addClass('active');
    } else {
        const $liMenu = $('#sub_vis_user').addClass('show').parent();
        if ($liMenu.length) {
            $liMenu.children('a').removeClass('collapsed').attr('aria-expanded', 'true');
            $liMenu.find('p:contains(' + dashboardName + ')').closest('li').addClass('active');
        } else {
            $('#sub_visualizations').find('p:contains(' + dashboardName + ')').closest('li').addClass('active');
        }
    }
    addListeners(readonly);
    initialize(function () {
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: contextRoot + 'dashboard/' + encodeURIComponent(dashboardName),
            cache: false,
            success: function (data) {
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
                success: function (data) {
                    if (!data) {
                        return;
                    }
                    const $graphSelect = $('#sel-graph');
                    $.each(data.graphs, function (index, graph) {
                        $graphSelect.append($('<option>').attr('value', graph.name).text(graph.name));
                        graphContainerMap[graph.name] = graph;
                    });
                    commons.sortSelectOptions($graphSelect);
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
        $('#dashboard-settings-columns').sortable({
            items: "div.row:not(.etm-table-header)"
        });
        $.when(
            $.ajax({
                type: 'GET',
                contentType: 'application/json',
                url: contextRoot + 'keywords',
                cache: false,
                success: function (data) {
                    if (!data || !data.keywords) {
                        return;
                    }
                    keywords = data.keywords;
                    $('#input-graph-query').on('keydown', function (event) {
                        if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                            event.stopPropagation();
                        } else if (event.which === $.ui.keyCode.SPACE && event.ctrlKey && !$(this).autocomplete('instance').menu.active) {
                            $(this).autocomplete("search", $(this).val());
                        }
                    }).autocompleteFieldQuery(
                        {
                            queryKeywords: keywords,
                            keywordIndexFilter: function (index) {
                                return index !== graphContainerMap[$('#sel-graph').val()].data.data_source;
                            }
                        }
                    );
                    $('#input-graph-time-filter-field').on('keydown', function (event) {
                        if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                            event.stopPropagation();
                        }
                    }).autocompleteFieldQuery(
                        {
                            queryKeywords: keywords,
                            mode: 'field',
                            keywordFilter: function (index, group, keyword) {
                                return !keyword.date;
                            },
                            keywordIndexFilter: function (index) {
                                return index !== graphContainerMap[$('#sel-graph').val()].data.data_source;
                            }
                        }
                    );
                }
            }),
            $.ajax({
                type: 'GET',
                contentType: 'application/json',
                url: contextRoot + 'graphs',
                cache: false,
                success: function (data) {
                    if (!data) {
                        return;
                    }
                    const $graphSelect = $('#sel-graph');
                    $.each(data.graphs, function (index, graph) {
                        $graphSelect.append($('<option>').attr('value', graph.name).text(graph.name));
                        graphContainerMap[graph.name] = graph;
                    });
                    commons.sortSelectOptions($graphSelect);
                    $graphSelect.val('');
                }
            }),
            $.ajax({
                type: 'GET',
                contentType: 'application/json',
                url: contextRoot + 'dashboards',
                cache: false,
                success: function (data) {
                    if (!data) {
                        return;
                    }
                    currentDashboardNames = data.dashboards;
                }
            }).done(function () {
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
    const $dashboardName = $('#input-dashboard-name');
    const $columns = $('#dashboard-settings-columns');

    $('#dashboard-container').hide();
    // Initialize settings page
    if (!dashboardData) {
        $('#btn-confirm-remove-dashboard').hide();
        resetSettings(true);
    } else {
        resetSettings(false);
        $dashboardName.val(dashboardData.name);
        if (dashboardData.rows) {
            $.each(dashboardData.rows, function (ix, row) {
                $columns.append(createColumnSettingsRow(row));
            });
        }
        enableOrDisableButtons();
    }
    $('#dashboard-settings').show();
    $dashboardName.trigger('focus');

    function resetSettings(addEmptyRow) {
        $columns.empty();
        $('#dashboard-settings_form').trigger("reset");
        $columns.append(
            $('<div>').addClass('row etm-table-header').append(
                $('<div>').addClass('col-sm-5 font-weight-bold').text('Columns'),
                $('<div>').addClass('col-sm-5 font-weight-bold').text('Height'),
                $('<div>').addClass('col-sm-2 font-weight-bold')
                    .append($('<a href="#">').text('Add row').attr('data-row-action', 'row-add')
                    )
            )
        );
        if (addEmptyRow) {
            $columns.append(createColumnSettingsRow());
        }
        enableOrDisableButtons();
    }
}

function buildPageLayout(readonly) {
    const $dashboardName = $('#dashboard-name');
    if (readonly) {
        $dashboardName.text(dashboardData.name);
    } else {
        $dashboardName.empty().append(
            $('<a>').attr('href', '#').attr('data-link-action', 'edit-dashboard').text(dashboardData.name)
        );
        $dashboardName.on('click', function (event) {
            event.preventDefault();
            showSettings(dashboardData);
        });
    }
    const $graphContainer = $('#graph-container').empty();
    $graphContainer.append($('<div>').attr('id', 'resize-template-row').addClass('row')
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
    $.each(dashboardData.rows, function (rowIx, row) {
        const $rowContainer = $('<div>').addClass('row pb-1').attr('data-row-id', row.id).attr('data-row-ix', rowIx).attr('style', 'height: ' + row.height + 'rem;');
        $graphContainer.append($rowContainer);
        if (rowIx !== 0) {
            $rowContainer.addClass('pt-2');
        }
        $.each(row.columns, function (colIx, graph) {
            const $cellContainer = $("<div>").attr('data-cell-id', graph.id);
            $rowContainer.append($cellContainer);
            appendCell($cellContainer, graph, readonly);
        });
    });
    $('#dashboard-settings').hide();
    $('#dashboard-container').show();
}

function isDashboardExistent(name) {
    return 'undefined' != typeof currentDashboardNames && $.inArray(name, currentDashboardNames) > -1;
}

/**
 * Add resize and edit listeners to the dashboard page.
 **/
function addListeners(readonly) {
    let dragStatus;

    const selectorToShowOrHide = readonly ? ".highcharts-exporting-group" : "div[data-action='resize-graph'], .highcharts-exporting-group";
    const $dashboardContainer = $('#dashboard-container');
    $dashboardContainer.on("mouseover", 'div[data-cell-id]', function () {
        if (!dragStatus) {
            const $this = $(this);
            $this.find(selectorToShowOrHide).removeClass('invisible');
            $this.find(".card").addClass('selectedColumn');
        }
    });

    $dashboardContainer.on("mouseout", 'div[data-cell-id]', function () {
        if (!dragStatus) {
            const $this = $(this);
            $this.find(selectorToShowOrHide).addClass('invisible');
            $this.find(".card").removeClass('selectedColumn');
        }
    });

    if (!readonly) {
        $dashboardContainer.on("mousedown", 'div[data-action="resize-graph"]', function (event) {
            dragStatus = {
                x: event.pageX,
                y: event.pageY,
                id: $(this).closest('[data-cell-id]').attr('data-cell-id'),
                columnWidth: Math.round($('div[data-column-template-id="1"]').outerWidth()),
                columnPartsLeft: maxParts
            };
            for (let rowIx = 0; rowIx < dashboardData.rows.length; rowIx++) {
                for (let colIx = 0; colIx < dashboardData.rows[rowIx].columns.length; colIx++) {
                    if (dashboardData.rows[rowIx].columns[colIx].id === dragStatus.id) {
                        dragStatus.row = dashboardData.rows[rowIx];
                        dragStatus.column = dragStatus.row.columns[colIx];
                        dragStatus.column.width = dragStatus.row.columns[colIx].width;
                    }
                }
            }
            $.each(dragStatus.row.columns, function (index, column) {
                dragStatus.columnPartsLeft -= column.width;
            });
        });

        $(document).on("mousemove", function (event) {
            if (dragStatus) {
                const fontSize = parseInt($(":root").css("font-size"));
                const $divToResize = $('div[data-cell-id="' + dragStatus.id + '"]');

                const heightPx = event.pageY - parseInt($divToResize.offset().top, 10);
                const widthPx = event.pageX - parseInt($divToResize.offset().left, 10);

                // Resize the width
                let width = Math.round(widthPx / dragStatus.columnWidth);
                if (width < 1) {
                    width = 1;
                }
                if (width > dragStatus.columnPartsLeft + dragStatus.column.width) {
                    width = dragStatus.columnPartsLeft + dragStatus.column.width;
                }
                let resized = false;
                if (dragStatus.column.width !== width) {
                    dragStatus.columnPartsLeft -= (width - dragStatus.column.width);
                    $divToResize.removeClass(function (index, className) {
                        return (className.match(/(^|\s)col-lg-\S+/g) || []).join(' ');
                    }).addClass("col-lg-" + width);
                    dragStatus.column.width = width;
                    resized = true;
                }

                // Resize the height
                let heightRem = Math.round(heightPx / fontSize);
                if (heightRem < 1) {
                    heightRem = 1;
                }
                if (heightRem > 50) {
                    heightRem = 50;
                }
                if (dragStatus.row.height !== heightRem) {
                    dragStatus.row.height = heightRem;
                    $divToResize.parent().height(heightRem + 'rem');
                    resized = true;
                }
                if (resized) {
                    $.each(dragStatus.row.columns, function (index, column) {
                        if (column.chart) {
                            column.chart.reflow();
                            if (column.graph && 'number' === column.graph.type) {
                                column.chart.redraw();
                            }
                        }
                    });
                }
            }
        });

        $(document).on("mouseup", function (event) {
            if (dragStatus) {
                $.each(dragStatus.row.columns, function (index, col) {
                    if (col.chart) {
                        col.chart.reflow();
                    }
                });
                const $resizedDiv = $('div[data-cell-id="' + dragStatus.id + '"]');
                const yLowest = parseInt($resizedDiv.offset().top, 10);
                const yHighest = yLowest + $resizedDiv.height();
                const xLowest = parseInt($resizedDiv.offset().left, 10);
                const xHighest = xLowest + $resizedDiv.width();
                if (event.pageY < yLowest || event.pageY > yHighest || event.pageX < xLowest || event.pageX > xHighest) {
                    // Mouseup outside of div. fire the mouseout event manually
                    dragStatus = null;
                    $resizedDiv.trigger('mouseout');
                }
                saveDashboard();
                dragStatus = null;
            }
        });


        $dashboardContainer.on("click", "a[data-link-action='edit-graph']", function (event) {
            event.preventDefault();
            editGraph($(this).closest('[data-cell-id]').attr('data-cell-id'));
        });

        $('#btn-apply-graph-settings').on('click', function () {
            const graphId = $('#input-graph-id').val();
            const $form = $('#graph_form');

            $form.find('[data-required="required"]:hidden').removeAttr('required');
            let valid = false;
            if ($form[0].checkValidity()) {
                valid = true;
            }
            $form.find('[data-required]').attr('required', 'required');
            if (!valid) {
                return;
            }
            const graphContainer = graphContainerMap[$('#sel-graph').val()];

            let column;
            for (let rowIx = 0; rowIx < dashboardData.rows.length; rowIx++) {
                for (let colIx = 0; colIx < dashboardData.rows[rowIx].columns.length; colIx++) {
                    if (dashboardData.rows[rowIx].columns[colIx].id === graphId) {
                        column = dashboardData.rows[rowIx].columns[colIx];
                    }
                }
            }
            if (!column.data) {
                column.data = {};
            }
            if (!column.graph) {
                column.graph = {};
            }
            const $from = $('#input-graph-from');
            const $till = $('#input-graph-till');
            const $filterField = $('#input-graph-time-filter-field');

            const $refreshRate = $('#input-refresh-rate');
            column.title = $("#input-graph-title").val();
            column.graph_name = $('#sel-graph').val();
            column.data.from = $from.val() ? $from.val() : null;
            column.data.till = $till.val() ? $till.val() : null;
            column.data.time_filter_field = $filterField.val() ? $filterField.val() : null;
            column.data.query = $('#input-graph-query').val();
            column.refresh_rate = $refreshRate.val() ? Number($refreshRate.val()) : null;
            column.graph.type = graphContainerMap[column.graph_name].graph.type;
            if ($('#grp-graph-subtype:hidden').length === 0) {
                column.graph.sub_type = $('#sel-graph-subtype').val();
            }
            if ($('#grp-graph-font-size:hidden').length === 0) {
                const $fontSize = $('#input-graph-font-size');
                column.graph.font_size = $fontSize.val() ? Number($fontSize.val()) : graphContainer.graph.font_size;
            }
            if ($('#grp-graph-vertical-alignment:hidden').length === 0) {
                column.graph.vertical_alignment = $('#sel-graph-vertical-alignment').val();
            }
            if ($('#grp-graph-line-type:hidden').length === 0) {
                column.graph.line_type = $('#sel-graph-line-type').val();
            }
            if ($('#grp-graph-orientation:hidden').length === 0) {
                column.graph.orientation = $('#sel-graph-orientation').val();
            }
            if ($('#grp-graph-show-markers:hidden').length === 0) {
                column.graph.show_markers = $('#sel-graph-show-markers').val() === 'true';
            }
            if ($('#grp-graph-show-data-labels:hidden').length === 0) {
                column.graph.show_data_labels = $('#sel-graph-show-data-labels').val() === 'true';
            }
            if ($('#grp-graph-show-legend:hidden').length === 0) {
                column.graph.show_legend = $('#sel-graph-show-legend').val() === 'true';
            }
            saveDashboard(function () {
                appendCell($("div[data-cell-id='" + column.id + "']").empty(), column, false);
            });
        });

        $('#sel-graph').on('change', function (event) {
            event.preventDefault();
            const graphContainer = graphContainerMap[$(this).val()];
            if ('undefined' !== typeof graphContainer) {
                $('#input-graph-from').val(graphContainer.data.from);
                $('#input-graph-till').val(graphContainer.data.till);
                $('#input-graph-time-filter-field').val(graphContainer.data.time_filter_field);
                $('#input-graph-query').val(graphContainer.data.query);
                $('#btn-apply-graph-settings').removeAttr('disabled');
                showOrHideGraphSubType(graphContainer, graphContainer.graph.sub_type);
                showOrHideGraphFontSize(graphContainer, graphContainer.graph.font_size);
                showOrHideGraphVerticalAlignment(graphContainer, graphContainer.graph.vertical_alignment);
                showOrHideGraphLineType(graphContainer, graphContainer.graph.line_type);
                showOrHideGraphOrientation(graphContainer, graphContainer.graph.orientation);
                showOrHideGraphShowMarkers(graphContainer, graphContainer.graph.show_markers);
                showOrHideGraphShowDataLabels(graphContainer, graphContainer.graph.show_data_labels);
                showOrHideGraphShowLegend(graphContainer, graphContainer.graph.show_legend);
            } else {
                $('#btn-apply-graph-settings').attr('disabled', 'disabled');
            }
        });

        $('#btn-confirm-remove-dashboard').on('click', function (event) {
            event.preventDefault();
            $('#remove-dashboard-name').text($('#input-dashboard-name').val());
            $('#modal-dashboard-remove').modal();
        });

        $('#btn-remove-dashboard').on('click', function () {
            removeDashboard($('#input-dashboard-name').val());
        });

        const $dashboardSettings = $('#dashboard-settings');
        $dashboardSettings.on('click', 'a[data-row-action]', function (event) {
            event.preventDefault();
            if ('row-remove' === $(this).attr('data-row-action')) {
                removeRow($(this).parent().parent().parent().parent());
            } else if ('row-add' === $(this).attr('data-row-action')) {
                $('#dashboard-settings-columns').append(createColumnSettingsRow());
            }

            function removeRow(row) {
                $(row).remove();
            }
        });

        $dashboardSettings.on('input', 'input', enableOrDisableButtons);

        $('#btn-confirm-save-dashboard').on('click', function (event) {
            if (!document.getElementById('dashboard-settings_form').checkValidity()) {
                return;
            }
            event.preventDefault();
            const dashboardName = $('#input-dashboard-name').val();
            const newDashboardData = applyDashboardSettings(dashboardData);
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
                saveDashboard(function () {
                    buildPageLayout(false);
                });
            }
        });

        $('#btn-save-dashboard').on('click', function (event) {
            event.preventDefault();
            const newDashboardData = applyDashboardSettings(dashboardData);
            if (newDashboardData.changed) {
                delete newDashboardData.changed;
                dashboardData = newDashboardData;
                saveDashboard(function () {
                    buildPageLayout(false);
                });
            }
            commons.hideModals($('#modal-dashboard-overwrite'));
        });
    }
}

function editGraph(cellId) {
    let column = {};
    column.id = cellId;
    for (let rowIx = 0; rowIx < dashboardData.rows.length; rowIx++) {
        for (let colIx = 0; colIx < dashboardData.rows[rowIx].columns.length; colIx++) {
            if (dashboardData.rows[rowIx].columns[colIx].id === cellId) {
                column = dashboardData.rows[rowIx].columns[colIx];
            }
        }
    }
    if (!column.data) {
        column.data = {};
    }
    if (!column.graph) {
        column.graph = {};
    }
    const $selGraph = $('#sel-graph');
    $('#input-graph-id').val(column.id);
    $('#input-graph-title').val(column.title);
    $selGraph.val(column.graph_name);
    $('#input-graph-from').val(column.data.from);
    $('#input-graph-till').val(column.data.till);
    $('#input-graph-time-filter-field').val(column.data.time_filter_field);
    $('#input-graph-query').val(column.data.query);
    $('#input-refresh-rate').val(column.refresh_rate);

    const graphContainer = graphContainerMap[column.graph_name];
    if (graphContainer !== undefined) {
        if (!column.graph.type) {
            column.graph.type = graphContainer.graph.type;
        }
        if (!column.graph.font_size) {
            column.graph.font_size = graphContainer.graph.font_size;
        }
        if (!column.graph.line_type) {
            column.graph.line_type = graphContainer.graph.line_type;
        }
        if (!column.graph.orientation) {
            column.graph.orientation = graphContainer.graph.orientation;
        }
        if (column.graph.show_markers === undefined) {
            column.graph.show_markers = graphContainer.graph.show_markers;
        }
        if (column.graph.show_data_labels === undefined) {
            column.graph.show_data_labels = graphContainer.graph.show_data_labels;
        }
        if (column.graph.show_legend === undefined) {
            column.graph.show_legend = graphContainer.graph.show_legend;
        }
    }
    showOrHideGraphSubType(graphContainer, column.graph.sub_type);
    showOrHideGraphFontSize(graphContainer, column.graph.font_size);
    showOrHideGraphVerticalAlignment(graphContainer, column.graph.vertical_alignment);
    showOrHideGraphLineType(graphContainer, column.graph.line_type);
    showOrHideGraphOrientation(graphContainer, column.graph.orientation);
    showOrHideGraphShowMarkers(graphContainer, column.graph.show_markers);
    showOrHideGraphShowDataLabels(graphContainer, column.graph.show_data_labels);
    showOrHideGraphShowLegend(graphContainer, column.graph.show_legend);

    $('#modal-graph-settings').modal();
}

function showOrHideGraphSubType(graphContainer, graphSubtype) {
    const $grpGraphSubtype = $('#grp-graph-subtype').hide();
    if ('undefined' !== typeof graphContainer) {
        const $graphSubtype = $("#sel-graph-subtype").empty();
        if ('area' === graphContainer.graph.type ||
            'bar' === graphContainer.graph.type ||
            'pie' === graphContainer.graph.type) {
            if ('pie' === graphContainer.graph.type) {
                $graphSubtype.append(
                    $('<option>').attr('value', 'basic').text('Basic'),
                    $('<option>').attr('value', 'donut').text('Donut'),
                    $('<option>').attr('value', 'semi_circle').text('Semi circle')
                );
            } else {
                $graphSubtype.append(
                    $('<option>').attr('value', 'basic').text('Basic'),
                    $('<option>').attr('value', 'percentage').text('Percentage'),
                    $('<option>').attr('value', 'stacked').text('Stacked')
                );
            }
            $graphSubtype.val(graphSubtype ? graphSubtype : graphContainer.graph.sub_type);
            $grpGraphSubtype.show();
        }
    }
}

function showOrHideGraphFontSize(graphContainer, graphFontSize) {
    const $grpGraphFontSize = $('#grp-graph-font-size').hide();
    if ('undefined' !== typeof graphContainer) {
        if ('number' === graphContainer.graph.type) {
            $("#input-graph-font-size").val(graphFontSize !== undefined ? graphFontSize : graphContainer.graph.line_type);
            $grpGraphFontSize.show();
        }
    }
}

function showOrHideGraphVerticalAlignment(graphContainer, graphVerticalAlignment) {
    const $grpGraphVerticalAlignment = $('#grp-graph-vertical-alignment').hide();
    if ('undefined' !== typeof graphContainer) {
        if ('number' === graphContainer.graph.type) {
            $("#sel-graph-vertical-alignment").val(graphVerticalAlignment !== undefined ? graphVerticalAlignment : graphContainer.graph.vertical_alignment);
            $grpGraphVerticalAlignment.show();
        }
    }
}

function showOrHideGraphLineType(graphContainer, graphLineType) {
    const $grpGraphLineType = $('#grp-graph-line-type').hide();
    if ('undefined' !== typeof graphContainer) {
        if ('area' === graphContainer.graph.type ||
            'line' === graphContainer.graph.type) {
            $("#sel-graph-line-type").val(graphLineType !== undefined ? graphLineType : graphContainer.graph.line_type);
            $grpGraphLineType.show();
        }
    }
}

function showOrHideGraphOrientation(graphContainer, graphOrientation) {
    const $grpGraphOrientation = $('#grp-graph-orientation').hide();
    if ('undefined' !== typeof graphContainer) {
        if ('area' === graphContainer.graph.type ||
            'bar' === graphContainer.graph.type ||
            'line' === graphContainer.graph.type ||
            'scatter' === graphContainer.graph.type) {
            $("#sel-graph-orientation").val(graphOrientation !== undefined ? graphOrientation : graphContainer.graph.orientation);
            $grpGraphOrientation.show();
        }
    }
}

function showOrHideGraphShowMarkers(graphContainer, showMarkers) {
    const $grpGraphShowMarkers = $('#grp-graph-show-markers').hide();
    if ('undefined' !== typeof graphContainer) {
        if ('area' === graphContainer.graph.type ||
            'line' === graphContainer.graph.type) {
            $("#sel-graph-show-markers").val(showMarkers !== undefined ? (showMarkers ? 'true' : 'false') : (graphContainer.graph.show_markers ? 'true' : 'false'));
            $grpGraphShowMarkers.show();
        }
    }
}

function showOrHideGraphShowDataLabels(graphContainer, showDataLabels) {
    const $grpGraphShowDataLabels = $('#grp-graph-show-data-labels').hide();
    if ('undefined' !== typeof graphContainer) {
        if ('area' === graphContainer.graph.type ||
            'line' === graphContainer.graph.type ||
            'pie' === graphContainer.graph.type ||
            'scatter' === graphContainer.graph.type) {
            $("#sel-graph-show-data-labels").val(showDataLabels !== undefined ? (showDataLabels ? 'true' : 'false') : (graphContainer.graph.show_data_labels ? 'true' : 'false'));
            $grpGraphShowDataLabels.show();
        }
    }
}

function showOrHideGraphShowLegend(graphContainer, showLegend) {
    const $grpGraphShowLegend = $('#grp-graph-show-legend').hide();
    if ('undefined' !== typeof graphContainer) {
        if ('area' === graphContainer.graph.type ||
            'bar' === graphContainer.graph.type ||
            'line' === graphContainer.graph.type ||
            'pie' === graphContainer.graph.type ||
            'scatter' === graphContainer.graph.type) {
            $("#sel-graph-show-legend").val(showLegend !== undefined ? (showLegend ? 'true' : 'false') : (graphContainer.graph.show_legend ? 'true' : 'false'));
            $grpGraphShowLegend.show();
        }
    }
}

function appendCell($cellContainer, graph, readonly) {

    function createEmptyChart(graph, $renderTo) {
        graph.chart = Highcharts.chart({
            credits: {
                enabled: false
            },
            title: {
                text: ''
            },
            exporting: {
                menuItemDefinitions: {
                    editGraph: {
                        onclick: function () {
                            editGraph(graph.id);
                        },
                        text: 'Edit Graph'
                    }
                },
                buttons: {
                    contextButton: {
                        menuItems: ['editGraph']
                    }
                }
            },
            chart: {
                style: {
                    fontFamily: 'inherit'
                },
                events: {
                    load: function () {
                        $cellContainer.css('overflow', 'visible');
                        $cellContainer.children('.highcharts-container').css('overflow', 'visible');
                    }
                },
                renderTo: $renderTo[0],
            }
        });
        if ($renderTo.closest('.selectedColumn').length === 0) {
            $renderTo.find('.highcharts-exporting-group').addClass('invisible');
        }
    }

    const $card = $('<div>').addClass('card card-block').attr('style', 'height: 100%;');
    const $cardBody = $('<div>').addClass('card-body p-1').attr('style', 'height: 100%;');

    if (!readonly) {
        $cardBody.append($('<div>').addClass('invisible').attr('data-action', 'resize-graph').attr('style', 'position:absolute;bottom:0px;right:0.5rem;margin:0;cursor:se-resize;z-index: 1;').append($('<span>').addClass('fa fa-angle-right').attr('style', '-webkit-transform: rotate(45deg); -moz-transform: rotate(45deg); -ms-transform: rotate(45deg); -o-transform: rotate(45deg); transform: rotate(45deg);')));
    }
    $card.append($cardBody);
    $cellContainer.append($card);
    $cellContainer.addClass('col-lg-' + graph.width).attr('style', 'height: 100%;');
    if (graph.interval) {
        // Remove previous update if present.
        clearInterval(graph.interval);
        delete graph.interval;
    }
    if (graph.chart) {
        graph.chart.destroy();
    }
    delete graph.chart;
    const $graphContainer = $('<div style="height: 100%;">');
    $cardBody.append($graphContainer);
    if (graph.graph_name) {
        const graphData = graphContainerMap[graph.graph_name];
        if ('undefined' !== typeof graphData) {
            updateChart(graph, $graphContainer, readonly);
            if (graph.refresh_rate) {
                graph.interval = setInterval(function () {
                    updateChart(graph, $graphContainer, readonly);
                }, graph.refresh_rate * 1000);
            }
        } else if (!readonly) {
            createEmptyChart(graph, $graphContainer);
        }
    } else if (!readonly) {
        createEmptyChart(graph, $graphContainer);
    }
}


function saveDashboard(successFunction) {
    const backendData = createDashboardData();
    $.ajax({
        type: 'PUT',
        contentType: 'application/json',
        url: contextRoot + 'dashboard/' + encodeURIComponent(backendData.name),
        cache: false,
        data: JSON.stringify(backendData),
        success: function (data) {
            if (!data) {
                return;
            }
            commons.showNotification('Dashboard \'' + backendData.name + '\' saved.', 'success');
            enableOrDisableButtons();
            if (successFunction) {
                successFunction();
            }
        }
    }).always(function () {
        commons.hideModals([$('#modal-dashboard-overwrite'), $('#modal-graph-settings')]);
    });

    function createDashboardData() {
        const clonedDashboardData = $.extend(true, {}, dashboardData);
        for (let rowIx = 0; rowIx < clonedDashboardData.rows.length; rowIx++) {
            for (let colIx = 0; colIx < clonedDashboardData.rows[rowIx].columns.length; colIx++) {
                delete clonedDashboardData.rows[rowIx].columns[colIx].chart;
                delete clonedDashboardData.rows[rowIx].columns[colIx].chartData;
                delete clonedDashboardData.rows[rowIx].columns[colIx].interval;
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
    const dashboardName = $('#input-dashboard-name').val();
    if (dashboardName && isDashboardExistent(dashboardName)) {
        $('#btn-confirm-remove-dashboard').removeAttr('disabled');
    } else {
        $('#btn-confirm-remove-dashboard').attr('disabled', 'disabled');
    }
}

function updateChart(graph, $container, readonly) {
    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: contextRoot + 'graphdata/' + encodeURIComponent(dashboardData.name) + '/' + encodeURIComponent(graph.id),
        cache: false,
        success: function (response) {
            if (!response) {
                return;
            }
            Highcharts.setOptions({
                lang: {
                    decimalPoint: response.locale.decimal,
                    thousandsSep: response.locale.thousands,
                    timezone: response.locale.timezone
                }
            });
            d3.formatDefaultLocale({
                decimal: response.locale.decimal,
                thousands: response.locale.thousands,
                grouping: [3],
                currency: response.locale.currency
            });
            const chartConfig = response.chart_config;
            chartConfig.title = {text: graph.title ? graph.title : ''};
            if (chartConfig.yAxis) {
                chartConfig.yAxis.labels = {
                    formatter: function () {
                        return formatLabel(response.valueFormat, this.value)
                    }
                };
            }
            if (chartConfig.chart) {
                chartConfig.chart.style = {
                    fontFamily: 'inherit'
                };
            }
            if ('number' === response.type) {
                chartConfig.chart = {
                    events: {
                        load: function () {
                            if (this.textBox) {
                                this.textBox.destroy();
                            }
                            this.textBox = this.renderer.text(formatLabel(response.valueFormat, response.value), this.chartWidth / 2, 'BOTTOM' === chartConfig.vertical_alignment ? this.chartHeight - 10 : this.chartHeight / 2)
                                .css({
                                    fontSize: response.chart_config.font_size,
                                })
                                .attr('text-anchor', 'middle')
                                .add();
                            $container.css('overflow', 'visible');
                            $container.children('.highcharts-container').css('overflow', 'visible');
                        },
                        redraw: function () {
                            this.textBox.attr({
                                x: this.chartWidth / 2,
                                y: 'BOTTOM' === chartConfig.vertical_alignment ? this.chartHeight - 10 : this.chartHeight / 2
                            });
                        }
                    }
                }
            } else {
                chartConfig.chart.events = {
                    load: function () {
                        $container.css('overflow', 'visible');
                        $container.children('.highcharts-container').css('overflow', 'visible');
                    }
                }
            }
            if (chartConfig.tooltip) {
                chartConfig.tooltip.pointFormatter = function () {
                    return '<span style="color:' + this.color + '">\u25CF</span> ' + this.series.name + ': <b>' + formatLabel(response.valueFormat, this.y) + '</b><br/>'
                };
            }
            if (readonly) {
                chartConfig.exporting = {
                    fallbackToExportServer: false,
                    buttons: {
                        contextButton: {
                            menuItems: ['downloadPNG', 'downloadSVG', 'downloadCSV', 'downloadXLS']
                        }
                    }
                }
            } else {
                chartConfig.exporting = {
                    fallbackToExportServer: false,
                    menuItemDefinitions: {
                        editGraph: {
                            onclick: function () {
                                editGraph(graph.id);
                            },
                            text: 'Edit Graph'
                        }
                    },
                    buttons: {
                        contextButton: {
                            menuItems: ['editGraph', 'separator', 'downloadPNG', 'downloadSVG', 'downloadCSV', 'downloadXLS']
                        }
                    }
                }
            }
            if (graph.chart) {
                graph.chart.update(chartConfig);
                if ('number' === response.type) {
                    graph.chart.textBox.attr({
                        text: formatLabel(response.valueFormat, response.value)
                    })
                }
            } else {
                graph.chart = Highcharts.chart($container[0], chartConfig);
            }
            if ($container.closest('.selectedColumn').length === 0) {
                $container.find('.highcharts-exporting-group').addClass('invisible');
            }
        }
    });

    function formatLabel(labelFormat, labelValue) {
        if (labelFormat) {
            try {
                const format = d3.format(labelFormat);
                return format(labelValue);
            } catch (err) {
                console.log(err);
            }
        }
        return labelValue;
    }
}

function createColumnSettingsRow(rowData) {
    const $row = $('<div>').addClass('row fieldConfigurationRow').attr('style', 'margin-top: 5px;');
    $row.append(
        $('<div>').addClass('col-sm-5').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append(
            $('<input>').attr('type', 'number').attr('min', '1').attr('max', '12').attr('name', 'row-cols').addClass('form-control form-control-sm').val(1)
        ),
        $('<div>').addClass('col-sm-5').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append(
            $('<input>').attr('type', 'number').attr('min', '1').attr('max', '50').attr('name', 'row-height').addClass('form-control form-control-sm').val(16)
        )
    );
    const $actionDiv = $('<div>').addClass('col-sm-2').append(
        $('<a href="#">').attr('data-row-action', 'row-remove').addClass('fa fa-times text-danger')
    );
    $row.append($actionDiv);
    if (rowData) {
        $row.attr('data-row-id', rowData.id);
        $row.children().each(function (index, child) {
            if (0 === index) {
                $(child).find('input').val(rowData.columns ? rowData.columns.length : 0);
            } else if (1 === index) {
                $(child).find('input').val(rowData.height);
            }
        });
    }
    return $row;
}

function applyDashboardSettings(currentDashboard) {
    const newDashboardData = {
        changed: false
    };
    if (!currentDashboard) {
        newDashboardData.changed = true;
    }
    newDashboardData.name = $('#input-dashboard-name').val();
    const oldRows = currentDashboard ? currentDashboard.rows : null;
    newDashboardData.rows = [];
    $('#dashboard-settings-columns > div.fieldConfigurationRow').each(function (rowIx, row) {
        const $row = $(row);
        const nrOfCols = Number($row.find("input[name='row-cols']").val());
        const height = Number($row.find("input[name='row-height']").val());
        let oldRow;
        const rowId = $row.attr('data-row-id');
        if (oldRows && rowId) {
            // find the row that is edited.
            oldRow = $.grep(oldRows, function (n, i) {
                return n.id === rowId;
            })[0];
        }
        const jsonRow = {
            id: commons.generateUUID(),
            height: height,
            columns: []
        };
        if (oldRow) {
            jsonRow.id = oldRow.id;
            jsonRow.height = height;
        }
        if (oldRow && oldRow.height !== jsonRow.height) {
            newDashboardData.changed = true;
        }
        if (oldRow && oldRow.columns && oldRow.columns.length === nrOfCols) {
            // Number of columns in row not changed.
            jsonRow.columns = oldRow.columns;
        } else {
            newDashboardData.changed = true;
            let remainingParts = maxParts;
            for (let i = 0; i < nrOfCols; i++) {
                const parts = Math.ceil(remainingParts / (nrOfCols - i));
                let column = {
                    id: commons.generateUUID(),
                    width: parts,
                };
                if (oldRow && oldRow.columns && oldRow.columns[i]) {
                    column = oldRow.columns[i];
                    column.width = parts;
                }
                jsonRow.columns.push(column);
                remainingParts -= parts;
            }
        }
        newDashboardData.rows.push(jsonRow);
    });
    if (!newDashboardData.changed) {
        if (oldRows && oldRows.length !== newDashboardData.rows.length) {
            newDashboardData.changed = true;
        }
        if (currentDashboard.name !== newDashboardData.name) {
            newDashboardData.changed = true;
        }

    }
    return newDashboardData;
}

function removeDashboard(dashboardName) {
    $.ajax({
        type: 'DELETE',
        contentType: 'application/json',
        url: contextRoot + 'dashboard/' + encodeURIComponent(dashboardName),
        cache: false,
        success: function (data) {
            if (!data) {
                return;
            }
            commons.showNotification('Dashboard \'' + dashboardName + '\' removed.', 'success');
            enableOrDisableButtons();
        }
    }).always(function () {
        // TODO remove from menu.
        dashboardData = null;
        commons.hideModals([$('#modal-dashboard-remove'), $('#modal-graph-settings')]);
        showSettings();
    });
}