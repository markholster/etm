onmessage = function(event) {
  importScripts('highlight.pack.min.js');
  var result;
  if ('HTML' == event.data[1] || 'SOAP' == event.data[1] || 'XML' == event.data[1]) {
	  result = hljs.highlight('xml', event.data[0], true);
  } else if ('SQL' == event.data[1]) {
	  result = hljs.highlight('sql', event.data[0], true);
  } else if ('JSON' == event.data[1]) {
	  result = hljs.highlight('json', event.data[0], true);
  } else {
	  result = hljs.highlightAuto(event.data[0]);
  }
  postMessage(result.value);
}