if (application) {ctx._source.application = application};
if (content) {ctx._source.content = content};
if (endpoint) {ctx._source.endpoint = endpoint};
if (expiry_time) {ctx._source.expiry_time = expiry_time};
if (correlation_id) {ctx._source.correlation_id = correlation_id};
if (correlation_data) {if (ctx._source.correlation_data) {ctx._source.correlation_data += correlation_data} else {ctx._source.correlation_data = correlation_data}};
if (creation_time) {ctx._source.creation_time = creation_time};
if (direction) {ctx._source.direction = direction};
if (metadata) {if (ctx._source.metadata) {ctx._source.metadata += metadata} else {ctx._source.metadata = metadata}};
if (name) {ctx._source.name = name};
if (transaction_id) {ctx._source.transaction_id = transaction_id};
if (transaction_name) {ctx._source.transaction_name = transaction_name};
if (type) {ctx._source.type = type};

// Update the response time.
if (creation_time && ctx._source.response_handling_time) {
    ctx._source.response_time = ctx._source.response_handling_time - creation_time; 
    ctx._source.remove('response_handling_time');
};
