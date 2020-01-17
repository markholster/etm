"use strict";
let eventEndpointsChart;
let eventChainChart;
let transactionMap = {};
let eventMap = {};
let clipboards = [];

function showEvent(scrollTo, id) {
	$('#search-container').hide();
	$('#event-tabs').children().slice(1).remove();
	$('#tabcontents').children().slice(1).remove();
	$('#event-tab').empty();
	$('#event-tabs a:first').tab('show');
	initialize();

	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
        url: '../rest/search/event/' + encodeURIComponent(id),
	    cache: false,
	    success: function(data) {
	        if (!data || !data.event) {
	            return;
	        }
	        addContent(data);
	    }
	});
	
	$('#btn-back-to-results, #link-back-to-results').attr('data-scroll-to', scrollTo);
	
	$('#event-container').show();

    $('#btn-download-transaction').on('click', function (event) {
        event.preventDefault();
        $('#modal-download-transaction').modal('hide');
        const q = {
            fileType: $('#sel-download-transaction-type').val()
        };
		window.location.href = '../rest/search/download/transaction/' + encodeURIComponent($('#input-download-transaction-id').val()) + '?q=' + encodeURIComponent(JSON.stringify(q));
    });

	function initialize() {
		if (eventEndpointsChart) {
			eventEndpointsChart.destroy();
			eventEndpointsChart = null;
		}
		if (eventChainChart) {
			eventChainChart.destroy();
			eventChainChart = null;
		}
		$.map(transactionMap, function (value) {
			return [value];
		}).forEach(function (element) {
			$(element).remove();
		});
		transactionMap = {};
		eventMap = {};
		$.each(clipboards, function (index, clipboard) {
			clipboard.destroy();
		});
		clipboards = [];
	}

	function addContent(data) {
        const $cardTitle = $('#event-card-title').text('Event ' + data.event.id);
        const $eventTabHeader = $('#event-tab-header').text(capitalize(data.event.source.type));
        const $eventTab = $('#event-tab');
		if (data.event.source) {
			if (data.event.source.name) {
                $cardTitle.text(data.event.source.name);
			}
            writeEventDataToTab($eventTab, $eventTabHeader, data.event, data.time_zone);
			if (data.correlated_events) {
				$.each(data.correlated_events, function(index, correlated_event) {
					$('#event-tabs').children().eq(0).after(
						$('<li>').addClass('nav-item').append(
							$('<a>').attr('id',  'correlation-header-' + index)
								.attr('data-toggle', 'tab')
								.attr('href', '#' + 'correlation-' + index)
								.attr('role', 'tab')
								.attr('aria-controls', 'correlation-' + index)
								.attr('aria-selected', 'false')
								.addClass('nav-link')
								.text('Correlating event ' + index)
						)
					);
					$('#tabcontents').children().eq(0).after(
						$('<div>').attr('id', 'correlation-' + index)
							.attr('role', 'tabpanel')
							.attr('aria-labelledby', 'correlation-header' + index)
							.addClass('tab-pane fade pt-3')
					);
					writeEventDataToTab($('#correlation-' + index), $('#correlation-header-' + index), correlated_event, data.time_zone);
				}); 
				
			}
            const $endpoints = $(data.event.source.endpoints);
			if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
				createEndpointsTab(data.event.id);
				// Check if a transaction id is present
				let hasTransactionId = false;
				$.each($endpoints, function (index, endpoint) {
					const writingEndpointHandler = getWritingEndpointHandler(endpoint.endpoint_handlers);
					if (writingEndpointHandler && writingEndpointHandler.transaction_id) {
						hasTransactionId = true;
						return false;
					}
					const readingEndpointHandlers = getReadingEndpointHandlers(endpoint.endpoint_handlers);
					if (readingEndpointHandlers) {
						$.each(readingEndpointHandlers, function (index, eh) {
							if (eh.transaction_id) {
								hasTransactionId = true;
								return false;
							}
						});
					}
					if (hasTransactionId) {
						return false;
					}
				});
				if (hasTransactionId) {
                    createEventChainTab(data.event.id);
				}
			}
			if (data.audit_logs) {
				createAuditLogsTab(data.audit_logs, data.time_zone);
			}
		}
	}
	
	function writeEventDataToTab(tab, tabHeader, data, timeZone) {
        const $eventTab = $(tab);
        const $tabHeader = $(tabHeader);
        const dataLink = $('<a>')
			.text(data.id)
			.addClass('form-control-static')
            .attr('href', '?id=' + encodeURIComponent(data.id))
			.attr('style', 'display: inline-block;')
			.attr('data-link-type', 'show-event')
			.attr('data-scroll-to', scrollTo)
			.attr('data-event-id', data.id);
		appendElementToContainerInRow($eventTab, 'Id', dataLink);
		appendToContainerInRow($eventTab, 'Name', data.source.name);
		if (data.source.correlation_id) {
            const correlationDataLink = $('<a>')
				.text(data.source.correlation_id)
				.addClass('form-control-static')
                .attr('href', '?id=' + encodeURIComponent(data.source.correlation_id))
				.attr('style', 'display: inline-block;')
				.attr('data-link-type', 'show-event')
				.attr('data-scroll-to', scrollTo)
				.attr('data-event-id', data.source.correlation_id);
            appendElementToContainerInRow($eventTab, 'Correlation id', correlationDataLink);
		}
		
		appendToContainerInRow($eventTab, 'Payload format', data.source.payload_format);
        const $endpoints = $(data.source.endpoints);
		if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
            const writingTimes = $endpoints.map(function () {
                const writingEndpointHandler = getWritingEndpointHandler(this.endpoint_handlers);
				if (writingEndpointHandler) {
					return writingEndpointHandler.handling_time
				}
			}).get();
            if (writingTimes.length !== 0) {
			    appendToContainerInRow($eventTab, 'First write time', moment.tz(Math.min.apply(Math, writingTimes), timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
			}
            const readingTimes = $endpoints.map(function () {
                const readingEndpointHandlers = getReadingEndpointHandlers(this.endpoint_handlers);
                const times = $(readingEndpointHandlers).map(function () {
                    return this.handling_time;
                }).get();
                if (times.length !== 0) {
                    return Math.min.apply(Math, times);
                }
            }).get();
            if (readingTimes.length !== 0) {
                appendToContainerInRow($eventTab, 'First read time', moment.tz(Math.min.apply(Math, readingTimes), timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
            }
		}
        if ('log' === data.source.object_type) {
		    $tabHeader.text('Log');
			appendToContainerInRow($eventTab, 'Log level', data.source.log_level);
        } else if ('http' === data.source.object_type) {
			appendToContainerInRow($eventTab, 'Http type', data.source.http_type);
			if (data.source.expiry) {
				appendToContainerInRow($eventTab, 'Expiry time', moment.tz(data.source.expiry, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
			}
			appendToContainerInRow($eventTab, 'Status code', data.source.status_code);
			if (data.source.http_type) {
				// http type known, determine request or response.
				if ('RESPONSE' === data.source.http_type) {
					$tabHeader.text('Http response');
				} else {
					$tabHeader.text('Http request');
					if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
                        let response_times = $endpoints.map(function () {
                            const writingEndpointHandler = getWritingEndpointHandler(this.endpoint_handlers);
                            if (writingEndpointHandler) {
                                return writingEndpointHandler.response_time
                            }
						}).get();
						if (response_times.length > 0) {
							appendToContainerInRow($eventTab, 'Highest writer response time', Math.max.apply(Math, response_times));
						}
						response_times = $endpoints.map(function () {
                            const readingEndpointHandlers = getReadingEndpointHandlers(this.endpoint_handlers);
							if (readingEndpointHandlers) {
								return $(readingEndpointHandlers).map(function () {
									return this.response_time;
								}).get();
							}
						}).get();
						if (response_times.length > 0) {
							appendToContainerInRow($eventTab, 'Highest reader response time', Math.max.apply(Math, response_times));
						}
					}					
				}
			}
        } else if ('messaging' === data.source.object_type) {
			appendToContainerInRow($eventTab, 'Messaging type', data.source.messaging_type);
			if (data.source.expiry) {
				appendToContainerInRow($eventTab, 'Expiry time', moment.tz(data.source.expiry, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
			}
			if (data.source.messaging_type) {
				// messaging type known, determine request or response.
				if ('RESPONSE' === data.source.messaging_type) {
					$tabHeader.text('Messaging response');
				} else if ('REQUEST' === data.source.messaging_type) {
					$tabHeader.text('Messaging request');
					if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
                        let response_times = $endpoints.map(function () {
                            const writingEndpointHandler = getWritingEndpointHandler(this.endpoint_handlers);
                            if (writingEndpointHandler) {
                                return writingEndpointHandler.response_time
                            }
						}).get();
						if (response_times) {
							appendToContainerInRow($eventTab, 'Highest writer response time', response_times.length > 0 ? Math.max.apply(Math, response_times) : null);
						}
						response_times = $endpoints.map(function () {
                            const readingEndpointHandlers = getReadingEndpointHandlers(this.endpoint_handlers);
							if (readingEndpointHandlers) {
								return $(readingEndpointHandlers).map(function () {
									return this.response_time;
								}).get();
							}
						}).get();
						if (response_times) {
							appendToContainerInRow($eventTab, 'Highest reader response time', response_times.length > 0 ? Math.max.apply(Math, response_times) : null);
						}
					}
				} else {
					$tabHeader.text('Messaging fire-forget');
				}
			}
        } else if ('sql' === data.source.object_type) {
			appendToContainerInRow($eventTab, 'Sql type', data.source.sql_type);
			if (data.source.expiry) {
				appendToContainerInRow($eventTab, 'Expiry time', moment.tz(data.source.expiry, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
			}
			if (data.source.sql_type) {
				// sql type known, determine query or result.
				if ('RESULTSET' === data.source.sql_type) {
					$tabHeader.text('Sql result');
				} else {
					$tabHeader.text('Sql query');
					if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
                        let response_times = $endpoints.map(function () {
                            const writingEndpointHandler = getWritingEndpointHandler(this.endpoint_handlers);
                            if (writingEndpointHandler) {
                                return writingEndpointHandler.response_time
                            }
						}).get();
						if (response_times) {
							appendToContainerInRow($eventTab, 'Highest writer response time', response_times.length > 0 ? Math.max.apply(Math, response_times) : null);
						}
						response_times = $endpoints.map(function () {
                            const readingEndpointHandlers = getReadingEndpointHandlers(this.endpoint_handlers);
                            if (readingEndpointHandlers) {
                                return $(readingEndpointHandlers).map(function () {
                                    return this.response_time;
                                }).get();
                            }
						}).get();
						if (response_times) {
							appendToContainerInRow($eventTab, 'Highest reader response time', response_times.length > 0 ? Math.max.apply(Math, response_times) : null);
						}
					}										
				}
            }
        } else if ('business' === data.source.object_type) {
		    $tabHeader.text('Business');
		}
        let metadataDetailMap;
		if ("undefined" != typeof data.source.metadata) {
		    metadataDetailMap = createDetailMap('metadata', data.source.metadata);
	    }
	    // Search for metadata in endpoint handlers
        if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
            $.each($endpoints, function (index, endpoint) {
                const endpointData = {
                    name: endpoint.name ? endpoint.name : null,
                    readers: []
                };
                const writingEndpointHandler = getWritingEndpointHandler(endpoint.endpoint_handlers);
                if (writingEndpointHandler && writingEndpointHandler.metadata) {
                    const writerName = writingEndpointHandler.application && writingEndpointHandler.application.name ? writingEndpointHandler.application.name : 'Writer';
                    endpointData.writer = {
                        name: writerName,
                        metadata: writingEndpointHandler.metadata
                    }
                }
                const readingEndpointHandlers = getReadingEndpointHandlers(endpoint.endpoint_handlers);
                if (readingEndpointHandlers) {
                    $.each(readingEndpointHandlers, function (index, eh) {
                        if (eh.metadata) {
                            const readerName = eh.application && eh.application.name ? eh.application.name : 'Reader';
                            const reader = {
                                name: readerName,
                                metadata: eh.metadata
                            };
                            endpointData.readers.push(reader);
                        }
                    });
                }
                if (endpointData.readers.length > 0 || endpointData.writer) {
                    if(!metadataDetailMap) {
                        metadataDetailMap = createDetailMap('metadata');
                    }
                    appendEndpointToDetailMap(metadataDetailMap, endpointData);
                }
            });
        }
        if (metadataDetailMap) {
            $eventTab.append(metadataDetailMap);
        }

	    if ("undefined" != typeof data.source.extracted_data) {
	    	$eventTab.append(createDetailMap('extracted data', data.source.extracted_data));
	    }
	    
	    $eventTab.append($('<br/>'));
	    if (data.source.payload) {
            const payloadCode = $('<code>').text(indentCode(data.source.payload, data.source.payload_format));
            const $clipboard = $('<a>').attr('href', "#").addClass('small').text('Copy raw payload to clipboard');
            $eventTab.append(
                    $('<div>').addClass('row').attr('style', 'background-color: #eceeef;').append(
                            $('<div>').addClass('col-sm-12').append(
                                    $clipboard,
                                    $('<pre>').attr('style', 'white-space: pre-wrap;').append(
                                            payloadCode
                                    ),
                                    $('<pre>').attr('style', 'display: none').text(data.source.payload)

                            )
                    )
            );
            clipboards.push(new Clipboard($clipboard[0], {
                text: function(trigger) {
                    return $(trigger).next().next().text();
                }
            }));
            if (typeof(Worker) !== "undefined" && data.source.payload_length <= 1048576) {
                // Only highlight for payload ~< 1 MiB
                const worker = new Worker('../scripts/highlight-worker.js');
                worker.onmessage = function(result) {
                    payloadCode.html(result.data);
                };
                worker.postMessage([payloadCode.text(), data.source.payload_format]);
            }
	    }
        if (('log' === data.source.object_type) && "undefined" != typeof data.source.stack_trace) {
	    	$eventTab.append(
	    	    $('<br />'),
	    		$('<div>').addClass('row').append(
	    				$('<div>').addClass('col-sm-12').append(
	    						$('<pre>').attr('style', 'white-space: pre-wrap;').append($('<code>').text(data.source.stack_trace))
	    				)
	    		)
			);
	    }
	    
	    function indentCode(code, format) {
	        try {
                if ('HTML' === format || 'SOAP' === format || 'XML' === format) {
                    return vkbeautify.xml(code, 4);
                } else if ('SQL' === format) {
                    return vkbeautify.sql(code, 4);
                } else if ('JSON' === format) {
                    return vkbeautify.json(code, 4);
                }
            } catch (err) {}
	    	return code;
	    }
	}
		
	function appendToContainerInRow(container, name, value) {
	    if ('undefined' != typeof value) {
		    appendElementToContainerInRow(container, name, $('<p>').addClass('form-control-static').text(value));
		}
	}
	
	function appendElementToContainerInRow(container, name, element) {
        const $container = $(container);
        const $row = $container.children(":last-child");
		if ($row.children().length > 0 && $row.children().length <= 2) {
			$row.append(
			    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold form-control-static').text(name)),
			    $('<div>').addClass('col-md-4').attr('style', 'word-wrap: break-word;').append(element)
			);
		} else {
			$container.append(
					$('<div>').addClass('row')
						.append(
						    $('<div>').addClass('col-md-2').append($('<label>').addClass('font-weight-bold form-control-static').text(name)),
						    $('<div>').addClass('col-md-4').attr('style', 'word-wrap: break-word;').append(element)
					)
			);
		}
	}
	
	function createDetailMap(name, valueMap) {
        const panelId = commons.generateUUID();
        const responsiveTableDiv = $('<div>').addClass('table-responsive');
		if (valueMap) {
		    appendTableToDetailMap(responsiveTableDiv, valueMap);
		}
        return $('<div>').addClass('panel panel-default').append(
				$('<div>').addClass('panel-heading clearfix').append(
						$('<div>').addClass('pull-left').append(
							$('<a>')
								.addClass('font-weight-bold')
								.attr('href', '#')
								.attr('data-link-type', 'toggle-detail-map')
								.attr('data-panel-id', panelId)
								.text(capitalize(name))
						)
				),
				$('<div>').attr('id', panelId).addClass('panel-collapse collapse').append(
				    $('<div>').addClass('panel-body').append(responsiveTableDiv)
				)
		);
	}

	function appendEndpointToDetailMap(metadataDetailMap, endpointData) {
        const responsiveTableDiv = $(metadataDetailMap).find('.table-responsive');
	    if (endpointData.writer) {
	        appendTableToDetailMap(responsiveTableDiv, endpointData.writer.metadata, endpointData.name == null ? endpointData.writer.name : endpointData.name + ' - ' + endpointData.writer.name);
	    }
	    if (endpointData.readers.length > 0) {
	        $.each(endpointData.readers, function (index, reader) {
	            appendTableToDetailMap(responsiveTableDiv, reader.metadata, endpointData.name == null ? reader.name : endpointData.name + ' - ' + reader.name);
            });
	    }

	}

	function appendTableToDetailMap(responsiveTableDiv, valueMap, caption) {
        const $table = $('<table>').addClass('table table-sm table-striped table-hover');
        if (caption) {
            $table.append(
                $('<caption>').attr('style', 'caption-side: top;').text(caption)
            );
        }
        $table.append(
            $('<thead>').append(
                $('<tr>').append(
                    $('<th>').attr('style', 'padding: 0.1rem;').text('Name')
                ).append(
                    $('<th>').attr('style', 'padding: 0.1rem;').text('Value')
                )
            )
        ).append(function () {
            const $tbody = $('<tbody>');
            const detailItems = [];
            $.each(valueMap, function(key, value) {
                if (endsWith(key, '_as_number') || endsWith(key, '_as_boolean') || endsWith(key, '_as_date')) {
                    return true;
                }
                detailItems.push( { key: key, value: value } );
            });
            detailItems.sort(function(item1,item2){ return item1.key.localeCompare(item2.key);});
            $.each(detailItems, function(index, item) {
				$tbody.append(
					$('<tr>').append(
						$('<td>').attr('style', 'padding: 0.1rem;').text(item.key),
						$('<td>').attr('style', 'padding: 0.1rem;').text(item.value)
					)
				);
			});
			return $tbody;
		});
		$(responsiveTableDiv).append($table);
	}

	function createEndpointsTab(id) {
		$('#event-tabs').append(
			$('<li>').addClass('nav-item').append(
				$('<a>').attr('id', 'endpoint-tab-header')
					.attr('data-toggle', 'tab')
					.attr('href', '#endpoint-tab')
					.attr('role', 'tab')
					.attr('aria-controls', 'endpoint-tab')
					.attr('aria-selected', 'false')
					.addClass('nav-link')
					.text('Endpoints')
			)
		);

		$('#tabcontents').append(
			$('<div>').attr('id', 'endpoint-tab')
				.attr('role', 'tabpanel')
				.attr('aria-labelledby', 'endpoint-tab-header')
				.addClass('tab-pane fade pt-3')
				.append(
					$('<div>').addClass('row').append(
						$('<div>').attr('id', 'endpoint-overview').attr('style', 'height: 30em; width: 100%; resize: vertical;')
					),
					$('<br />'),
					$('<div>').attr('id', 'endpoint-node-detail').append(
						$('<div>').addClass('row').append(
							$('<div>').addClass('col-sm-12').append(
								$('<p>').addClass('text-center').text('Click on a node to view more details')
							)
						)
					),
					$('<div>').attr('id', 'endpoint-node-transaction-detail')
				)
		);
		$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
			const target = $(e.target).attr("href"); // activated tab
			if (target === '#endpoint-tab' && !$('#endpoint-overview > div > svg').length) {
				$.ajax({
					type: 'GET',
					contentType: 'application/json',
					url: '../rest/search/event/' + encodeURIComponent(id) + '/dag',
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
							currency: response.locale.currency
						});

						eventEndpointsChart = Highcharts.chart('endpoint-overview', {
							credits: {enabled: false},
							exporting: {
								sourceWidth: 1600,
								fallbackToExportServer: false,
								buttons: {
									contextButton: {
										menuItems: ['downloadPNG', 'downloadSVG']
									}
								}
							},
							chart: {
								backgroundColor: 'white',
								height: '100%',
								panning: true,
								events: {
									load: function () {
										const ren = this.renderer, colors = Highcharts.getOptions().colors;

										let bbox = ren.boxWrapper.getBBox();
										const mainGroup = ren.g('mainGroup').attr('text-anchor', 'middle').add();

										const yTop = 75;
										const yLevelHeight = 150;

										const lineHeight = 12;
										const endpointSize = lineHeight * 3;
										const halfEndpointSize = endpointSize / 2;
										const eventSize = lineHeight * 2;
										const halfEventSize = eventSize / 2;
										const flowOffset = 5;

										const calculatedPositions = {};

										$.each(response.layers, function (layerIx, layer) {
											const nrOfVertices = layer.vertices.length;
											const xUnits = bbox.width / (nrOfVertices + 1);

											$.each(layer.vertices, function (vertexIx, vertex) {
												if ('endpoint' === vertex.type) {
													const xPos = xUnits * (vertexIx + 1);
													const yPos = yTop + (layerIx * yLevelHeight);
													const vertexGroup = ren.g().addClass('endpoint').translate(xPos, yPos).add(mainGroup);
													let image;
													if ('HTTPS' === vertex.protocol) {
														image = '../images/chain/https.svg';
													} else if ('HTTP' === vertex.protocol) {
														image = '../images/chain/http.svg';
													} else if ('MQ' === vertex.protocol) {
														image = '../images/chain/queue.svg';
													} else if ('KAFKA' === vertex.protocol) {
														image = '../images/chain/kafka.svg';
													}
													ren.image(image, -halfEndpointSize, 0, endpointSize, endpointSize)
														.attr({
															'data-event-id': vertex.event_id,
															'data-endpoint': vertex.name,
															'style': 'cursor: pointer;'
														})
														.addClass('etm-gen-drawing-endpoint')
														.add(vertexGroup);
													ren.text(vertex.name, 0, endpointSize + lineHeight).add(vertexGroup);
													calculatedPositions[vertex.vertex_id] = {
														x: xPos,
														y: yPos + halfEndpointSize,
														r: halfEndpointSize
													};
												} else if ('application' === vertex.type) {
													const xPos = (xUnits * (vertexIx + 1)) - xUnits / 2;
													const yPos = yTop + (layerIx * yLevelHeight) - (yLevelHeight / 3);
													const applicationGroup = ren.g().addClass('application').translate(xPos, yPos).add(mainGroup);
													ren.rect(0, 0, xUnits, yLevelHeight, lineHeight).attr('fill', colors[7]).attr('fill-opacity', '0.25').attr('stroke-width', 3).attr('stroke', colors[7]).add(applicationGroup);
													const appName = vertex.instance ? vertex.name + ' (' + vertex.instance + ')' : vertex.name;
													ren.text(appName, lineHeight * .75, lineHeight * 1.5).attr('text-anchor', 'start').attr('font-weight', 'bold').add(applicationGroup);
													const childWith = xUnits / vertex.children.length;
													const yOffset = yLevelHeight / 2 + halfEventSize;
													$.each(vertex.children, function (childIx, childVertex) {
														const itemXPos = childWith * (childIx + .5);
														const itemYPos = yOffset - eventSize - halfEventSize;
														ren.image('../images/chain/message.svg', -halfEventSize + itemXPos, itemYPos, eventSize, eventSize)
															.addClass('etm-gen-drawing-message')
															.attr({
																'data-event-id': childVertex.event_id,
																'data-transaction-id': childVertex.transaction_id,
																'data-endpoint': childVertex.endpoint,
																'data-name': childVertex.name,
																'data-time': childVertex.event_start_time,
																'data-end-time': childVertex.event_end_time ? childVertex.event_end_time : false,
																'style': 'cursor: pointer;'
															})
															.add(applicationGroup);
														ren.text(childVertex.name, childWith * (childIx + .5), yOffset).add(applicationGroup);
														calculatedPositions[childVertex.vertex_id] = {
															x: xPos + itemXPos,
															y: yPos + itemYPos + halfEventSize,
															r: halfEventSize
														};
													});
												} else if ('event' === vertex.type) {
													const xPos = xUnits * (vertexIx + 1);
													const yPos = yTop + (layerIx * yLevelHeight);
													const vertexGroup = ren.g().addClass('event').translate(xPos, yPos).add(mainGroup);
													ren.image('../images/chain/message.svg', -halfEndpointSize, 0, endpointSize, endpointSize)
														.addClass('etm-gen-drawing-message')
														.attr({
															'data-event-id': vertex.event_id,
															'data-transaction-id': vertex.transaction_id,
															'data-endpoint': vertex.endpoint,
															'data-name': vertex.name,
															'data-time': vertex.event_start_time,
															'data-end-time': vertex.event_end_time ? vertex.event_end_time : false,
															'style': 'cursor: pointer;'
														})
														.add(vertexGroup);
													ren.text(vertex.name, 0, endpointSize + lineHeight).add(vertexGroup);
													calculatedPositions[vertex.vertex_id] = {
														x: xPos,
														y: yPos + halfEventSize,
														r: halfEndpointSize
													};
												}
											});
										});

										$.each(response.edges, function (edgeIx, edge) {
											const from = calculatedPositions[edge.from];
											const to = calculatedPositions[edge.to];
											if (from === undefined || to === undefined) {
												return;
											}
											let fromX = from.x;
											let fromY = from.y;
											let toX = to.x;
											let toY = to.y;
											if (from.y < to.y) {
												// From top to bottom.
												fromY += from.r + lineHeight + flowOffset;
												toY -= (to.r + flowOffset);
											} else if (from.y > to.y) {
												// From bottom to top.
												fromY -= (from.r + flowOffset);
												toY += to.r + lineHeight + flowOffset;
											} else if (from.x < to.x) {
												// From left to right.
												fromX += from.r + flowOffset;
												toX -= (to.r + flowOffset)
											} else if (from.x > to.x) {
												// From right to left.
												fromX -= (from.r + flowOffset);
												toX += to.r + flowOffset;
											}
											let color = colors[1];
											if (edge.transition_time_percentage) {
												const labelPosX = fromX + ((toX - fromX) / 2);
												const labelPosy = fromY + ((toY - fromY) / 2) - lineHeight;
												const percentage = (Number(edge.transition_time_percentage) * 100).toFixed(2) + '%';
												ren.text(percentage, labelPosX, labelPosy)
													.attr({
														'data-percentage': percentage,
														'data-duration': edge.transition_time
													})
													.addClass('etm-gen-drawing-edge')
													.add(mainGroup);
												const redColor = Math.round(255 * Number(edge.transition_time_percentage));
												color = '#' + (redColor < 10 ? ('0' + redColor.toString(16)) : (redColor.toString(16))) + '0000';
											}
											ren.path(['M', fromX, fromY, 'C', fromX, toY, toX, fromY, toX, toY])
												.attr({'stroke-width': 2, stroke: color})
												.addClass('moving-path')
												.add(mainGroup);

										});
									}
								}
							},
							title: {
								text: '',
							}
						});
						svgPanZoom($('#endpoint-overview > div > svg')[0], {
							viewportSelector: '.highcharts-mainGroup',
							panEnabled: true,
							zoomEnabled: true
						});
						// Fix some attributes to resizing work in chrome. Resize isn't working when a svg displayed behind the resize icon.
						$('#endpoint-overview .highcharts-container').css('pointer-events', 'none');
						$('#endpoint-overview .highcharts-mainGroup, #endpoint-overview .highcharts-exporting-group, #endpoint-overview .highcharts-background').css('pointer-events', 'auto');
						const $hcbg = $('#endpoint-overview .highcharts-background');
						$hcbg.attr('width', Number($hcbg.attr('width')) - 10);

						$('.etm-gen-drawing-edge').tooltip({
							placement: 'auto',
							html: true,
							title: function () {
								return 'Duration: ' + $(this).attr('data-duration') + 'ms<br />Percentage of total: ' + $(this).attr('data-percentage');
							}
						});
						$('.etm-gen-drawing-message').on('click', function () {
							const eventData = loadEventData($(this).attr('data-event-id'));
							if ("" === eventData) {
								return;
							}
							const endpointName = $(this).attr('data-endpoint');
							const transactionId = $(this).attr('data-transaction-id');
							$.each(eventData.source.endpoints, function (index, endpoint) {
								if (endpointName === endpoint.name) {
									const writingEndpointHandler = getWritingEndpointHandler(endpoint.endpoint_handlers);
									if (writingEndpointHandler && transactionId === writingEndpointHandler.transaction_id) {
										displayWritingEndpointHandler('endpoint-node-detail', 'endpoint-node-transaction-detail', writingEndpointHandler, response.locale.timezone, endpoint.name);
										return false;
									}
									const readingEndpointHandlers = getReadingEndpointHandlers(endpoint.endpoint_handlers);
									if (readingEndpointHandlers) {
										$.each(readingEndpointHandlers, function (index, eh) {
											if (transactionId === eh.transaction_id || readingEndpointHandlers.length === 0) {
												displayReadingEndpointHandler('endpoint-node-detail', 'endpoint-node-transaction-detail', eh, response.locale.timezone, endpoint.name);
												return false;
											}
										});
									}
								}
							});
						}).tooltip({
							placement: 'auto',
							html: true,
							title: function () {
								const startTime = Number($(this).attr('data-time'));
								let text = 'Name: ' + $(this).attr('data-name') + '<br />Event time: ' + moment.tz(startTime, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ');
								if ('false' !== $(this).attr('data-end-time')) {
									const endTime = Number($(this).attr('data-end-time'));
									text += '<br />Response time: ' + (endTime - startTime) + 'ms';
								}
								return text;
							}
						});

						$('.etm-gen-drawing-endpoint').on('click', function () {
							const eventData = loadEventData($(this).attr('data-event-id'));
							if ("" === eventData) {
								return;
							}
							const endpointName = $(this).attr('data-endpoint');
							$.each(eventData.source.endpoints, function (index, endpoint) {
								if (endpointName === endpoint.name) {
									displayEndpoint('endpoint-node-detail', 'endpoint-node-transaction-detail', endpoint, response.locale.timezone);
									return false;
								}
							});
						});
					}
				});

			}
		});
	}

	function loadEventData(eventId) {
		let eventData = eventMap[eventId];
		if ("undefined" == typeof eventData) {
			$.ajax({
				type: 'GET',
				contentType: 'application/json',
				async: false,
				url: '../rest/search/event/' + encodeURIComponent(eventId) + '/endpoints',
				cache: false,
				success: function (data) {
					if (!data || !data.event || !data.event.source) {
						eventMap[eventId] = "";
						return;
					}
					eventMap[eventId] = eventData = data.event;
				}
			});
		}
		return eventData;
	}

	function displayEndpoint(nodeDetailContainerId, transactionDetailContainerId, endpoint, timeZone) {
		$('#' + nodeDetailContainerId).fadeOut('fast', function () {
			$(this).empty();
			if (endpoint) {
				const writingEndpointHandler = getWritingEndpointHandler(endpoint.endpoint_handlers);
				if (writingEndpointHandler && writingEndpointHandler.handling_time) {
					appendToContainerInRow($(this), 'Write time', moment.tz(writingEndpointHandler.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
				}
				const readingEndpointHandlers = getReadingEndpointHandlers(endpoint.endpoint_handlers);
				const times = $(readingEndpointHandlers).map(function () {
					return this.handling_time;
				}).get();
				if (times.length !== 0) {
					appendToContainerInRow($(this), 'First read time', moment.tz(Math.min.apply(Math, times), timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
				}
			}
			appendToContainerInRow($(this), 'Endpoint name', endpoint.name);
			$(this).append('<br>');
			$(this).fadeIn('fast');
		});
		$('#' + transactionDetailContainerId).fadeOut('fast', function () {
			$('#transaction-detail-table').detach();
			$(this).empty();
		});
	}

	function displayWritingEndpointHandler(nodeDetailContainerId, transactionDetailContainerId, endpoint_handler, timeZone, endpointName) {
		const $transactionDetails = $('#' + transactionDetailContainerId);
		$transactionDetails.fadeOut('fast', function () {
			$('#transaction-detail-table').detach();
			$(this).empty();
		});
		$('#' + nodeDetailContainerId).fadeOut('fast', function () {
			$(this).empty();
			const eh = formatEndpointHandler(endpoint_handler, timeZone);
			if (endpointName) {
				appendToContainerInRow($(this), 'Endpoint', endpointName);
			}
			appendToContainerInRow($(this), 'Write time', eh.handling_time);
			appendToContainerInRow($(this), 'Response time', eh.response_time);
			appendToContainerInRow($(this), 'Transaction id', eh.transaction_id);
			appendToContainerInRow($(this), 'Location', eh.location);
			appendToContainerInRow($(this), 'Application name', eh.application_name);
			appendToContainerInRow($(this), 'Application version', eh.application_version);
			appendToContainerInRow($(this), 'Application instance', eh.application_instance);
			appendToContainerInRow($(this), 'Application user', eh.application_principal);
			appendToContainerInRow($(this), 'Application address', eh.application_host);
            if (eh.transaction_id) {
                displayTransactionDetails($transactionDetails, eh.transaction_id);
				$transactionDetails.append('<br>');
			} else {
				$(this).append('<br>');
			}
			$(this).fadeIn('fast');
			$transactionDetails.fadeIn('fast');
		});
	}
	
    function displayReadingEndpointHandler(nodeDetailContainerId, transactionDetailContainerId, endpoint_handler, timeZone, endpointName) {
		const $transactionDetails = $('#' + transactionDetailContainerId);
		$transactionDetails.fadeOut('fast', function () {
			$('#transaction-detail-table').detach();
			$(this).empty();
		});
		$('#' + nodeDetailContainerId).fadeOut('fast', function () {
			$(this).empty();
			const eh = formatEndpointHandler(endpoint_handler, timeZone);
			if (endpointName) {
				appendToContainerInRow($(this), 'Endpoint', endpointName);
			}
			appendToContainerInRow($(this), 'Read time', eh.handling_time);
			appendToContainerInRow($(this), 'Response time', eh.response_time);
			appendToContainerInRow($(this), 'Transaction id', eh.transaction_id);
			appendToContainerInRow($(this), 'Location', eh.location);
			appendToContainerInRow($(this), 'Application name', eh.application_name);
			appendToContainerInRow($(this), 'Application version', eh.application_version);
			appendToContainerInRow($(this), 'Application instance', eh.application_instance);
			appendToContainerInRow($(this), 'Application user', eh.application_principal);
			appendToContainerInRow($(this), 'Application address', eh.application_host);
			appendToContainerInRow($(this), 'Latency', eh.latency);
            if (eh.transaction_id) {
                displayTransactionDetails($transactionDetails, eh.transaction_id);
				$transactionDetails.append('<br>');
			} else {
				$(this).append('<br>');
			}
			$(this).fadeIn('fast');
			$transactionDetails.fadeIn('fast');
		});
	}

    function displayTransactionDetails(container, transactionId) {
        const $container = $(container);
        let $transactionTable = transactionMap[transactionId];
        $('#input-download-transaction-id').val(transactionId);
		if ("undefined" != typeof $transactionTable) {
			$container.append($transactionTable);
		} else {
			$.ajax({
			    type: 'GET',
			    contentType: 'application/json',
                url: '../rest/search/transaction/' + encodeURIComponent(transactionId),
			    cache: false,
			    success: function(transaction_data) {
			        if (!transaction_data || !transaction_data.events) {
			            return;
			        }
			        $transactionTable = $('<table id="transaction-detail-table">').addClass('table table-hover table-sm').append($('<caption>').attr('style', 'caption-side: top;').text('Transaction ' + transactionId)).append(
			        	$('<thead>').append(
			        		$('<tr>').append(
			        			$('<th>').attr('style' ,'padding: 0.1rem;').text('Id'),
			        			$('<th>').attr('style' ,'padding: 0.1rem;').text('Date'),
			        			$('<th>').attr('style' ,'padding: 0.1rem;').text('Type'),
			        			$('<th>').attr('style' ,'padding: 0.1rem;').append(
                                    $('<a>').attr('href', "#").addClass('fa fa-download float-right').attr('title', 'Download transaction')
			        			    .on('click', function(event) {
			        			        event.preventDefault();
			        			        $('#modal-download-transaction').modal();
			        			    })
			        			)
			        		)
			        	)
			        ).append(function () {
                        const $tbody = $('<tbody>');
			        	$.each(transaction_data.events, function(index, event) {
                            const $link = $('<a>')
                                .attr('href', '?id=' + encodeURIComponent(event.id))
								.text(event.id)
								.attr('data-link-type', 'show-event')
								.attr('data-scroll-to', scrollTo)
								.attr('data-event-id', event.id);
			        		$tbody.append(
			        			$('<tr>').append(
                                    event.id === id ? $('<td>').attr('style', 'padding: 0.1rem;').text(event.id) : $('<td>').attr('style', 'padding: 0.1rem;').append($link),
			        				$('<td>').attr('style' ,'padding: 0.1rem;').text(moment.tz(event.handling_time, transaction_data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ')),
                                    $('<td>').attr('style', 'padding: 0.1rem;').text('sql' === event.object_type ? 'SQL' : capitalize(event.object_type)),
			        				$('<td>').attr('style' ,'padding: 0.1rem;').text(formatTransactionLine(event))
			        			)
			        		);	
			        	});
			        	return $tbody;
                    });
			        transactionMap[transactionId] = $transactionTable;
			        $container.append($transactionTable);
			    }
			});			
		}
	}
	
	function formatTransactionLine(event) {
        if ('business' === event.object_type) {
			return event.name ? event.name : '?';
        } else if ('http' === event.object_type) {
            return ('incoming' === event.direction ? 'Received ' : 'Sent ') + ("RESPONSE" === event.sub_type ? 'http response ' : 'http ') + (event.name ? event.name : '?')
        } else if ('log' === event.object_type) {
			return event.payload;
        } else if ('messaging' === event.object_type) {
            if ('REQUEST' === event.sub_type) {
                if ('incoming' === event.direction) {
					return 'Received request message ' + (event.name ? event.name : '?') + ' from ' + event.endpoint;
				} else {
					return 'Sent request message ' + (event.name ? event.name : '?') + ' to ' + event.endpoint;
				}
            } else if ('RESPONSE' === event.sub_type) {
                if ('incoming' === event.direction) {
					return 'Received response message ' + (event.name ? event.name : '?') + ' from ' + event.endpoint;
				} else {
					return 'Sent response message ' + (event.name ? event.name : '?') + ' to ' + event.endpoint;
				}
			} else {
                if ('incoming' === event.direction) {
					return 'Received fire-forget message ' + (event.name ? event.name : '?') + ' from ' + event.endpoint;
				} else {
					return 'Sent fire-forget message ' + (event.name ? event.name : '?') + ' to ' + event.endpoint;
				}
			}
        } else if ('sql' === event.object_type) {
            if ('incoming' === event.direction) {
                return 'Received ' + ("RESULTSET" === event.sub_type ? 'sql resultset' : event.payload);
			} else {
                return 'Sent ' + ("RESULTSET" === event.sub_type ? 'sql resultset' : event.payload);
			}
		}
	}
	
	function formatEndpointHandler(endpoint_handler, timeZone) {
        const flat = {
			handling_time: undefined,
			latency: endpoint_handler.latency,
			response_time: endpoint_handler.response_time,
			transaction_id: endpoint_handler.transaction_id,
			application_name: undefined,
			application_version: undefined,
			application_instance: undefined,
			application_principal: undefined,
			application_host: undefined,
			location: undefined,
		};
		if (endpoint_handler.handling_time) {
			flat.handling_time = moment.tz(endpoint_handler.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ');
		}
		if (endpoint_handler.application) {
			flat.application_name = endpoint_handler.application.name; 
			flat.application_version = endpoint_handler.application.version; 
			flat.application_instance = endpoint_handler.application.instance; 
			flat.application_principal = endpoint_handler.application.principal;
			if (endpoint_handler.application.host_name) {
				flat.application_host = endpoint_handler.application.host_name;
				if (endpoint_handler.application.host_address && endpoint_handler.application.host_address !== endpoint_handler.application.host_name) {
					flat.application_host += ' (' + endpoint_handler.application.host_address + ')'
				}
			} else {
				flat.application_host = endpoint_handler.application.host_address;
			}
		}
		if (endpoint_handler.location) {
			flat.location = convertDDToDMS(endpoint_handler.location.lat, false) + ' ' + convertDDToDMS(endpoint_handler.location.lon, true); 
		}
		
		function convertDDToDMS(D, lng){
            const dms = {
		        dir : D<0?lng?'W':'S':lng?'E':'N',
		        deg : 0|(D<0?D=-D:D),
		        min : 0|D%1*60,
		        sec :(0|D*60%1*6000)/100
		    };
		    return dms.deg + '°' + dms.min + '\'' + dms.sec + '" ' + dms.dir
		}
		
		return flat;
	}
	
	function createAuditLogsTab(auditLogs, timeZone) {
		$('#event-tabs').append(
				$('<li>').addClass('nav-item').append(
					$('<a>').attr('id', 'audit-log-tab-header')
						.attr('data-toggle', 'tab')
						.attr('href', '#audit-log-tab')
						.attr('role', 'tab')
						.attr('aria-controls', 'audit-log-tab')
						.attr('aria-selected', 'false')
						.addClass('nav-link')
						.text('Audit logs')
				)
		);

		const $auditTable = $('<table id="correlation-table">').addClass('table table-hover table-sm').append(
			$('<thead>').append(
				$('<tr>').append(
					$('<th>').attr('style', 'padding: 0.1rem;').text('Handling time'),
					$('<th>').attr('style', 'padding: 0.1rem;').text('Principal id'),
					$('<th>').attr('style', 'padding: 0.1rem;').text('Direct'),
					$('<th>').attr('style', 'padding: 0.1rem;').text('Payload visible'),
					$('<th>').attr('style', 'padding: 0.1rem;').text('Downloaded')
				)
			)
		).append(function () {
			const $tbody = $('<tbody>');
			$.each(auditLogs, function (index, auditLog) {
				$tbody.append(
					$('<tr>').append(
						$('<td>').attr('style', 'padding: 0.1rem;').text(moment.tz(auditLog.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ')),
						$('<td>').attr('style', 'padding: 0.1rem;').text(auditLog.principal_id),
						$('<td>').attr('style', 'padding: 0.1rem;').text(auditLog.direct ? 'Yes' : 'No'),
						$('<td>').attr('style', 'padding: 0.1rem;').text(auditLog.payload_visible ? 'Yes' : 'No'),
						$('<td>').attr('style', 'padding: 0.1rem;').text(auditLog.downloaded ? 'Yes' : 'No')
					)
				);
			});
			return $tbody;
		});

		$('#tabcontents').append(
			$('<div>').attr('id', 'audit-log-tab')
				.attr('role', 'tabpanel')
				.attr('aria-labelledby', 'audit-log-tab-header')
				.addClass('tab-pane fade pt-3')
				.append($auditTable)
		);

	}

    function createEventChainTab(id) {
		$('#event-tabs').append(
			$('<li>').addClass('nav-item').append(
				$('<a>').attr('id', 'event-chain-tab-header')
					.attr('data-toggle', 'tab')
					.attr('href', '#event-chain-tab')
					.attr('role', 'tab')
					.attr('aria-controls', 'event-chain-tab')
					.attr('aria-selected', 'fals')
					.addClass('nav-link')
					.text('Chain times')
			)
		);
	
		$('#tabcontents').append(
				$('<div>').attr('id', 'event-chain-tab')
					.attr('role', 'tabpanel')
					.attr('aria-labelledby', 'event-chain-tab-header')
					.addClass('tab-pane fade pt-3')
					.append(
							$('<div>').addClass('row').append(
									$('<br/>'),
									$('<div>').attr('id', 'event-chain').attr('style', 'width: 100%;')
							),
							$('<div>').attr('id', 'event-chain-node-detail').append(
								$('<div>').addClass('row').append(
									$('<div>').addClass('col-sm-12').append(
                                        $('<p>').addClass('text-center').text('Click on a timespan to view more details')
									)		
								)		
							),
							$('<div>').attr('id', 'event-chain-node-transaction-detail')				
					) 
		);
		$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
            const target = $(e.target).attr("href"); // activated tab
            if (target === '#event-chain-tab' && !$('#event-chain > div > svg').length) {
				$.ajax({
				    type: 'GET',
				    contentType: 'application/json',
                    url: '../rest/search/event/' + encodeURIComponent(id) + '/chain',
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
							currency: response.locale.currency
						});
						const chartConfig = response.chart_config;
						chartConfig.exporting = {
							sourceWidth: 1600,
							fallbackToExportServer: false,
							buttons: {
								contextButton: {
									menuItems: ['downloadPNG', 'downloadSVG', 'downloadCSV', 'downloadXLS']
								}
							}
						};
						chartConfig.plotOptions = {
							xrange: {
								cursor: 'pointer',
								events: {
									click: function (event) {
										let eventData = eventMap[event.point.event_id];
										if ("undefined" == typeof eventData) {
											$.ajax({
												type: 'GET',
												contentType: 'application/json',
												async: false,
												url: '../rest/search/event/' + encodeURIComponent(event.point.event_id) + '/endpoints',
												cache: false,
												success: function (data) {
													if (!data || !data.event || !data.event.source) {
														eventMap[event.point.event_id] = "";
														return;
													}
													eventMap[event.point.event_id] = eventData = data.event;
												}
											});
										}
										$('#event-chain-node-detail, #event-chain-node-transaction-detail').fadeOut().empty();
										if ("" === eventData) {
											return;
										}
										const endpointName = event.point.endpoint;
										const transactionId = event.point.transaction_id;
										$.each(eventData.source.endpoints, function (index, endpoint) {
											if (endpointName === endpoint.name) {
												const writingEndpointHandler = getWritingEndpointHandler(endpoint.endpoint_handlers);
												if (writingEndpointHandler && transactionId === writingEndpointHandler.transaction_id) {
													displayWritingEndpointHandler('event-chain-node-detail', 'event-chain-node-transaction-detail', writingEndpointHandler, response.locale.timezone, endpoint.name);
													return false;
												}
												const readingEndpointHandlers = getReadingEndpointHandlers(endpoint.endpoint_handlers);
												if (readingEndpointHandlers) {
													$.each(readingEndpointHandlers, function (index, eh) {
														if (transactionId === eh.transaction_id) {
															displayReadingEndpointHandler('event-chain-node-detail', 'event-chain-node-transaction-detail', eh, response.locale.timezone, endpoint.name);
															return false;
														}
													});
												}
											}
										});
									}
								}
							}
						};
						// Calculate the height of the chain graph.
						const rowInPixels = 16 + 8; //16 pixels for the event line + 8 pixels for the horizontal line delimiter
						const $scopeTest = $('<div style="display: none; font-size: 1em; margin: 0; padding:0; height: auto; line-height: 1; border:0;">&nbsp;</div>').appendTo('body');
						const scopeVal = $scopeTest.height();
						$scopeTest.remove();
						const emPerLine = (rowInPixels / scopeVal).toFixed(8);

						$('#event-chain').attr('style', 'height: ' + ((chartConfig.yAxis.categories.length * emPerLine) + 5.5) + 'em; width: 100%;');

						eventChainChart = Highcharts.chart('event-chain', chartConfig);
					}
                });
			}
        });
    }

    function getWritingEndpointHandler(endpointHandlers) {
        if (!endpointHandlers) {
            return null;
        }
        const writers = $.grep(endpointHandlers, function (e) {
            return e.type === 'WRITER';
        });
        return writers.length === 0 ? null : writers[0];
    }

    function getReadingEndpointHandlers(endpointHandlers) {
        if (!endpointHandlers) {
            return null;
        }
        return $.grep(endpointHandlers, function (e) {
            return e.type === 'READER';
        });
    }
	
	function capitalize(text) {
		if (!text) {
			return text;
		}
		return text.charAt(0).toUpperCase() + text.toLowerCase().slice(1);
	}
	
	function endsWith(value, valueToTest) {
        const d = value.length - valueToTest.length;
		return d >= 0 && value.lastIndexOf(valueToTest) === d;
	}
	
}