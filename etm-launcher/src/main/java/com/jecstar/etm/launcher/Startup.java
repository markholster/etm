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

import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.builder.ApplicationBuilder;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.slf4j.EtmLoggerFactory;
import com.jecstar.etm.launcher.slf4j.LogConfiguration;
import com.jecstar.etm.processor.jms.configuration.CustomConnectionFactory;
import com.jecstar.etm.processor.jms.configuration.JNDIConnectionFactory;
import com.jecstar.etm.processor.jms.configuration.NativeConnectionFactory;
import com.jecstar.etm.server.core.persisting.internal.BusinessEventLogger;
import com.jecstar.etm.server.core.persisting.internal.InternalBulkProcessorWrapper;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Startup {

    public static void main(String[] args) {
        CommandLineParameters commandLineParameters = new CommandLineParameters(args);
        if (!commandLineParameters.isProceedNormalStartup()) {
            return;
        }
        try {
            Configuration configuration = loadConfiguration(new File(commandLineParameters.getConfigDirectory()));
            LogConfiguration.loggers.putAll(configuration.logging.loggers);
            LogConfiguration.rootLogLevel = configuration.logging.rootLogger;
            LogConfiguration.applicationInstance = configuration.instanceName;
            LogConfiguration.hostAddress = InetAddress.getByName(configuration.bindingAddress);
            if (commandLineParameters.isTail()) {
                new TailCommand().tail(configuration);
            } else if (commandLineParameters.isMigrate()) {
                new MigrateCommand().migrate(commandLineParameters.getMigratorName(), configuration);
                System.exit(0);
            } else {
                InternalBulkProcessorWrapper bulkProcessorWrapper = new InternalBulkProcessorWrapper();
                EtmLoggerFactory.initialize(bulkProcessorWrapper, commandLineParameters.isQuiet());
                BusinessEventLogger.initialize(bulkProcessorWrapper,
                        new EndpointBuilder().setName(configuration.instanceName)
                                .addEndpointHandler(new EndpointHandlerBuilder().setHandlingTime(Instant.now())
                                        .setType(EndpointHandler.EndpointHandlerType.WRITER)
                                        .setApplication(new ApplicationBuilder().setName("Enterprise Telemetry Monitor")
                                                .setVersion(System.getProperty("app.version"))
                                                .setInstance(configuration.instanceName)
                                                .setPrincipal(System.getProperty("user.name"))
                                                .setHostAddress(InetAddress.getByName(configuration.bindingAddress)))));
                new LaunchEtmCommand().launch(commandLineParameters, configuration, bulkProcessorWrapper);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Configuration file not found: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("Invalid binding address: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
        }
    }

    /**
     * Loads the configuration from the configuration file and overrides the
     * options provided in the environment.
     *
     * @param configDir The directory that contains the "etm.yml" configuration file.
     * @return A fully initialized <code>Configuration</code> instance.
     * @throws IOException When reading or parsing the "etm.yml" file fails.
     */
    private static Configuration loadConfiguration(File configDir) throws IOException {
        if (!configDir.exists()) {
            throw new FileNotFoundException(configDir.getAbsolutePath());
        }
        File configFile = new File(configDir, "etm.yml");
        if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
            throw new FileNotFoundException(configFile.getAbsolutePath());
        }
        Configuration configuration;
        try (Reader reader = new FileReader(configFile)) {
            configuration = createYamlInstance().loadAs(reader, Configuration.class);
        }
        addEnvironmentToConfiguration(configuration, null);
        if (configuration.secret == null) {
            createSecret(configuration, configFile);
        }
        return configuration;
    }

    /**
     * Creates a random secret and add it to the configuration file and object.
     *
     * @param configuration The <code>Configuration instance</code>
     * @param configFile    The configuration file.
     * @throws IOException when reading or updating the configuration file fails.
     */
    private static void createSecret(Configuration configuration, File configFile) throws IOException {
        final var secret = generateSecret();
        final var commentLine = "# The secret that is used to encrypt passwords before they get saved. Make sure this secret has the same value on all Enterprise Telemetry Monitor instances you run in your cluster!";
        final var secretLine = "secret: " + secret;
        var path = configFile.toPath();
        var lines = Files.lines(path).collect(Collectors.toList());
        int instanceNameIx = -1;
        int secretIx = -1;
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            if (line.startsWith("instanceName:")) {
                instanceNameIx = i;
            } else if (line.startsWith("secret:")) {
                secretIx = i;
            }
        }
        if (secretIx != -1) {
            lines.remove(secretIx);
            lines.add(secretIx, secretLine);
        } else if (instanceNameIx != -1) {
            lines.add(instanceNameIx + 1, secretLine);
            lines.add(instanceNameIx + 1, commentLine);
        } else {
            lines.add(0, secretLine);
            lines.add(0, commentLine);
        }
        Files.write(path, lines);
        configuration.secret = secret;
    }

    private static Yaml createYamlInstance() {
        var constructor = new Constructor();
        constructor.addTypeDescription(new TypeDescription(NativeConnectionFactory.class, new Tag("!nativeConnectionFactory")));
        constructor.addTypeDescription(new TypeDescription(JNDIConnectionFactory.class, new Tag("!jndiConnectionFactory")));
        constructor.addTypeDescription(new TypeDescription(CustomConnectionFactory.class, new Tag("!customConnectionFactory")));
        return new Yaml(constructor);
    }

    private static String generateSecret() {
        var dictionary = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
        var random = new SecureRandom();
        StringBuilder pwd = new StringBuilder();
        for (var i = 0; i < 48; i++) {
            pwd.append(dictionary.charAt(random.nextInt(dictionary.length())));
        }
        return pwd.toString();
    }

    private static void addEnvironmentToConfiguration(Object configurationInstance, String prefix) {
        Field[] fields = configurationInstance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().getName().startsWith("com.jecstar.")) {
                // A nested configuration class.
                try {
                    addEnvironmentToConfiguration(field.get(configurationInstance), prefix == null ? field.getName() : prefix + "_" + field.getName());
                } catch (IllegalArgumentException | IllegalAccessException e) {
                }
            } else {
                String value = System.getenv(prefix == null ? field.getName() : prefix + "_" + field.getName());
                if (value == null) {
                    continue;
                }
                try {
                    if (field.getType().equals(String.class)) {
                        field.set(configurationInstance, value);
                    } else if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
                        field.set(configurationInstance, Boolean.valueOf(value));
                    } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                        field.set(configurationInstance, Integer.valueOf(value));
                    } else if (field.getType().equals(File.class)) {
                        field.set(configurationInstance, new File(value));
                    } else if (field.getType().equals(List.class)) {
                        List<String> listValue = Arrays.asList(value.split(","));
                        field.set(configurationInstance, listValue);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                }
            }
        }
    }
}
