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

package com.jecstar.etm.launcher.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceSupplier;
import io.undertow.server.handlers.resource.URLResource;

import java.net.URL;

public class FavIconResourceSupplier implements ResourceSupplier {

    @Override
    public Resource getResource(HttpServerExchange exchange, String path) {
        if (exchange.isComplete()) {
            return null;
        }
        URL resource = getClass().getClassLoader().getResource("com/jecstar/etm/gui/resources/images/favicon/favicon.ico");
        if (resource != null) {
            return new URLResource(resource, "/favicon.ico");
        }
        return null;
    }
}
