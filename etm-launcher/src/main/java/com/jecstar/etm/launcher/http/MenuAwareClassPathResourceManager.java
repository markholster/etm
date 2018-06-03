package com.jecstar.etm.launcher.http;

import com.jecstar.etm.launcher.http.MenuAwareURLResource.MenuContext;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.Resource;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.stream.Collectors;

class MenuAwareClassPathResourceManager extends ClassPathResourceManager {

    private final EtmConfiguration etmConfiguration;
    private final String prefix;
    private final ClassLoader classLoader;

    MenuAwareClassPathResourceManager(EtmConfiguration etmConfiguration, ClassLoader classLoader, String prefix) {
        super(classLoader, prefix);
        this.etmConfiguration = etmConfiguration;
        this.prefix = prefix;
        this.classLoader = classLoader;
    }


    @Override
    public Resource getResource(String path) throws IOException {
        if (path.toLowerCase().endsWith(".html")) {
            String modPath = path;
            if (modPath.startsWith("/")) {
                modPath = path.substring(1);
            }
            final String realPath = this.prefix + modPath;
            final URL resource = this.classLoader.getResource(realPath);
            if (resource == null) {
                return null;
            } else {
                final int pathLevels = path.length() - path.replace("/", "").length();
                final String pathPrefixToContextRoot = pathLevels <= 1 ? "./" : Collections.nCopies(pathLevels - 1, "../").stream().collect(Collectors.joining());
                MenuContext menuContext = null;
                final String lowerCasePath = path.toLowerCase();
                if (lowerCasePath.startsWith("/search/")) {
                    menuContext = MenuContext.SEARCH;
                } else if (lowerCasePath.startsWith("/dashboard/")) {
                    menuContext = MenuContext.DASHBOARD;
                } else if (lowerCasePath.startsWith("/signal/")) {
                    menuContext = MenuContext.SIGNAL;
                } else if (lowerCasePath.startsWith("/preferences/")) {
                    menuContext = MenuContext.PREFERENCES;
                } else if (lowerCasePath.startsWith("/settings/") || lowerCasePath.startsWith("/iib/")) {
                    menuContext = MenuContext.SETTINGS;
                }
                return new MenuAwareURLResource(this.etmConfiguration, pathPrefixToContextRoot, menuContext, resource, path);
            }

        }
        return super.getResource(path);
    }
}
