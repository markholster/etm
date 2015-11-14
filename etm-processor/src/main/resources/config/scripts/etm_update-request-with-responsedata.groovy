if (ctx._source.creation_time) {
// The request is present, calculate the response time
    if (!ctx._source.responsetime || (ctx._source.responsetime && ctx._source.responsetime > (creation_time - ctx._source.creation_time))) {
        ctx._source.responsetime = creation_time - ctx._source.creation_time;
    }
} else {
// The request is not stored yet, store the response data in the request.
    if (!ctx._source.response_handling_time || (ctx._source.response_handling_time > creation_time)) {
        ctx._source.response_handling_time = creation_time;
    }
};
if (correlation_id) {
    if (ctx._source.child_correlation_ids) {
        ctx._source.child_correlation_ids += correlation_id
    } else {
        ctx._source.child_correlation_ids = [correlation_id]
    }
};