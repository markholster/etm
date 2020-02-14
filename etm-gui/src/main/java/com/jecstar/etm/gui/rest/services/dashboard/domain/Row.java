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

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.ColumnListConverter;
import com.jecstar.etm.server.core.converter.JsonField;

import java.util.List;

public class Row {

    public static final String ID = "id";
    public static final String HEIGHT = "height";
    public static final String COLUMNS = "columns";

    @JsonField(ID)
    private String id;
    @JsonField(HEIGHT)
    private int height;
    @JsonField(value = COLUMNS, converterClass = ColumnListConverter.class)
    private List<Column> columns;

    public String getId() {
        return this.id;
    }

    public int getHeight() {
        return this.height;
    }

    public List<Column> getColumns() {
        return this.columns;
    }

    public Row setId(String id) {
        this.id = id;
        return this;
    }

    public Row setHeight(int height) {
        this.height = height;
        return this;
    }

    public Row setColumns(List<Column> columns) {
        this.columns = columns;
        return this;
    }

    public boolean removeGraph(String graphName) {
        if (getColumns() == null) {
            return false;
        }
        boolean deleted = false;
        for (Column column : getColumns()) {
            if (column.removeGraph(graphName)) {
                deleted = true;
            }
        }
        return deleted;
    }

    public Column getColumnById(String columnId) {
        if (getColumns() == null) {
            return null;
        }
        for (Column column : getColumns()) {
            if (columnId.equals(column.getId())) {
                return column;
            }
        }
        return null;
    }
}
