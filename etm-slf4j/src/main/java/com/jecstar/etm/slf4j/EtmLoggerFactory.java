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

package com.jecstar.etm.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EtmLoggerFactory implements ILoggerFactory {

    private static Configuration configuration;

    static {
        try {
            @SuppressWarnings("unchecked")
            Class<Configuration> clazz = (Class<Configuration>) Class.forName("com.jecstar.etm.slf4j.ConfigurationImpl");
            configuration = clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
        }
        if (configuration == null) {
            String className = System.getProperty("etm.slf4j.configuration");
            if (className != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<Configuration> clazz = (Class<Configuration>) Class.forName(className);
                    configuration = clazz.getDeclaredConstructor().newInstance();
                } catch (Throwable t) {
                }
            }
        }
        if (configuration == null) {
            System.out.println("No etm-slf4j configuration found. Make sure "
                    + "class \"com.jecstar.etm.slf4j.ConfigurationImpl\" is "
                    + "available on the classpath and is implementing the "
                    + "\"com.jecstar.etm.slf4j.Configuration\" interface. "
                    + "Also make sure the class has a public default no-arg "
                    + "constructor. Another option is to provide the system "
                    + "property \"etm.slf4j.configuration\" with a classname "
                    + "that is implementing \"com.jecstar.etm.slf4j.Configuration\". "
                    + "Falling back to defaut configuration which does not log anything!");
        }
        if (configuration == null) {
            configuration = new DefaultConfiguration();
        }
    }

    private final ConcurrentMap<String, Logger> loggerMap;
    private EtmLogForwarder logForwarder;

    public EtmLoggerFactory() {
        this.loggerMap = new ConcurrentHashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Class<EtmLogForwarder> clazz = (Class<EtmLogForwarder>) Class.forName("com.jecstar.etm.slf4j.InternalEtmLogForwarder");
            this.logForwarder = clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            RemoteEtmLogForwarder.configuration = configuration;
            this.logForwarder = RemoteEtmLogForwarder.getInstance();
        }

    }

    @Override
    public Logger getLogger(String name) {
        Logger etmLogger = this.loggerMap.get(name);
        if (etmLogger != null) {
            return etmLogger;
        } else {
            Logger newInstance = new EtmLogger(this.logForwarder, name, configuration);
            Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }

}
