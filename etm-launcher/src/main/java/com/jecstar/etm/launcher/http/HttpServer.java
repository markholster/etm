package com.jecstar.etm.launcher.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;

import org.elasticsearch.client.Client;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.wadl.ResteasyWadlServlet;

import com.jecstar.etm.gui.rest.EtmExceptionMapper;
import com.jecstar.etm.gui.rest.RestGuiApplication;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.rest.RestTelemetryEventProcessorApplication;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipalRole;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.ssl.SSLContextBuilder;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.predicate.Predicates;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.InMemorySingleSignOnManager;
import io.undertow.security.impl.SingleSignOnAuthenticationMechanism;
import io.undertow.security.impl.SingleSignOnManager;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.util.ImmediateAuthenticationMechanismFactory;

public class HttpServer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(HttpServer.class);

	private Configuration configuration;
	private Undertow server;
	private GracefulShutdownHandler shutdownHandler;
	private boolean started;
	private final SingleSignOnManager singleSignOnManager = new InMemorySingleSignOnManager();

	public HttpServer(final IdentityManager identityManager, Configuration configuration, EtmConfiguration etmConfiguration, TelemetryCommandProcessor processor, Client client) {;
		this.configuration = configuration;
		final PathHandler root = Handlers.path();
		this.shutdownHandler = Handlers.gracefulShutdown(Handlers.requestLimitingHandler(configuration.http.maxConcurrentRequests, configuration.http.maxQueuedRequests, root));
		final ServletContainer container = ServletContainer.Factory.newInstance();
		Builder builder = Undertow.builder();
		if (this.configuration.getHttpPort() > 0) {
			builder.addHttpListener(this.configuration.getHttpPort(), this.configuration.bindingAddress);
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Binding http listener to '" + this.configuration.bindingAddress + ":" + this.configuration.getHttpPort() + "'");
			}
		}
		if (this.configuration.getHttpsPort() > 0) {
			if (this.configuration.http.sslKeystoreLocation == null) {
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
							configuration.http.sslTruststorePassword == null ? null : configuration.http.sslKeystorePassword.toCharArray());
					
					builder.addHttpsListener(this.configuration.getHttpsPort(), this.configuration.bindingAddress, sslContext);
					builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Binding https listener to '" + this.configuration.bindingAddress + ":" + this.configuration.getHttpsPort() + "'");
					}
				} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
					if (log.isErrorLevelEnabled()) {
						log.logErrorMessage("Unable to create SSL context. Https listener not started.", e);
					}
				}
			}
		}
		this.server = builder.setHandler(root).build();
		if (this.configuration.http.restProcessorEnabled) {
			DeploymentInfo di = createProcessorDeploymentInfo(processor, null);
			DeploymentManager manager = container.addDeployment(di);
			manager.deploy();
			try {
				HttpHandler httpHandler = manager.start();
				root.addPrefixPath(di.getContextPath(), new SessionAttachmentHandler(httpHandler, manager.getDeployment().getSessionManager(), manager.getDeployment().getServletContext().getSessionConfig()));
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Bound rest processor to '" + di.getContextPath() + "'.");
				}
			} catch (ServletException e) {
				if (log.isErrorLevelEnabled()) {
					log.logErrorMessage("Error deploying rest processor", e);
				}
			}
		}
		if (this.configuration.http.guiEnabled) {
			DeploymentInfo di = createGuiDeploymentInfo(client, identityManager, etmConfiguration);
			DeploymentManager manager = container.addDeployment(di);
			manager.deploy();
			try {
				EncodingHandler httpHandler = new EncodingHandler(new ContentEncodingRepository()
					.addEncodingHandler("gzip", new GzipEncodingProvider(), 100, Predicates.maxContentSize(1024))
					.addEncodingHandler("deflate", new DeflateEncodingProvider(), 50, Predicates.maxContentSize(1024)))
					.setNext(manager.start());
				root.addPrefixPath(di.getContextPath(), new SessionAttachmentHandler(httpHandler, manager.getDeployment().getSessionManager(), manager.getDeployment().getServletContext().getSessionConfig()));
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Bound GUI to '" + di.getContextPath() + "'.");
				}
			} catch (ServletException e) {
				if (log.isErrorLevelEnabled()) {
					log.logErrorMessage("Error deploying GUI", e);
				}
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

	private DeploymentInfo createProcessorDeploymentInfo(TelemetryCommandProcessor processor, IdentityManager identityManager) {
		RestTelemetryEventProcessorApplication processorApplication = new RestTelemetryEventProcessorApplication(processor);
		ResteasyDeployment deployment = new ResteasyDeployment();
		deployment.setApplication(processorApplication);
		DeploymentInfo di = undertowRestDeployment(deployment, "/");
		di.setContextPath("/rest/processor/");
		if (identityManager != null) {
			deployment.setSecurityEnabled(true);
			di.addSecurityConstraint(new SecurityConstraint()
					.addRolesAllowed(EtmPrincipalRole.ADMIN.getRoleName(), EtmPrincipalRole.PROCESSOR.getRoleName())
					.addWebResourceCollection(new WebResourceCollection().addUrlPattern("/*")));
			di.addSecurityRoles(EtmPrincipalRole.ADMIN.getRoleName(), EtmPrincipalRole.PROCESSOR.getRoleName());
			di.setIdentityManager(identityManager);
			di.addAuthenticationMechanism("SSO", new ImmediateAuthenticationMechanismFactory(new SingleSignOnAuthenticationMechanism(this.singleSignOnManager, identityManager).setPath(di.getContextPath())));
			di.setLoginConfig(new LoginConfig("BASIC","Enterprise Telemetry Monitor").addFirstAuthMethod("SSO"));
		}
		di.setClassLoader(processorApplication.getClass().getClassLoader());
		di.setDeploymentName("Rest event processor - " + di.getContextPath());
		
		// Add wadl generation support
		ServletInfo resteasyWadlServlet = Servlets.servlet("ResteasyWadlServlet", ResteasyWadlServlet.class)
	                .setAsyncSupported(false)
	                .setLoadOnStartup(1)
	                .addMapping("/application.wadl");	
		di.addServlet(resteasyWadlServlet);

		// Add the logout servlet
		ServletInfo logoutServlet = Servlets.servlet("LogoutServlet", LogoutServlet.class)
                .setAsyncSupported(false)
                .addMapping("/logout");
		di.addServlet(logoutServlet);
		return di;
	}
	
	private DeploymentInfo createGuiDeploymentInfo(Client client, IdentityManager identityManager, EtmConfiguration etmConfiguration) {
		RestGuiApplication guiApplication = new RestGuiApplication(client, etmConfiguration);
		ResteasyDeployment deployment = new ResteasyDeployment();
		deployment.setApplication(guiApplication);
		deployment.getProviderClasses().add(EtmExceptionMapper.class.getName());
		DeploymentInfo di = undertowRestDeployment(deployment, "/rest/");
		di.addInnerHandlerChainWrapper(new HandlerWrapper() {
			@Override
			public HttpHandler wrap(HttpHandler handler) {
				return new ChangePasswordHandler("/gui/", handler);
			}
		});
		di.addWelcomePage("index.html");
		di.setContextPath("/gui");
		deployment.setSecurityEnabled(true);
		di.addSecurityConstraint(new SecurityConstraint()
					.addRolesAllowed(EtmPrincipalRole.ADMIN.getRoleName(), EtmPrincipalRole.SEARCHER.getRoleName(), EtmPrincipalRole.CONTROLLER.getRoleName(), EtmPrincipalRole.IIB_ADMIN.getRoleName())
					.addWebResourceCollection(new WebResourceCollection().addUrlPattern("/").addUrlPattern("/index.html").addUrlPattern("/preferences/*").addUrlPattern("/rest/user/*")));
		di.addSecurityConstraint(new SecurityConstraint()
				.addRolesAllowed(EtmPrincipalRole.ADMIN.getRoleName(), EtmPrincipalRole.SEARCHER.getRoleName())
				.addWebResourceCollection(new WebResourceCollection().addUrlPattern("/search/*").addUrlPattern("/rest/search/*")));
		di.addSecurityConstraint(new SecurityConstraint()
				.addRolesAllowed(EtmPrincipalRole.ADMIN.getRoleName(), EtmPrincipalRole.CONTROLLER.getRoleName())
				.addWebResourceCollection(new WebResourceCollection().addUrlPattern("/dashboard/*").addUrlPattern("/rest/dashboard/*")));
		di.addSecurityConstraint(new SecurityConstraint()
				.addRolesAllowed(EtmPrincipalRole.ADMIN.getRoleName())
				.addWebResourceCollection(new WebResourceCollection().addUrlPattern("/settings/*").addUrlPattern("/rest/settings/*")));
		di.addSecurityConstraint(new SecurityConstraint()
				.addRolesAllowed(EtmPrincipalRole.ADMIN.getRoleName(), EtmPrincipalRole.IIB_ADMIN.getRoleName())
				.addWebResourceCollection(new WebResourceCollection().addUrlPattern("/iib/*").addUrlPattern("/rest/iib/*")));
		di.addSecurityRoles(EtmPrincipalRole.ADMIN.getRoleName(), EtmPrincipalRole.SEARCHER.getRoleName(), EtmPrincipalRole.CONTROLLER.getRoleName(), EtmPrincipalRole.IIB_ADMIN.getRoleName());
		di.setIdentityManager(identityManager);
		di.addAuthenticationMechanism("SSO", new ImmediateAuthenticationMechanismFactory(new SingleSignOnAuthenticationMechanism(this.singleSignOnManager, identityManager).setPath(di.getContextPath())));
		di.setLoginConfig(new LoginConfig("FORM","Enterprise Telemetry Monitor", "/login/login.html", "/login/login-error.html").addFirstAuthMethod("SSO"));
		di.setClassLoader(guiApplication.getClass().getClassLoader());
		di.setResourceManager(new ClassPathResourceManager(guiApplication.getClass().getClassLoader(), "com/jecstar/etm/gui/resources/"));
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
			mapping = "/";
		}
		if (!mapping.startsWith("/")) {
			mapping = "/" + mapping;
		}
		if (!mapping.endsWith("/")) {
			mapping += "/";
		}
		mapping = mapping + "*";
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
