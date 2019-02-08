package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.RowListConverter;
import com.jecstar.etm.server.core.converter.JsonField;

import java.util.List;

/**
 * A <code>Dashboard</code> holds one or more <code>GraphsContainer</code>s and is displayed to the end user.
 */
public class Dashboard {

    public static final String NAME = "name";
    public static final String ROWS = "rows";

    @JsonField(NAME)
    private String name;

    @JsonField(value = ROWS, converterClass = RowListConverter.class)
    private List<Row> rows;

    public String getName() {
        return this.name;
    }

    public Dashboard setName(String name) {
        this.name = name;
        return this;
    }

    public List<Row> getRows() {
        return this.rows;
    }

    public Dashboard setRows(List<Row> rows) {
        this.rows = rows;
        return this;
    }

    public boolean removeGraph(String graphName) {
        if (getRows() == null) {
            return false;
        }
        boolean deleted = false;
        for (Row row : getRows()) {
            if (row.removeGraph(graphName)) {
                deleted = true;
            }
        }
        return deleted;
    }

    public Column getColumnById(String columnId) {
        if (getRows() == null) {
            return null;
        }
        Column column = null;
        for (Row row : getRows()) {
            column = row.getColumnById(columnId);
            if (column != null) {
                break;
            }
        }
        return column;
    }
}
