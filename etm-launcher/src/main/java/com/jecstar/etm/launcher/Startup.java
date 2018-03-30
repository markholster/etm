package com.jecstar.etm.launcher;

import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.builder.ApplicationBuilder;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.slf4j.EtmLoggerFactory;
import com.jecstar.etm.launcher.slf4j.LogConfiguration;
import com.jecstar.etm.processor.internal.persisting.BusinessEventLogger;
import com.jecstar.etm.processor.internal.persisting.InternalBulkProcessorWrapper;
import com.jecstar.etm.processor.jms.configuration.CustomConnectionFactory;
import com.jecstar.etm.processor.jms.configuration.JNDIConnectionFactory;
import com.jecstar.etm.processor.jms.configuration.NativeConnectionFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;

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
            InternalBulkProcessorWrapper bulkProcessorWrapper = new InternalBulkProcessorWrapper();
            EtmLoggerFactory.initialize(bulkProcessorWrapper);
            BusinessEventLogger.initialize(bulkProcessorWrapper,
                    new EndpointBuilder().setName(configuration.instanceName)
                            .addEndpointHandler(new EndpointHandlerBuilder().setHandlingTime(ZonedDateTime.now())
                                    .setType(EndpointHandler.EndpointHandlerType.WRITER)
                                    .setApplication(new ApplicationBuilder().setName("Enterprise Telemetry Monitor")
                                            .setVersion(System.getProperty("app.version"))
                                            .setInstance(configuration.instanceName)
                                            .setPrincipal(System.getProperty("user.name"))
                                            .setHostAddress(InetAddress.getByName(configuration.bindingAddress)))));
            if (commandLineParameters.isTail()) {
                new TailCommand().tail(configuration);
            } else {
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
        try (Reader reader = new FileReader(configFile);) {
            Constructor constructor = new Constructor();
            constructor.addTypeDescription(new TypeDescription(NativeConnectionFactory.class, new Tag("!nativeConnectionFactory")));
            constructor.addTypeDescription(new TypeDescription(JNDIConnectionFactory.class, new Tag("!jndiConnectionFactory")));
            constructor.addTypeDescription(new TypeDescription(CustomConnectionFactory.class, new Tag("!customConnectionFactory")));
            Yaml yaml = new Yaml(constructor);

            configuration = yaml.loadAs(reader, Configuration.class);
        }
        addEnvironmentToConfigruation(configuration, null);
        return configuration;
    }

    private static void addEnvironmentToConfigruation(Object configurationInstance, String prefix) {
        Field[] fields = configurationInstance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().getName().startsWith("com.jecstar.")) {
                // A nested configuration class.
                try {
                    addEnvironmentToConfigruation(field.get(configurationInstance), prefix == null ? field.getName() : prefix + "_" + field.getName());
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
                        field.set(configurationInstance, new Integer(value));
                    } else if (field.getType().equals(File.class)) {
                        field.set(configurationInstance, new File(value));
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                }
            }
        }
    }
}
