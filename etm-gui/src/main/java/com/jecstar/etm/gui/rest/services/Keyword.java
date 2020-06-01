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

package com.jecstar.etm.gui.rest.services;

public class Keyword {

    public final static Keyword EXISTS = new Keyword("_exists_", null);

    private final String name;
    private final String type;

    public Keyword(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public boolean isNumber() {
        return "long".equals(this.type) || "float".equals(this.type);
    }

    public boolean isDate() {
        return "date".equals(this.type);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Keyword) {
            return ((Keyword) obj).getName().equals(this.name);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
