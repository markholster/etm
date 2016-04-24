if (template) {
    if (ctx._source.searchtemplates) {
        def current_template = ctx._source.searchtemplates.find { item -> item.name.equals(template.name)};
        if (current_template) {
            current_template.query = template.query;
            current_template.types = template.types;
            current_template.fields = template.fields;
            current_template.results_per_page = template.results_per_page;
            current_template.sort_field = template.sort_field;
            current_template.sort_order = template.sort_order;
        } else {
            ctx._source.searchtemplates += template;
        }
    } else {
        ctx._source.searchtemplates = [template];
    }
}
