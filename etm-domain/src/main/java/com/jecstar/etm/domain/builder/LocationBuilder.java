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

package com.jecstar.etm.domain.builder;

import com.jecstar.etm.domain.Location;

public class LocationBuilder {

    private final Location location;

    public LocationBuilder() {
        this.location = new Location();
    }

    public Location build() {
        return this.location;
    }

    public LocationBuilder setLatitude(Double latitude) {
        this.location.latitude = latitude;
        return this;
    }

    public LocationBuilder setLongitude(Double longitude) {
        this.location.longitude = longitude;
        return this;
    }
}
