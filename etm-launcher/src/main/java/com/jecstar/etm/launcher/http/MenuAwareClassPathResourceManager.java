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

import com.jecstar.etm.server.core.Etm;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.Resource;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

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
                final String pathPrefixToContextRoot = pathLevels <= 1 ? "./" : String.join("", Collections.nCopies(pathLevels - 1, "../"));
                return new MenuAwareURLResource(this.etmConfiguration, pathPrefixToContextRoot, resource, path);
            }
        } else if (Etm.hasVersion() && path.toLowerCase().endsWith(".js")) {
            return super.getResource(path.replace("/scripts/" + Etm.getVersion() + "/", "/scripts/"));
        }
        return super.getResource(path);
    }

}
