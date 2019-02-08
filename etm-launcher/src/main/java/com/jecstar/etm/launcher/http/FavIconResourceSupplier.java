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
