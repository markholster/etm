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

package com.jecstar.etm.processor.jms.configuration;

import java.util.ArrayList;
import java.util.List;

public class Jms {

    public boolean enabled = false;

    public final List<AbstractConnectionFactory> connectionFactories = new ArrayList<>();

    public int getMinimumNumberOfListeners() {
        if (this.connectionFactories.isEmpty()) {
            return 0;
        }
        return this.connectionFactories.stream().mapToInt(
                f -> f.destinations.stream().mapToInt(Destination::getMinNrOfListeners).sum()
        ).sum();
    }

    public int getMaximumNumberOfListeners() {
        if (this.connectionFactories.isEmpty()) {
            return 0;
        }
        return this.connectionFactories.stream().mapToInt(
                f -> f.destinations.stream().mapToInt(Destination::getMaxNrOfListeners).sum()
        ).sum();
    }

    public List<AbstractConnectionFactory> getConnectionFactories() {
        return this.connectionFactories;
    }
}
