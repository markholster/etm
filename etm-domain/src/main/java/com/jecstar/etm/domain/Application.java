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

import java.net.InetAddress;
import java.util.Objects;

public class Application {

    /**
     * The name of the application.
     */
    public String name;

    /**
     * The hostAddress of the application.
     */
    public InetAddress hostAddress;

    /**
     * The instance of the application. Useful if the application is clustered.
     */
    public String instance;

    /**
     * The principal that executed the action that triggers the event.
     */
    public String principal;

    /**
     * The version of the application.
     */
    public String version;

    public Application initialize() {
        this.name = null;
        this.hostAddress = null;
        this.instance = null;
        this.principal = null;
        this.version = null;
        return this;
    }

    public Application initialize(Application copy) {
        this.initialize();
        if (copy == null) {
            return this;
        }
        this.name = copy.name;
        this.hostAddress = copy.hostAddress;
        this.instance = copy.instance;
        this.principal = copy.principal;
        this.version = copy.version;
        return this;
    }

    public boolean isSet() {
        return this.name != null || this.instance != null || this.principal != null || this.version != null || this.hostAddress != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.instance, that.instance) &&
                Objects.equals(this.version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.instance, this.version);
    }
}
