if (name) {
    if (ctx._source.searchtemplates) {
        ctx._source.searchtemplates.removeAll { item -> item.name.equals(name)};
        if (ctx._source.searchtemplates.size == 0) {
            ctx._source.searchtemplates = null;
        }
    } else {
        ctx._source.searchtemplates = [template];
    }
}
