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

package com.jecstar.etm.server.core;

class NoSuchEventException extends RuntimeException {

    private static final long serialVersionUID = -5013113610442814312L;

    private final String index;
    private final String id;
    private final Long version;

    public NoSuchEventException(String index, String id, Long version) {
        this.index = index;
        this.id = id;
        this.version = version;
    }

    @Override
    public String getMessage() {
        if (this.version == null) {
            return "Event '" + this.index + " - " + this.id + "' does not exist";
        } else {
            return "Event '" + this.index + " - " + this.id + " - " + this.version + "' does not exist";
        }
    }
}
