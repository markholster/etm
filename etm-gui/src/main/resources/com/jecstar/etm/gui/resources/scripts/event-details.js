var cyEndpoints; 
var cyEventChain;
var transactionMap = {};
var eventMap = {};
var clipboards = [];

function showEvent(scrollTo, type, id) {
	$('#search-container').hide();
	$('#event-tabs').children().slice(1).remove();
	$('#tabcontents').children().slice(1).remove();
	$eventTab = $('#event-tab');
	$eventTab.empty();	
	$eventTab.tab('show');
	$('#event-tab-header').addClass('active').attr('area-expanded', 'true');
	// Since bootstap4 alpha 6 we need to add the class 'show' to the tab
	$eventTab.addClass('show')
	intialize();
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/search/event/' + encodeURIComponent(type) + '/' + encodeURIComponent(id),
	    success: function(data) {
	        if (!data || !data.event) {
	            return;
	        }
	        addContent(data);
	    }
	});
	
	$('#btn-back-to-results, #link-back-to-results').attr('data-scroll-to', scrollTo);
	
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
		eventMap = {};
		$.each(clipboards, function(index, clipboard) {
			clipboard.destroy();
		}); 
		clipboards = [];
	}

	function addContent(data) {
		$('#event-card-title').text('Event ' + data.event.id);
		$('#event-tab-header').text(capitalize(data.event.type));
		$eventTab = $('#event-tab');
		if (data.event.source) {
			if (data.event.source.name) {
				$('#event-card-title').text(data.event.source.name);
			}
			writeEventDataToTab($eventTab, $('#event-tab-header'), data.event, data.time_zone);
			if (data.correlated_events) {
				$.each(data.correlated_events, function(index, correlated_event) {
					$('#event-tabs').children().eq(0).after(
						$('<li>').addClass('nav-item').append(
							$('<a>').attr('id',  'correlation-header-' + index)
								.attr('aria-expanded', 'false')
								.attr('role', 'tab')
								.attr('data-toggle', 'tab')
								.attr('href', '#' + 'correlation-' + index)
								.addClass('nav-link')
								.text('Correlating event ' + index)
						)
					);
					$('#tabcontents').children().eq(0).after(
						$('<div>').attr('id', 'correlation-' + index)
							.attr('aria-labelledby', 'correlation-header' + index)
							.attr('role', 'tabpanel')
							.attr('aria-expanded', 'false')
							.addClass('tab-pane fade')
					);
					writeEventDataToTab($('#correlation-' + index), $('#correlation-header-' + index), correlated_event, data.time_zone);
				}); 
				
			}
			var $endpoints = $(data.event.source.endpoints);
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
					createEventChainTab(data.event.id, data.event.type, data.time_zone);
				}
			}
		}
	}
	
	function writeEventDataToTab(tab, tabHeader, data, timeZone) {
		$eventTab = $(tab);
		$tabHeader = $(tabHeader);
		var dataLink = $('<a href="#">')
			.text(data.id)
			.addClass('form-control-static')
			.attr('style', 'display: inline-block;')
			.attr('data-link-type', 'show-event')
			.attr('data-scroll-to', scrollTo)
			.attr('data-event-type', data.type)
			.attr('data-event-id', data.id);
		appendElementToContainerInRow($eventTab, 'Id', dataLink);
		appendToContainerInRow($eventTab, 'Name', data.source.name);
		if (data.source.correlation_id) {
			var dataLink = $('<a href="#">')
				.text(data.source.correlation_id)
				.addClass('form-control-static')
				.attr('style', 'display: inline-block;')
				.attr('data-link-type', 'show-event')
				.attr('data-scroll-to', scrollTo)
				.attr('data-event-type', data.type)
				.attr('data-event-id', data.source.correlation_id);
			appendElementToContainerInRow($eventTab, 'Correlation id', dataLink);
		}
		
		appendToContainerInRow($eventTab, 'Payload format', data.source.payload_format);
		var $endpoints = $(data.source.endpoints);
		if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
			var writing_times = $endpoints.map(function () {
				if (this.writing_endpoint_handler) {
					return this.writing_endpoint_handler.handling_time
				}
			}).get();			
			appendToContainerInRow($eventTab, 'First write time', moment.tz(Math.min.apply(Math, writing_times), timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
		}
		if ('log' === data.type) {
			appendToContainerInRow($eventTab, 'Log level', data.source.log_level);
		} else if ('http' === data.type) {
			appendToContainerInRow($eventTab, 'Http type', data.source.http_type);
			if (data.source.expiry) {
				appendToContainerInRow($eventTab, 'Expiry time', moment.tz(data.source.expiry, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
			}
			if (data.source.http_type) {
				// http type known, determine request or response.
				if ('RESPONSE' === data.source.http_type) {
					$tabHeader.text('Http response');
				} else {
					$tabHeader.text('Http request');
					if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
						var response_times = $endpoints.map(function () {
							if (this.writing_endpoint_handler) {
								return this.writing_endpoint_handler.response_time
							}
						}).get();
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
				}
			}
		} else if ('messaging' === data.type) {
			appendToContainerInRow($eventTab, 'Messaging type', data.source.messaging_type);
			if (data.source.expiry) {
				appendToContainerInRow($eventTab, 'Expiry time', moment.tz(data.source.expiry, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
			}
			if (data.source.messaging_type) {
				// messaging type known, determine request or response.
				if ('RESPONSE' === data.source.messaging_type) {
					$tabHeader.text('Response message');
				} else if ('REQUEST' === data.source.messaging_type) {
					$tabHeader.text('Request message');
					if ("undefined" != typeof $endpoints && $endpoints.length > 0) {
						var response_times = $endpoints.map(function () {
							if (this.writing_endpoint_handler) {
								return this.writing_endpoint_handler.response_time
							}
						}).get();
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
				} else {
					$tabHeader.text('Fire-forget message');
				}
			}
		} else if ('sql' === data.type) {
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
						var response_times = $endpoints.map(function () {
							if (this.writing_endpoint_handler) {
								return this.writing_endpoint_handler.response_time
							}
						}).get();
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
				}
			}		
		}
		if ("undefined" != typeof data.source.metadata) {
	    	$eventTab.append(createDetailMap('metadata', data.source.metadata));
	    }
	    if ("undefined" != typeof data.source.extracted_data) {
	    	$eventTab.append(createDetailMap('extracted data', data.source.extracted_data));
	    }
	    
	    $eventTab.append($('<br/>'));
	    var payloadCode = $('<code>').text(indentCode(data.source.payload, data.source.payload_format));
	    $clipboard = $('<a>').attr('href', "#").addClass('small').text('Copy raw payload to clipboard');
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
	    	var worker = new Worker('../scripts/highlight-worker.js');
	    	worker.onmessage = function(result) { 
	    		payloadCode.html(result.data);
	    	}
	    	worker.postMessage([payloadCode.text(), data.source.payload_format]);
	    }
	    if ('log' === data.type && "undefined" != typeof data.source.stack_trace) {
	    	$eventTab.append(
	    		$('<div>').addClass('row').append(
	    				$('<div>').addClass('col-sm-12').append(
	    						$('<pre>').attr('style', 'white-space: pre-wrap;').append($('<code>').text(data.source.stack_trace))
	    				)
	    		)
			);
	    }
	    
	    function indentCode(code, format) {
	    	if ('HTML' == format || 'SOAP' == format || 'XML' == format) {
	    		return vkbeautify.xml(code, 4);
	    	} else if ('SQL' == format) {
	    		return vkbeautify.sql(code, 4);
	    	} else if ('JSON' == format) {
	    		return vkbeautify.json(code, 4);
	    	}
	    	return code;
	    }
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
		var panelId = generateUUID();
		$detailMap = $('<div>').addClass('panel panel-default').append(
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
						$('<div>').addClass('panel-body').append(
								$('<div>').addClass('table-responsive').append(
										$('<table>').addClass('table table-sm table-striped table-hover').append(
												$('<thead>').append($('<tr>').append($('<th>').attr('style', 'padding: 0.1rem;').text('Name')).append($('<th>').attr('style', 'padding: 0.1rem;').text('Value')))
										).append(function () {
											$tbody = $('<tbody>');
											var detailItems = [];
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
										})
								)
						)
				)
		);
		return $detailMap;
	}
	
	function generateUUID(){
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
													$('<p>').addClass('text-center').text('Click on a node to view more details')
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
							  displayWritingEndpointHandler('endpoint-node-detail', 'endpoint-node-transaction-detail', evtTarget.data('writing_endpoint_handler'), evtTarget.data('time_zone'));
						  } else if (evtTarget.data('endpoint')) {
							  displayEndpoint('endpoint-node-detail', 'endpoint-node-transaction-detail', evtTarget.data('endpoint'), evtTarget.data('time_zone'));
						  } else if (evtTarget.data('reading_endpoint_handler')) {
							  displayReadingEndpointHandler('endpoint-node-detail', 'endpoint-node-transaction-detail', evtTarget.data('reading_endpoint_handler'), evtTarget.data('time_zone'));
						  }
					  }
				});
			}
		});
	}
	
	function displayWritingEndpointHandler(nodeDetailContainerId, transactionDetailContainerId, endpoint_handler, timeZone) {
		$transactionDetails = $('#' + transactionDetailContainerId);
		$transactionDetails.fadeOut('fast', function() {
			$('#transaction-detail-table').detach();
			$(this).empty();
		});
		$('#' + nodeDetailContainerId).fadeOut('fast', function () {
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
	
	function displayEndpoint(nodeDetailContainerId, transactionDetailContainerId, endpoint, timeZone) {
		$('#' + nodeDetailContainerId).fadeOut('fast', function () {
			$this = $(this).empty();
			appendToContainerInRow($this, 'Write time', moment.tz(endpoint.writing_endpoint_handler.handling_time, timeZone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'));
			appendToContainerInRow($this, 'Endpoint name', endpoint.name);
			$this.append('<br>');
			$this.fadeIn('fast');
		});
		$('#' + transactionDetailContainerId).fadeOut('fast', function() {
			$('#transaction-detail-table').detach();
			$(this).empty();
		});
	}
	
	function displayReadingEndpointHandler(nodeDetailContainerId, transactionDetailContainerId, endpoint_handler, timeZone) {
		$transactionDetails = $('#' + transactionDetailContainerId);
		$transactionDetails.fadeOut('fast', function() {
			$('#transaction-detail-table').detach();
			$(this).empty();
		});
		$('#' + nodeDetailContainerId).fadeOut('fast', function () {
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
								.attr('data-link-type', 'show-event')
								.attr('data-scroll-to', scrollTo)
								.attr('data-event-type', event.type)
								.attr('data-event-id', event.id);
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
			if ('incoming' == event.direction) {
				return 'Received ' + ("RESPONSE" == event.sub_type ? 'http response' : 'http ' + event.payload);
			} else {
				return 'Send ' + ("RESPONSE" == event.sub_type ? 'http response' : 'http ' + event.payload);
			}
		} else if ('log' == event.type) {
			return event.payload;
		} else if ('messaging' == event.type) {
			if ('REQUEST' == event.sub_type) {
				if ('incoming' == event.direction) {
					return 'Received request message ' + (event.name ? event.name : '?') + ' from ' + event.endpoint;
				} else {
					return 'Send request message ' + (event.name ? event.name : '?') + ' to ' + event.endpoint;
				}
			} else if ('RESPONSE' == event.sub_type) {
				if ('incoming' == event.direction) {
					return 'Received response message ' + (event.name ? event.name : '?') + ' from ' + event.endpoint;
				} else {
					return 'Send response message ' + (event.name ? event.name : '?') + ' to ' + event.endpoint;
				}
			} else {
				if ('incoming' == event.direction) {
					return 'Received fire-forget message ' + (event.name ? event.name : '?') + ' from ' + event.endpoint;
				} else {
					return 'Send fire-forget message ' + (event.name ? event.name : '?') + ' to ' + event.endpoint;
				}
			}
		} else if ('sql' == event.type) {
			if ('incoming' == event.direction) {
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
									$('<br/>'),
									$('<div>').attr('id', 'event-chain').attr('style', 'width: 100%;')
							),
							$('<div>').attr('id', 'event-chain-node-detail').append(
								$('<div>').addClass('row').append(
									$('<div>').addClass('col-sm-12').append(
											$('<p>').addClass('text-center').text('Click on a node to view more details')
									)		
								)		
							),
							$('<div>').attr('id', 'event-chain-node-transaction-detail')				
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
							var color = node.node_type == 'application' || node.missing ? '#000000' : '#ffffff';
							var borderColor = node.node_type == 'endpoint' ? '#98afc7' : '#b6b6b6';
							var borderStyle = node.missing ? 'dotted' : 'solid';
							var backgroundColor = node.missing ? '#ffffff' : (node.node_type == 'endpoint' ? '#98afc7' : '#b6b6b6');
							nodesData.push({
								data: {
									id: node.id,
									label: node.label,
									event_id: node.event_id,
									event_type: node.event_type,
									endpoint: node.endpoint,
									transaction_id: node.transaction_id,
									type: node.node_type,
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
							var color = '#000000';
							var arrowColor = '#dddddd';
							var arrowWidth = 2;
							if ("undefined" != typeof edge.transition_time_percentage) {
								var redFactor = 178;
								var colorValue = Math.round(edge.transition_time_percentage * redFactor);
								arrowColor = '#' + ('0' + colorValue.toString(16)).slice(-2) + ('0' + (redFactor - colorValue).toString(16)).slice(-2) + '34';
								arrowWidth += Math.round(edge.transition_time_percentage * 8);
							}
							edgesData.push({
								data: {
									label: (edge.transition_time_percentage ? Math.round(edge.transition_time_percentage * 100) : '0') + '%',
									source: edge.source,
									target: edge.target,
									color: color,
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
						    	'color': 'data(color)',
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
						}).ready(function () {
							var maxZoom = this.maxZoom();
							this.maxZoom(1);
							this.fit();
							this.maxZoom(maxZoom);
							this.center();  
						});
						cyEventChain.on('tap', function(event) {
							var evtTarget = event.cyTarget;
							if( evtTarget !== cyEndpoints ) {
								var eventId = evtTarget.data('event_id');
								var eventData = eventMap[eventId];
								if ("undefined" == typeof eventData) {
									var eventType = evtTarget.data('event_type');
									$.ajax({
									    type: 'GET',
									    contentType: 'application/json',
									    async: false,
									    url: '../rest/search/event/' + encodeURIComponent(eventType) + '/' + encodeURIComponent(eventId) + '/endpoints',
									    success: function(data) {
									        if (!data || !data.event || !data.event.source) {
									        	eventMap[eventId] = "";
									            return;
									        }
									        eventMap[eventId] = eventData = data.event;
									    }
									});									
								}
								if ("" == eventData) {
									return;
								}
								var nodeType = evtTarget.data('type');
								if ("endpoint" === nodeType) {
									var endpointName = evtTarget.data('label')
									$.each(eventData.source.endpoints, function (index, endpoint) {
										if (endpointName == endpoint.name) {
											displayEndpoint('event-chain-node-detail', 'event-chain-node-transaction-detail', endpoint, eventData.time_zone);
											return false;
										}
									});
								} else if ("event" === nodeType) {
									var endpointName = evtTarget.data('endpoint')
									var transactionId = evtTarget.data('transaction_id')
									$.each(eventData.source.endpoints, function (index, endpoint) {
										if (endpointName == endpoint.name) {
											if (endpoint.writing_endpoint_handler && transactionId == endpoint.writing_endpoint_handler.transaction_id) {
												displayWritingEndpointHandler('event-chain-node-detail', 'event-chain-node-transaction-detail', endpoint.writing_endpoint_handler, eventData.time_zone);
												return false;
											}
											if (endpoint.reading_endpoint_handlers) {
												$.each(endpoint.reading_endpoint_handlers, function (index, eh) {
													if (transactionId == eh.transaction_id) {
														displayReadingEndpointHandler('event-chain-node-detail', 'event-chain-node-transaction-detail', eh, eventData.time_zone);
														return false;
													}
												});
											}
										}
									});
									
								}
							}
						});						
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