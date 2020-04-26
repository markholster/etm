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

package com.jecstar.etm.processor.elastic.apm.domain.spans;

import com.jecstar.etm.server.core.converter.JsonField;

/**
 * An object containing contextual data for database spans
 */
public class Db {

    @JsonField("instance")
    private String instance;
    @JsonField("link")
    private String link;
    @JsonField("statement")
    private String statement;
    @JsonField("type")
    private String type;
    @JsonField("user")
    private String user;
    @JsonField("rows_affected")
    private Long rowsAffected;

    /**
     * Database instance name
     */
    public String getInstance() {
        return this.instance;
    }

    /**
     * Database link
     */
    public String getLink() {
        return this.link;
    }

    /**
     * A database statement (e.g. query) for the given database type
     */
    public String getStatement() {
        return this.statement;
    }

    /**
     * Database type. For any SQL database, "sql". For others, the lower-case database category, e.g. "cassandra", "hbase", or "redis"
     */
    public String getType() {
        return this.type;
    }

    /**
     * Username for accessing database
     */
    public String getUser() {
        return this.user;
    }

    /**
     * Number of rows affected by the SQL statement (if applicable)
     */
    public Long getRowsAffected() {
        return this.rowsAffected;
    }
}