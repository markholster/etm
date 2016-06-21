var cyEndpoints; 
var cyEventChain;
var transactionMap = {};

function showEvent(scrollTo, type, id) {
	$('#search-container').hide();
	$('#event-tabs').children().slice(1).remove();
	$('#tabcontents').children().slice(1).remove();
	$eventTab = $('#event-tab');
	$eventTab.empty();	
	$eventTab.tab('show');
	$('#event-tab-header').addClass('active').attr('area-expanded', 'true');
	intialize();
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/search/event/' + encodeURIComponent(type) + '/' + encodeURIComponent(id),
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        addContent(data);
	    }
	});
	
	$('#btn-back-to-results, #link-back-to-results')
		.unbind('click')
		.click(function(event) {
			event.preventDefault();
			intialize();
			$('#event-container').hide();
			$('#search-container').show();
			$('html,body').animate({scrollTop: scrollTo},'fast');
    });
	$('#event-container').show();

	function intialize() {
		if (cyEndpoints) {
			cyEndpoints.destroy();
		}
		if (cyEventChain) {
			cyEventChain.destroy();
		}
		$.map(transactionMap, function(value, index) {
    		return [value];
		}).forEach(function (element) {$(element).remove();});
		transactionMap = {};
	}

	function addContent(data) {
		$('#event-card-title').text('Event ' + data.id);
		$('#event-tab-header').text(capitalize(data.type));
		$eventTab = $('#event-tab');
		if (data.source) {
			if (data.source.name) {
				$('#event-card-title').text(data.source.name);
			}
			writeEventDataToTab($eventTab, $('#event-tab-header'), data, false);
			var $endpoints = $(data.source.endpoints);
			if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
				createEndpointsTab($endpoints, data.time_zone);
				// Check if a transaction id is present
				var hasTransactionId = false;
				$.each($endpoints, function (index, endpoint) {
					if (endpoint.writing_endpoint_handler && endpoint.writing_endpoint_handler.transaction_id) {
						hasTransactionId = true;
						return false;
					}
					if (endpoint.reading_endpoint_handlers) {
						$.each(endpoint.reading_endpoint_handlers, function (index, eh) {
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
					createEventChainTab(data.id, data.type, data.time_zone);
				}
			}
		}
	}
	
	function writeEventDataToTab(tab, tabHeader, data, correlated) {
		$eventTab = $(tab);
		$tabHeader = $(tabHeader);
		if (correlated && data.id) {
			var dataLink = $('<a href="#">')
				.text(data.id)
				.addClass('form-control-static')
				.attr('style', 'display: inline-block;')
				.click(function (event) {
					showEvent(scrollTo, data.type, data.id);
			}); 
			appendElementToContainerInRow($eventTab, 'Id', dataLink);
		} else {
			appendToContainerInRow($eventTab, 'Id', data.id);
		}
		appendToContainerInRow($eventTab, 'Name', data.source.name);
		if (!correlated && data.source.correlation_id) {
			var dataLink = $('<a href="#">')
				.text(data.source.correlation_id)
				.addClass('form-control-static')
				.attr('style', 'display: inline-block;')
				.click(function (event) {
					showEvent(scrollTo, data.type, data.source.correlation_id);
			}); 
			appendElementToContainerInRow($eventTab, 'Correlation id', dataLink);
		} else {
			appendToContainerInRow($eventTab, 'Correlation id', data.source.correlation_id);
		}
		
		appendToContainerInRow($eventTab, 'Payload format', data.source.payload_format);
		var $endpoints = $(data.source.endpoints);
		if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
			var writing_times = $endpoints.map(function () {return this.writing_endpoint_handler.handling_time}).get();
			appendToContainerInRow($eventTab, 'First write time', moment.tz(Math.min.apply(Math, writing_times), data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
		}
		appendToContainerInRow($eventTab, 'Messaging type', data.source.messaging_type);
		appendToContainerInRow($eventTab, 'Expiry time', moment.tz(data.source.expiry, data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
		if ('log' === data.type) {
			appendToContainerInRow($eventTab, 'Log level', data.source.log_level);
		} else if ('http' === data.type) {
			if (data.source.http_type) {
				// http type known, determine request or response.
				if ('RESPONSE' === data.source.http_type) {
					$tabHeader.text('Http response');
					if (data.source.correlation_id && !correlated) {
						addCorrelationTab(scrollTo, data.type, data.source.correlation_id, 'request-tab', 'Http request');
					}
				} else {
					$tabHeader.text('Http request');
					if (data.source.correlations && !correlated) {
						$.each(data.source.correlations, function (index, correlation_id) {
							addCorrelationTab(scrollTo, data.type, correlation_id, 'response-tab' + index, 'Http response');
						});
					}
				}
			}
			appendToContainerInRow($eventTab, 'Http type', data.source.http_type);
			if ('RESPONSE' !== data.source.http_type) {
				appendToContainerInRow($eventTab, 'Expiry time', moment.tz(data.source.expiry, data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
			}
		} else if ('messaging' === data.type) {
			if (data.source.messaging_type) {
				// messaging type known, determine request or response.
				if ('RESPONSE' === data.source.messaging_type) {
					$tabHeader.text('Response message');
					if (data.source.correlation_id && !correlated) {
						addCorrelationTab(scrollTo, data.type, data.source.correlation_id, 'request-tab', 'Request message');
					}
				} else if ('REQUEST' === data.source.messaging_type) {
					$tabHeader.text('Request message');
					if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
						var response_times = $endpoints.map(function () {return this.writing_endpoint_handler.response_time}).get();
						if (response_times) {
							appendToContainerInRow($eventTab, 'Highest writer response time', response_times.length > 0 ? Math.max.apply(Math, response_times) : null);
						}
						response_times = $endpoints.map(function () {
							if (this.reading_endpoint_handlers) {
								return $(this.reading_endpoint_handlers).map(function () {
									return this.response_time;
								}).get();
							}
						}).get();
						if (response_times) {
							appendToContainerInRow($eventTab, 'Highest reader response time', response_times.length > 0 ? Math.max.apply(Math, response_times) : null);
						}
					}
					if (data.source.correlations && !correlated) {
						$.each(data.source.correlations, function (index, correlation_id) {
							addCorrelationTab(scrollTo, data.type, correlation_id, 'response-tab' + index, 'Response message');
						});
					}
				} else {
					$tabHeader.text('Fire-forget message');
				}
			}
		} else if ('sql' === data.type) {
			if (data.source.sql_type) {
				// sql type known, determine query or result.
				if ('RESULTSET' === data.source.sql_type) {
					$tabHeader.text('Sql result');
					if (data.source.correlation_id && !correlated) {
						addCorrelationTab(scrollTo, data.type, data.source.correlation_id, 'request-tab', 'Sql query');
					}
				} else {
					$tabHeader.text('Sql query');
					if (data.source.correlations && !correlated) {
						$.each(data.source.correlations, function (index, correlation_id) {
							addCorrelationTab(scrollTo, data.type, correlation_id, 'response-tab' + index, 'Sql result');
						});
					}
				}
			}		
		}
		if ("undefined" != typeof data.source.metadata) {
	    	$eventTab.append(createDetailMap('metadata', data.source.metadata));
	    }
	    if ("undefined" != typeof data.source.extracted_data) {
	    	$eventTab.append(createDetailMap('extracts', data.source.extracted_data));
	    }
	    $eventTab.append(
	    		$('<div>').addClass('row').append(
	    				$('<div>').addClass('col-sm-12').append(
	    						$('<pre>').attr('style', 'white-space: pre-wrap;').text(data.source.payload)
	    				)
	    		)
	    );
	    
	    if ('log' === data.type && "undefined" != typeof data.source.stack_trace) {
	    	$eventTab.append(
	    		$('<div>').addClass('row').append(
	    				$('<div>').addClass('col-sm-12').append(
	    						$('<pre>').attr('style', 'white-space: pre-wrap;').text(data.source.stack_trace)
	    				)
	    		)
			);
	    }
		
	}
	
	function addCorrelationTab(scrollTo, type, id, tabType, tabName) {
		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
		    url: '../rest/search/event/' + encodeURIComponent(type) + '/' + encodeURIComponent(id),
		    success: function(correlation_data) {
		        if (!correlation_data) {
		            return;
		        }
				$('#event-tabs').children().eq(0).after(
					$('<li>').addClass('nav-item').append(
						$('<a>').attr('id',  tabType + '-header')
							.attr('aria-expanded', 'false')
							.attr('role', 'tab')
							.attr('data-toggle', 'tab')
							.attr('href', '#' + tabType)
							.addClass('nav-link')
							.text(tabName)
					)
				);
				$('#tabcontents').children().eq(0).after(
					$('<div>').attr('id', tabType)
						.attr('aria-labelledby', tabType + '-header')
						.attr('role', 'tabpanel')
						.attr('aria-expanded', 'false')
						.addClass('tab-pane fade')
				);
		        writeEventDataToTab($('#' + tabType), $('#' + tabType + '-header'), correlation_data, true);
		    }
		});
	}
	
	function appendToContainerInRow(container, name, value) {
		appendElementToContainerInRow(container, name, $('<p>').addClass('form-control-static').text(value));
	}
	
	function appendElementToContainerInRow(container, name, element) {
		var $container = $(container);
		var $row = $container.children(":last-child");
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
		$detailMap = $('<div>').addClass('panel panel-default').append(
				$('<div>').addClass('panel-heading clearfix').append(
						$('<div>').addClass('pull-left').append($('<a>').addClass('font-weight-bold').attr('href', '#').text(capitalize(name)).click(function (event) {
							event.preventDefault();
							$('#event_' + name + '_panel_collapse').collapse('toggle');
						}))
				),
				$('<div>').attr('id', 'event_' + name + '_panel_collapse').addClass('panel-collapse collapse').append(
						$('<div>').addClass('panel-body').append(
								$('<div>').addClass('table-responsive').append(
										$('<table>').addClass('table table-sm table-striped table-hover').append(
												$('<thead>').append($('<tr>').append($('<th>').attr('style', 'padding: 0.1rem;').text('Name')).append($('<th>').attr('style', 'padding: 0.1rem;').text('Value')))
										).append(function () {
											$tbody = $('<tbody>');
								            $.each(valueMap, function(key, value) {
								            	if (endsWith(key, '_as_number') || endsWith(key, '_as_boolean') || endsWith(key, '_as_date')) {
								            		return true;
								            	}
								            	$tbody.append(
								            			$('<tr>').append(
								            					$('<td>').attr('style', 'padding: 0.1rem;').text(key),
								            					$('<td>').attr('style', 'padding: 0.1rem;').text(value)
								            			)
								            	);
								            });
											return $tbody;
										})
								)
						)
				)
		);
		return $detailMap;
	}
	
	function createEndpointsTab(endpoints, timeZone) {
		$('#event-tabs').append(
				$('<li>').addClass('nav-item').append(
						$('<a>').attr('id', 'endpoint-tab-header')
							.attr('aria-expanded', 'false')
							.attr('role', 'tab')
							.attr('data-toggle', 'tab')
							.attr('href', '#endpoint-tab')
							.addClass('nav-link')
							.text('Endpoints')
				)
		);
	
		// Sort endpoints on writing time
		endpoints.sort(function (ep1, ep2) {
			return ep1.writing_endpoint_handler.handling_time - ep2.writing_endpoint_handler.handling_time;
		});
		
		var nodesData = [];
		var edgesData = [];
		var rowIx = -1;
		$.each(endpoints, function (index, endpoint) {
			rowIx++;
			var endpointId = rowIx + '-1';
			if (endpoint.writing_endpoint_handler.application) {
				var name = endpoint.writing_endpoint_handler.application.name ? endpoint.writing_endpoint_handler.application.name : '?';
				nodesData.push({
					data: {
						id: rowIx + '-0',
						name: name,
						shape: 'roundrectangle',
						width: 'label',
						color: '#ffffff',
						background_color: '#b6b6b6',
						row: rowIx,
						col: 0,
						writing_endpoint_handler: endpoint.writing_endpoint_handler,
						time_zone: timeZone
					}
				});
				edgesData.push({ data: { source: rowIx + '-0', target: endpointId } });
			}
			nodesData.push({
				data: {
					id: endpointId,
					name: endpoint.name ? endpoint.name : '?',
					shape: 'roundrectangle',
					width: 'label',
					color: '#ffffff',
					background_color: '#98afc7',
					row: rowIx,
					col: 1,
					endpoint: endpoint,
					time_zone: timeZone
				}
			});
			if (endpoint.reading_endpoint_handlers) {
				endpoint.reading_endpoint_handlers.sort(function (han1, han2) {
					return han1.handling_time - han2.handling_time;
				});
				$.each(endpoint.reading_endpoint_handlers, function (repIx, rep) {
					if (repIx !== 0) {
						rowIx++;
					}
					var name = rep.application.name ? rep.application.name : '?';
					nodesData.push({
						data: {
							id: rowIx + '-2',
							name: name,
							shape: 'roundrectangle',
							width: 'label',
							color: '#ffffff',
							background_color: '#b6b6b6',
							row: rowIx,
							col: 2,
							reading_endpoint_handler: rep,
							time_zone: timeZone
						}
					});
					edgesData.push({ data: { source: endpointId, target: rowIx + '-2' } });
				});
			}
		}); 
		var body = document.body, html = document.documentElement;
		var height = Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight) / 10;
		$('#tabcontents').append(
				$('<div>').attr('id', 'endpoint-tab')
					.attr('aria-labelledby', 'endpoint-tab-header')
					.attr('role', 'tabpanel')
					.attr('aria-expanded', 'false')
					.addClass('tab-pane fade')
					.append(
							$('<div>').addClass('row').append(
									$('<div>').attr('id', 'endpoint-overview').attr('style', 'height: ' + ((rowIx == 0 ? 1 : rowIx) * height) + 'px; width: 100%;')
							),
							$('<div>').attr('id', 'endpoint-node-detail').append(
									$('<div>').addClass('row').append(
											$('<div>').addClass('col-sm-12').append(
													$('<p>').addClass('text-xs-center').text('Click on a node to view more details')
											)		
									)		
							),
							$('<div>').attr('id', 'endpoint-node-transaction-detail')
					) 
		);
		$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
			var target = $(e.target).attr("href") // activated tab
			if (target == '#endpoint-tab' && !$('#endpoint-overview > div > canvas').length) {
				cyEndpoints = cytoscape({
					  container: document.querySelector('#endpoint-overview'),
					  zoomingEnabled: false,
					  panningEnabled: true,
					  boxSelectionEnabled: false,
					  autoungrabify: true, 
					  
					  style: cytoscape.stylesheet()
					    .selector('node')
					      .css({
					        'content': 'data(name)',
					        'shape': 'data(shape)',
					        'width': 'data(width)',
					        'text-valign': 'center',
					        'color': 'data(color)',
					        'background-color': 'data(background_color)'
					      })
					    .selector('edge')
					      .css({
					    	'width': 2,
					        'target-arrow-shape': 'triangle'
					      }),				  
					  elements: {
					    nodes: nodesData,
					    edges: edgesData
					  },
					  layout: {
						  name: 'grid',
						  condense: false,
						  cols: 3,
						  rows: rowIx + 1,
						  position: function( node ){
							  return {row: node.data('row'), col: node.data('col')} 
						  }
					  }
					});
				cyEndpoints.on('tap', function(event) {
					  var evtTarget = event.cyTarget;
					  if( evtTarget !== cyEndpoints ){
						  if (evtTarget.data('writing_endpoint_handler')) {
							  displayWritingEndpointHandler(cyEndpoints, evtTarget.data('writing_endpoint_handler'), evtTarget.data('time_zone'));
						  } else if (evtTarget.data('endpoint')) {
							  displayEndpoint(cyEndpoints, evtTarget.data('endpoint'), evtTarget.data('time_zone'));
						  } else if (evtTarget.data('reading_endpoint_handler')) {
							  displayReadingEndpointHandler(cyEndpoints, evtTarget.data('reading_endpoint_handler'), evtTarget.data('time_zone'));
						  }
					  }
				});
			}
		});
	}
	
	function displayWritingEndpointHandler(cyEndpoints, endpoint_handler, timeZone) {
		$transactionDetails = $('#endpoint-node-transaction-detail');
		$transactionDetails.fadeOut('fast', function() {
			$('#transaction-detail-table').detach();
			$(this).empty();
		});
		$('#endpoint-node-detail').fadeOut('fast', function () {
			$this = $(this).empty();
			var eh = formatEndpointHandler(endpoint_handler, timeZone);
			appendToContainerInRow($this, 'Write time', eh.handling_time);
			appendToContainerInRow($this, 'Response time', eh.response_time);
			appendToContainerInRow($this, 'Transaction id', eh.transaction_id);
			appendToContainerInRow($this, 'Location', eh.location);
			appendToContainerInRow($this, 'Application name', eh.application_name);
			appendToContainerInRow($this, 'Application version', eh.application_version);
			appendToContainerInRow($this, 'Application instance', eh.application_instance);
			appendToContainerInRow($this, 'Application user', eh.application_principal);
			appendToContainerInRow($this, 'Application address', eh.application_host);
			if (eh.transaction_id && eh.application_name) {
				displayTransactionDetails($transactionDetails, eh.application_name, eh.transaction_id)
				$transactionDetails.append('<br>');
			} else {
				$this.append('<br>');
			}
			$this.fadeIn('fast');
			$transactionDetails.fadeIn('fast');
		});
	}
	
	function displayEndpoint(cyEndpoints, endpoint, timeZone) {
		$('#endpoint-node-detail').fadeOut('fast', function () {
			$this = $(this).empty();
			appendToContainerInRow($this, 'Write time', moment.tz(endpoint.writing_endpoint_handler.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
			appendToContainerInRow($this, 'Endpoint name', endpoint.name);
			$this.append('<br>');
			$this.fadeIn('fast');
		});
		$('#endpoint-node-transaction-detail').fadeOut('fast', function() {
			$('#transaction-detail-table').detach();
			$(this).empty();
		});
	}
	
	function displayReadingEndpointHandler(cyEndpoints, endpoint_handler, timeZone) {
		$transactionDetails = $('#endpoint-node-transaction-detail');
		$transactionDetails.fadeOut('fast', function() {
			$('#transaction-detail-table').detach();
			$(this).empty();
		});
		$('#endpoint-node-detail').fadeOut('fast', function () {
			$this = $(this).empty();
			var eh = formatEndpointHandler(endpoint_handler, timeZone);
			appendToContainerInRow($this, 'Read time', eh.handling_time);
			appendToContainerInRow($this, 'Response time', eh.response_time);
			appendToContainerInRow($this, 'Transaction id', eh.transaction_id);
			appendToContainerInRow($this, 'Location', eh.location);
			appendToContainerInRow($this, 'Application name', eh.application_name);
			appendToContainerInRow($this, 'Application version', eh.application_version);
			appendToContainerInRow($this, 'Application instance', eh.application_instance);
			appendToContainerInRow($this, 'Application user', eh.application_principal);
			appendToContainerInRow($this, 'Application address', eh.application_host);
			appendToContainerInRow($this, 'Latency', eh.latency);
			if (eh.transaction_id && eh.application_name) {
				displayTransactionDetails($transactionDetails, eh.application_name, eh.transaction_id)
				$transactionDetails.append('<br>');
			} else {
				$this.append('<br>');
			}
			$this.fadeIn('fast');
			$transactionDetails.fadeIn('fast');
		});
	}
	
	function displayTransactionDetails(container, applicationName, transactionId) {
		$container = $(container);
		$transactionTable = transactionMap[transactionId];
		if ("undefined" != typeof $transactionTable) {
			$container.append($transactionTable);
		} else {
			$.ajax({
			    type: 'GET',
			    contentType: 'application/json',
			    url: '../rest/search/transaction/' + encodeURIComponent(applicationName) + '/' + encodeURIComponent(transactionId),
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
			        			$('<th>').attr('style' ,'padding: 0.1rem;')
			        		)
			        	)
			        ).append(function () {
			        	$tbody = $('<tbody>');
			        	$.each(transaction_data.events, function(index, event) {
			        		$link = $('<a href="#">')
								.text(event.id)
								.click(function (clickEvent) {
									showEvent(scrollTo, event.type, event.id);
							}); 
			        		$tbody.append(
			        			$('<tr>').append(
			        				event.id == id ? $('<td>').attr('style' ,'padding: 0.1rem;').text(event.id) : $('<td>').attr('style' ,'padding: 0.1rem;').append($link),
			        				$('<td>').attr('style' ,'padding: 0.1rem;').text(moment.tz(event.handling_time, transaction_data.time_zone).format('YYYY-MM-DDTHH:mm:ss.SSSZ')),
			        				$('<td>').attr('style' ,'padding: 0.1rem;').text('sql' == event.type ? 'SQL' : capitalize(event.type)),
			        				$('<td>').attr('style' ,'padding: 0.1rem;').text(formatTransactionLine(event))
			        			)
			        		);	
			        	});
			        	return $tbody;
			        })
			        transactionMap[transactionId] = $transactionTable;
			        $container.append($transactionTable);
			    }
			});			
		}
	}
	
	function formatTransactionLine(event) {
		if ('business' == event.type) {
			return event.name ? event.name : '?';
		} else if ('http' == event.type) {
			if ('incomming' == event.direction) {
				return 'Received ' + ("RESPONSE" == event.sub_type ? 'http response' : 'http ' + event.payload);
			} else {
				return 'Send ' + ("RESPONSE" == event.sub_type ? 'http response' : 'http ' + event.payload);
			}
		} else if ('log' == event.type) {
			return event.payload;
		} else if ('messaging' == event.type) {
			if ('REQUEST' == event.sub_type) {
				if ('incomming' == event.direction) {
					return 'Received request message ' + (event.name ? event.name : '?');
				} else {
					return 'Send request message ' + (event.name ? event.name : '?');
				}
			} else if ('RESPONSE' == event.sub_type) {
				if ('incomming' == event.direction) {
					return 'Received response message ' + (event.name ? event.name : '?');
				} else {
					return 'Send response message ' + (event.name ? event.name : '?');
				}
			} else {
				if ('incomming' == event.direction) {
					return 'Received fire-forget message ' + (event.name ? event.name : '?');
				} else {
					return 'Send fire-forget message ' + (event.name ? event.name : '?');
				}
			}
		} else if ('sql' == event.type) {
			if ('incomming' == event.direction) {
				return 'Received ' + ("RESULTSET" == event.sub_type ? 'sql resultset' : event.payload);
			} else {
				return 'Send ' + ("RESULTSET" == event.sub_type ? 'sql resultset' : event.payload);
			}
		}
	}
	
	function formatEndpointHandler(endpoint_handler, timeZone) {
		var flat = {
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
		    var dms =  {
		        dir : D<0?lng?'W':'S':lng?'E':'N',
		        deg : 0|(D<0?D=-D:D),
		        min : 0|D%1*60,
		        sec :(0|D*60%1*6000)/100
		    };
		    return dms.deg + '°' + dms.min + '\'' + dms.sec + '" ' + dms.dir
		}
		
		return flat;
	}
	
	function createEventChainTab(id, type, timeZone) {
		$('#event-tabs').append(
			$('<li>').addClass('nav-item').append(
					$('<a>').attr('id', 'event-chain-tab-header')
						.attr('aria-expanded', 'false')
						.attr('role', 'tab')
						.attr('data-toggle', 'tab')
						.attr('href', '#event-chain-tab')
						.addClass('nav-link')
						.text('Event chain')
			)
		);
	
		$('#tabcontents').append(
				$('<div>').attr('id', 'event-chain-tab')
					.attr('aria-labelledby', 'event-chain-tab-header')
					.attr('role', 'tabpanel')
					.attr('aria-expanded', 'false')
					.addClass('tab-pane fade')
					.append(
							$('<div>').addClass('row').append(
									$('<div>').attr('id', 'event-chain').attr('style', 'width: 100%;')
							)
					) 
		);
		$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
			var target = $(e.target).attr("href") // activated tab
			if (target == '#event-chain-tab' && !$('#event-chain > div > canvas').length) {
				var body = document.body, html = document.documentElement;
				var height = Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight) * 0.40;
				$('#event-chain').attr('style', 'height: ' + height+ 'px; width: 100%;');
				$.ajax({
				    type: 'GET',
				    contentType: 'application/json',
				    url: '../rest/search/event/' + encodeURIComponent(type) + '/' + encodeURIComponent(id) + '/chain',
				    success: function(data) {
				        if (!data) {
				            return;
				        }
						var nodesData = [];
						$.each(data.nodes, function (index, node) {
							var eventColorCode = '#b6b6b6';
							if ("undefined" != typeof node.absolute_response_percentage) {
								var colorValue = Math.round(node.absolute_response_percentage * 178);
								eventColorCode = '#' + ('0' + colorValue.toString(16)).slice(-2) + ('0' + (178 - colorValue).toString(16)).slice(-2) + '34';
							}
							var color = node.type == 'application' || node.missing ? '#000000' : '#ffffff';
							var borderColor = node.type == 'endpoint' ? '#98afc7' : '#b6b6b6';
							var borderStyle = node.missing ? 'dotted' : 'solid';
							var backgroundColor = node.missing ? '#ffffff' : (node.type == 'endpoint' ? '#98afc7' : '#b6b6b6');
							nodesData.push({
								data: {
									id: node.id,
									label: node.label,
									width: 'label',
									color: color,
									border_style: borderStyle,
									border_color: borderColor,
									background_color: backgroundColor,
									parent: node.parent
								}
							});
						});
						var edgesData = [];
						$.each(data.edges, function (index, edge) {
							var arrowColor = '#dddddd';
							var arrowWidth = 2;
							if ("undefined" != typeof edge.transition_time_percentage) {
								var colorValue = Math.round(edge.transition_time_percentage * 178);
								arrowColor = '#' + ('0' + colorValue.toString(16)).slice(-2) + ('0' + (178 - colorValue).toString(16)).slice(-2) + '34';
								arrowWidth += Math.round(edge.transition_time_percentage * 8);
							}
							edgesData.push({
								data: {
									source: edge.source,
									target: edge.target,
									arrow_color: arrowColor,
									arrow_width: arrowWidth
								}
							});
						});
						cyEventChain = cytoscape({
						  container: document.querySelector('#event-chain'),
						  zoomingEnabled: true,
						  panningEnabled: true,
						  boxSelectionEnabled: false,
					  	  autounselectify: true, 						  
					  	  style: cytoscape.stylesheet()
						    .selector('node')
						      .css({
						        'content': 'data(label)',
						        'shape': 'roundrectangle',
						        'width': 'label',
						        'text-valign': 'center',
						        'border-width': 2,
						        'border-style': 'data(border_style)',
						        'color': 'data(color)',
						        'border-color': 'data(border_color)',
						        'background-color': 'data(background_color)'
						      })
						    .selector('$node > node') 
						      .css({
						        'content': 'data(label)',
						        'shape': 'roundrectangle',
						        'text-valign': 'top',
						        'text-halign': 'center',
						        'border-width': 2,
						        'border-style': 'data(border_style)',
						        'color': 'data(color)',
						        'border-color': 'data(border_color)',
						        'background-color': 'data(background_color)'						      
						      })
						    .selector('edge')
						      .css({
						      	'label': 'data(label)',
						      	'edge-text-rotation': 'autorotate',
						      	'curve-style': 'bezier',
						    	'width': 'data(arrow_width)',
						    	'line-color': 'data(arrow_color)',
						    	'target-arrow-color': 'data(arrow_color)',
						        'target-arrow-shape': 'triangle'
					      }),
						  elements: {
						    nodes: nodesData,
						    edges: edgesData
						  },
						  
						  layout: {
							name: 'dagre',
							rankDir: 'LR',
							fit: true
							
						  }
						});
						cyEventChain.center();
				    }
				});				
			}
		});				
	}
	
	function capitalize(text) {
		if (!text) {
			return text;
		}
		return text.charAt(0).toUpperCase() + text.toLowerCase().slice(1);
	}
	
	function endsWith(value, valueToTest) {
		var d = value.length - valueToTest.length;
		return d >= 0 && value.lastIndexOf(valueToTest) === d;
	}
	
}