if (ctx._source.writing_endpoint_handler.handling_time) {
	if (ctx._source.response_time && ctx._source.response_time > (ctx._source.writing_endpoint_handler.handling_time - response_handling_time)) {
		ctx._source.response_time = ctx._source.writing_endpoint_handler.handling_time - response_handling_time;
	}
} else {
	if (!ctx._source.response_handling_time || (ctx._source.response_handling_time > response_handling_time)) {
		ctx._source.response_handling_time = response_handling_time;
	}
}