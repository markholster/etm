/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
