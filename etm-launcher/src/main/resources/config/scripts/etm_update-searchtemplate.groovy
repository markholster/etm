if (template) {
    if (ctx._source.searchtemplates) {
        def current_template = ctx._source.searchtemplates.find { item -> item.name.equals(template.name)};
        if (current_template) {
            current_template.query = template.query;
            current_template.types = template.types;
        } else {
            ctx._source.searchtemplates += template;
        }
    } else {
        ctx._source.searchtemplates = [template];
    }
}
