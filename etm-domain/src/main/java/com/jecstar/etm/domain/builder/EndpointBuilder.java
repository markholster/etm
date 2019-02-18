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
