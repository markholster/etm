package com.jecstar.etm.launcher.http;

import com.jecstar.etm.gui.rest.EtmExceptionMapper;
import com.jecstar.etm.gui.rest.RestGuiApplication;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.http.auth.ApiKeyAuthenticationMechanism;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionManagerFactory;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.rest.RestTelemetryEventProcessorApplication;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.tls.SSLContextBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.predicate.Predicates;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class HttpServer {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(HttpServer.class);

    private final Undertow server;
    private final GracefulShutdownHandler shutdownHandler;
    private boolean started;
    private final SessionManagerFactory sessionManagerFactory;

    public HttpServer(
            final IdentityManager identityManager,
            final Configuration configuration,
            final EtmConfiguration etmConfiguration,
            final TelemetryCommandProcessor processor,
            final DataRepository dataRepository) {
        this.sessionManagerFactory = new ElasticsearchSessionManagerFactory(dataRepository, etmConfiguration);
        final PathHandler root = Handlers.path();
        this.shutdownHandler = Handlers.gracefulShutdown(root);
        final ServletContainer container = ServletContainer.Factory.newInstance();
        Builder builder = Undertow.builder()
                .setIoThreads(configuration.http.ioThreads)
                .setWorkerThreads(configuration.http.workerThreads);

        if (configuration.http.httpPort > 0) {
            builder.addHttpListener(configuration.http.httpPort, configuration.bindingAddress);
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("Binding http listener to '" + configuration.bindingAddress + ":" + configuration.http.httpPort + "'");
            }
        }
        if (configuration.http.httpsPort > 0) {
            if (configuration.http.sslKeystoreLocation == null) {
                if (log.isWarningLevelEnabled()) {
                    log.logWarningMessage("SSL keystore not provided. Https listener not started.");
                }
            } else {
                try {
                    SSLContext sslContext = new SSLContextBuilder().createSslContext(
                            configuration.http.sslProtocol,
                            configuration.http.sslKeystoreLocation,
                            configuration.http.sslKeystoreType,
                            configuration.http.sslKeystorePassword == null ? null : configuration.http.sslKeystorePassword.toCharArray(),
                            configuration.http.sslTruststoreLocation,
                            configuration.http.sslTruststoreType,
                            configuration.http.sslTruststorePassword == null ? null : configuration.http.sslTruststorePassword.toCharArray());

                    builder.addHttpsListener(configuration.http.httpsPort, configuration.bindingAddress, sslContext);
                    builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
                    if (log.isInfoLevelEnabled()) {
                        log.logInfoMessage("Binding https listener to '" + configuration.bindingAddress + ":" + configuration.http.httpsPort + "'");
                    }
                } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
                    if (log.isErrorLevelEnabled()) {
                        log.logErrorMessage("Unable to create SSL context. Https listener not started.", e);
                    }
                }
            }
        }
        this.server = builder.setHandler(root).build();
        SessionListenerAuditLogger sessionListenerAuditLogger = new SessionListenerAuditLogger(dataRepository, etmConfiguration);
        final ServletSessionConfig servletSessionConfig = new ServletSessionConfig();
        servletSessionConfig.setHttpOnly(true).setSecure(configuration.http.secureCookies);

        if (configuration.http.restProcessorEnabled) {
            DeploymentInfo di = createProcessorDeploymentInfo(configuration.http.getContextRoot(), processor, identityManager);
            di.setDefaultSessionTimeout(Long.valueOf(etmConfiguration.getSessionTimeout() / 1000).intValue());
            di.setServletSessionConfig(servletSessionConfig);
            DeploymentManager manager = container.addDeployment(di);
            manager.deploy();
            manager.getDeployment().getSessionManager().registerSessionListener(sessionListenerAuditLogger);
            try {
                root.addPrefixPath(
                        di.getContextPath(),
                        Handlers.requestLimitingHandler(
                                configuration.http.restProcessorMaxConcurrentRequests,
                                configuration.http.restProcessorMaxQueuedRequests,
                                new SessionAttachmentHandler(
                                        manager.start(),
                                        manager.getDeployment().getSessionManager(),
                                        manager.getDeployment().getServletContext().getSessionConfig()
                                )
                        )
                );
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Bound rest processor to '" + di.getContextPath() + "'.");
                }
            } catch (ServletException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Error deploying rest processor", e);
                }
            }
        }
        if (configuration.http.guiEnabled) {
            DeploymentInfo di = createGuiDeploymentInfo(configuration.http.getContextRoot(), dataRepository, identityManager, etmConfiguration);
            di.setDefaultSessionTimeout(Long.valueOf(etmConfiguration.getSessionTimeout() / 1000).intValue());
            di.setServletSessionConfig(servletSessionConfig);
            DeploymentManager manager = container.addDeployment(di);
            manager.deploy();
            manager.getDeployment().getSessionManager().registerSessionListener(sessionListenerAuditLogger);
            try {
                EncodingHandler encodigHandler = new EncodingHandler(new ContentEncodingRepository()
                        .addEncodingHandler("gzip", new GzipEncodingProvider(), 100, Predicates.maxContentSize(1024))
                        .addEncodingHandler("deflate", new DeflateEncodingProvider(), 50, Predicates.maxContentSize(1024)))
                        .setNext(manager.start());
                root.addPrefixPath(
                        di.getContextPath(),
                        Handlers.requestLimitingHandler(
                                configuration.http.guiMaxConcurrentRequests,
                                configuration.http.guiMaxQueuedRequests,
                                new SessionAttachmentHandler(
                                        encodigHandler,
                                        manager.getDeployment().getSessionManager(),
                                        manager.getDeployment().getServletContext().getSessionConfig()
                                )
                        )
                );
                root.addExactPath("/favicon.ico", new ResourceHandler(new FavIconResourceSupplier()));
                root.addExactPath("/", new RedirectHandler(configuration.http.getContextRoot() + "gui/"));
                root.addExactPath(configuration.http.getContextRoot(), new RedirectHandler(configuration.http.getContextRoot() + "gui/"));
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Bound GUI to '" + di.getContextPath() + "'.");
                }
            } catch (ServletException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Error deploying GUI", e);
                }
            }
        }
        // Add the healthcheck servlet
        try {
            HealthCheckServlet.initialize(dataRepository);
            DeploymentInfo di = Servlets.deployment()
                    .setClassLoader(HealthCheckServlet.class.getClassLoader())
                    .setContextPath(configuration.http.getContextRoot())
                    .setDeploymentName("Health check")
                    .addServlets(Servlets.servlet("healthCheckServlet", HealthCheckServlet.class)
                            .addMapping("/status"));
            DeploymentManager manager = container.addDeployment(di);
            manager.deploy();
            root.addPrefixPath(di.getContextPath(), Handlers.requestLimitingHandler(10, 10, manager.start()));
        } catch (ServletException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Error deploying health check servlet", e);
            }
        }
    }

    public void start() {
        if (!this.started) {
            this.server.start();
            this.started = true;
        }
    }

    public void stop() {
        if (this.shutdownHandler != null) {
            this.shutdownHandler.shutdown();
            try {
                this.shutdownHandler.awaitShutdown(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (this.server != null && this.started) {
            this.server.stop();
            this.started = false;
        }
    }

    private DeploymentInfo createProcessorDeploymentInfo(String contextRoot, TelemetryCommandProcessor processor, IdentityManager identityManager) {
        RestTelemetryEventProcessorApplication processorApplication = new RestTelemetryEventProcessorApplication(processor);
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplication(processorApplication);
        DeploymentInfo di = undertowRestDeployment(deployment, "/*");
        di.setContextPath(contextRoot + "rest/processor/");
        di.setSessionManagerFactory(this.sessionManagerFactory);
        di.getAuthenticationMechanisms().put(ApiKeyAuthenticationMechanism.NAME, ApiKeyAuthenticationMechanism.FACTORY);
        if (identityManager != null) {
            deployment.setSecurityEnabled(true);
            di.addSecurityConstraint(new SecurityConstraint()
                    .addRolesAllowed(SecurityRoles.ETM_EVENT_WRITE, SecurityRoles.ETM_EVENT_READ_WRITE)
                    .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/*")));
            di.addSecurityRoles(SecurityRoles.ETM_EVENT_WRITE, SecurityRoles.ETM_EVENT_READ_WRITE);
            di.setIdentityManager(identityManager);
            di.setLoginConfig(new LoginConfig(HttpServletRequest.BASIC_AUTH, "Enterprise Telemetry Monitor").addFirstAuthMethod(ApiKeyAuthenticationMechanism.NAME));
        }
        di.setClassLoader(processorApplication.getClass().getClassLoader());
        di.setDeploymentName("Rest event processor - " + di.getContextPath());

        // Add the logout servlet
        ServletInfo logoutServlet = Servlets.servlet("LogoutServlet", LogoutServlet.class)
                .setAsyncSupported(false)
                .addMapping("/logout");
        di.addServlet(logoutServlet);
        return di;
    }

    private DeploymentInfo createGuiDeploymentInfo(String contextRoot, DataRepository dataRepository, IdentityManager identityManager, EtmConfiguration etmConfiguration) {
        RestGuiApplication guiApplication = new RestGuiApplication(dataRepository, etmConfiguration);
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplication(guiApplication);
        deployment.getProviderClasses().add(EtmExceptionMapper.class.getName());
        DeploymentInfo di = undertowRestDeployment(deployment, "rest/*");
        di.setSessionManagerFactory(this.sessionManagerFactory);
        di.addInnerHandlerChainWrapper(handler -> new ChangePasswordHandler(contextRoot + "gui/", handler));
        di.addWelcomePage("index.html");
        di.setContextPath(contextRoot + "gui/");
        deployment.setSecurityEnabled(true);
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.ALL_ROLES_ARRAY)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/").addUrlPattern("/index.html").addUrlPattern("/preferences/*").addUrlPattern("/rest/user/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WITHOUT_PAYLOAD, SecurityRoles.ETM_EVENT_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/search/*").addUrlPattern("/rest/search/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.USER_DASHBOARD_READ_WRITE, SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/dashboard/graphs.html").addUrlPattern("/dashboard/dashboards.html").addUrlPattern("/rest/dashboard/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.USER_SIGNAL_READ_WRITE, SecurityRoles.GROUP_SIGNAL_READ, SecurityRoles.GROUP_SIGNAL_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/signal/signals.html").addUrlPattern("/rest/signal/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.IIB_NODE_READ, SecurityRoles.IIB_NODE_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/iib/nodes.html").addUrlPattern("/rest/iib/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.IIB_EVENT_READ, SecurityRoles.IIB_EVENT_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/iib/events.html").addUrlPattern("/rest/iib/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.AUDIT_LOG_READ)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/auditlogs.html").addUrlPattern("/rest/audit/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.CLUSTER_SETTINGS_READ, SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/cluster.html").addUrlPattern("/rest/settings/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.IMPORT_PROFILES_READ, SecurityRoles.IMPORT_PROFILES_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/import_profiles.html").addUrlPattern("/rest/settings/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.GROUP_SETTINGS_READ, SecurityRoles.GROUP_SETTINGS_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/groups.html").addUrlPattern("/rest/settings/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.INDEX_STATISTICS_READ)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/indexstats.html").addUrlPattern("/rest/settings/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.LICENSE_READ, SecurityRoles.LICENSE_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/license.html").addUrlPattern("/rest/settings/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.NODE_SETTINGS_READ, SecurityRoles.NODE_SETTINGS_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/nodes.html").addUrlPattern("/rest/settings/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.PARSER_SETTINGS_READ, SecurityRoles.PARSER_SETTINGS_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/parsers.html").addUrlPattern("/rest/settings/*")));
        di.addSecurityConstraint(new SecurityConstraint()
                .addRolesAllowed(SecurityRoles.USER_SETTINGS_READ, SecurityRoles.USER_SETTINGS_READ_WRITE)
                .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/users.html").addUrlPattern("/rest/settings/*")));
        di.addSecurityRoles(SecurityRoles.ALL_ROLES_ARRAY);
        di.setIdentityManager(identityManager);
        di.setLoginConfig(new LoginConfig("FORM", "Enterprise Telemetry Monitor", "/login/login.html", "/login/login-error.html"));
        di.setClassLoader(guiApplication.getClass().getClassLoader());
        di.setResourceManager(new MenuAwareClassPathResourceManager(etmConfiguration, guiApplication.getClass().getClassLoader(), "com/jecstar/etm/gui/resources/"));
        di.setInvalidateSessionOnLogout(true);
        di.setDeploymentName("GUI - " + di.getContextPath());
        // Add the logout servlet.
        ServletInfo logoutServlet = Servlets.servlet("LogoutServlet", LogoutServlet.class)
                .setAsyncSupported(false)
                .addMapping("/logout");
        di.addServlet(logoutServlet);
        return di;
    }

    private DeploymentInfo undertowRestDeployment(ResteasyDeployment deployment, String mapping) {
        if (mapping == null) {
            mapping = "*";
        }
        String prefix = null;
        if (!mapping.equals("/*")) {
            prefix = mapping.substring(0, mapping.length() - 2);
        }
        ServletInfo resteasyServlet = Servlets.servlet("ResteasyServlet", HttpServletDispatcher.class).setAsyncSupported(true)
                .setLoadOnStartup(1).addMapping(mapping);
        if (prefix != null) {
            resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);
        }
        resteasyServlet.addInitParam("resteasy.logger.type", "SLF4J");
        return new DeploymentInfo().addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
                .addServlet(resteasyServlet);
    }
}
