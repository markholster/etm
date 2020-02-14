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

package com.jecstar.etm.launcher;

import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.server.core.util.BCrypt;
import org.yaml.snakeyaml.Yaml;

class CommandLineParameters {

    private static final String PARAM_CONFIG_DIRECTORY = "--config-dir=";
    private static final String PARAM_CREATE_PASSWORD = "--create-passwordhash=";
    private static final String PARAM_DUMP_DEFAULT_CONFIG = "--dump-default-config";
    private static final String PARAM_REINITIALIZE = "--reinitialize";
    private static final String PARAM_QUIET = "--quiet";
    private static final String PARAM_TAIL = "--tail";

    private String configDirectory = "config";

    private boolean quiet = false;
    private boolean reinitialize = false;
    private boolean proceedNormalStartup = true;
    private boolean tail = false;

    CommandLineParameters(String[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return;
        }
        for (String argument : arguments) {
            if (argument.startsWith(PARAM_CONFIG_DIRECTORY)) {
                this.configDirectory = argument.substring(PARAM_CONFIG_DIRECTORY.length());
            } else if (argument.startsWith(PARAM_CREATE_PASSWORD)) {
                this.proceedNormalStartup = false;
                System.out.println(BCrypt.hashpw(argument.substring(PARAM_CREATE_PASSWORD.length()), BCrypt.gensalt()));
            } else if (argument.equals(PARAM_QUIET)) {
                this.quiet = true;
            } else if (argument.equals(PARAM_DUMP_DEFAULT_CONFIG)) {
                this.proceedNormalStartup = false;
                Yaml yaml = new Yaml();
                System.out.print(yaml.dumpAsMap(new Configuration()));
            } else if (argument.equals(PARAM_REINITIALIZE)) {
                this.reinitialize = true;
            } else if (argument.equals(PARAM_TAIL)) {
                this.tail = true;
            }
        }
    }

    public boolean isProceedNormalStartup() {
        return this.proceedNormalStartup;
    }

    public boolean isQuiet() {
        return this.quiet;
    }

    public boolean isReinitialize() {
        return this.reinitialize;
    }

    public String getConfigDirectory() {
        return this.configDirectory;
    }

    public boolean isTail() {
        return this.tail;
    }
}
