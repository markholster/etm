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

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandler;

import java.time.Instant;

public class EndpointBuilder {

    private final Endpoint endpoint;

    public EndpointBuilder() {
        this.endpoint = new Endpoint();
    }

    public Endpoint build() {
        return this.endpoint;
    }

    public EndpointBuilder setName(String name) {
        this.endpoint.name = name;
        return this;
    }

    public String getName() {
        return this.endpoint.name;
    }

    /**
     * @deprecated Use {@link EndpointBuilder#addEndpointHandler(EndpointHandler)}
     */
    @Deprecated
    public EndpointBuilder setWritingEndpointHandler(EndpointHandler writingEndpointHandler) {
        writingEndpointHandler.type = EndpointHandler.EndpointHandlerType.WRITER;
        return addEndpointHandler(writingEndpointHandler);
    }

    /**
     * @deprecated Use {@link EndpointBuilder#addEndpointHandler(EndpointHandler)}
     */
    @Deprecated
    public EndpointBuilder setWritingEndpointHandler(EndpointHandlerBuilder writingEndpointHandlerBuilder) {
        return addEndpointHandler(writingEndpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.WRITER));
    }

    /**
     *
     * @deprecated Use {@link EndpointBuilder#addEndpointHandler(EndpointHandler)}
     */
    @Deprecated
    public EndpointBuilder addReadingEndpointHandler(EndpointHandler readingEndpointHandler) {
        readingEndpointHandler.type = EndpointHandler.EndpointHandlerType.READER;
        return addEndpointHandler(readingEndpointHandler);
    }

    /**
     *
     * @deprecated Use {@link EndpointBuilder#addEndpointHandler(EndpointHandler)}
     */
    @Deprecated
    public EndpointBuilder addReadingEndpointHandler(EndpointHandlerBuilder readingEndpointHandlerBuilder) {
        return addEndpointHandler(readingEndpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.READER));
    }

    public EndpointBuilder addEndpointHandler(EndpointHandler endpointHandler) {
        this.endpoint.addEndpointHandler(endpointHandler);
        return this;
    }

    public EndpointBuilder addEndpointHandler(EndpointHandlerBuilder endpointHandlerBuilder) {
        this.endpoint.addEndpointHandler(endpointHandlerBuilder.build());
        return this;
    }

    public EndpointBuilder setWritingTimeToNow() {
        EndpointHandler endpointHandler = this.endpoint.getWritingEndpointHandler();
        if (endpointHandler != null) {
            endpointHandler.handlingTime = Instant.now();
        }
        return this;
    }
}
