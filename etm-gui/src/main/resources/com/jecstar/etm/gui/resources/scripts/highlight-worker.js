onmessage = function(event) {
  importScripts('highlight.pack.min.js');
  var result = self.hljs.highlightAuto(event.data);
  postMessage(result.value);
}