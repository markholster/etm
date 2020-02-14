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

package com.jecstar.etm.domain;

public class Location {

    /**
     * The latitude part of the location.
     */
    public Double latitude;

    /**
     * The longitude part of the location.
     */
    public Double longitude;

    public Location initialize() {
        this.latitude = null;
        this.longitude = null;
        return this;
    }

    public Location initialize(Location copy) {
        this.initialize();
        if (copy == null) {
            return this;
        }
        this.latitude = copy.latitude;
        this.longitude = copy.longitude;
        return this;
    }

    public boolean isSet() {
        return this.latitude != null && this.longitude != null;
    }
}
