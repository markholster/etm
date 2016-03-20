package com.jecstar.etm.launcher.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletException;

import org.elasticsearch.client.Client;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.gui.rest.RestGuiApplication;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;
import com.jecstar.etm.processor.rest.RestTelemetryEventProcessorApplication;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.InMemorySingleSignOnManager;
import io.undertow.security.impl.SingleSignOnAuthenticationMechanism;
import io.undertow.security.impl.SingleSignOnManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
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

	public HttpServer(final IdentityManager identityManager, Configuration configuration, TelemetryCommandProcessor processor, Client client) {;
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
				SSLContext sslContext;
				try {
					sslContext = createSslContext(this.configuration);
					builder.addHttpsListener(this.configuration.getHttpsPort(), this.configuration.bindingAddress, sslContext);
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
		if (this.configuration.restProcessorEnabled) {
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
		if (this.configuration.guiEnabled) {
			DeploymentInfo di = createGuiDeploymentInfo(client, identityManager);
			DeploymentManager manager = container.addDeployment(di);
			manager.deploy();
			try {
				HttpHandler httpHandler = manager.start();
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
		if (identityManager != null) {
			deployment.setSecurityEnabled(true);
			di.addSecurityConstraint(new SecurityConstraint().addRolesAllowed("admin", "processor").addWebResourceCollection(new WebResourceCollection().addUrlPattern("/*")));
			di.addSecurityRoles("admin", "processor");
			di.setIdentityManager(identityManager);
			di.addAuthenticationMechanism("SSO", new ImmediateAuthenticationMechanismFactory(new SingleSignOnAuthenticationMechanism(this.singleSignOnManager, identityManager)));
			di.setLoginConfig(new LoginConfig("BASIC","Enterprise Telemetry Monitor").addFirstAuthMethod("SSO"));
		}
		di.setClassLoader(processorApplication.getClass().getClassLoader());
		di.setContextPath("/rest/processor/");
		di.setDeploymentName("Rest event processor - " + di.getContextPath());
		return di;
	}
	
	private DeploymentInfo createGuiDeploymentInfo(Client client, IdentityManager identityManager) {
		RestGuiApplication guiApplication = new RestGuiApplication(client);
		ResteasyDeployment deployment = new ResteasyDeployment();
		deployment.setApplication(guiApplication);
		DeploymentInfo di = undertowRestDeployment(deployment, "/rest/");
		di.addWelcomePage("index.html");
		// TODO, even nadenken of dit uberhaupt zonder inloggen kan...
		if (identityManager != null) {
			deployment.setSecurityEnabled(true);
			// TODO add the uri of all rest interfaces to the appropriated roles.
			di.addSecurityConstraint(new SecurityConstraint().addRolesAllowed("admin","searcher").addWebResourceCollection(new WebResourceCollection().addUrlPattern("/search/*").addUrlPattern("/rest/*")));
			di.addSecurityRoles("admin", "searcher");
			di.setIdentityManager(identityManager);
			di.addAuthenticationMechanism("SSO", new ImmediateAuthenticationMechanismFactory(new SingleSignOnAuthenticationMechanism(this.singleSignOnManager, identityManager)));
			di.setLoginConfig(new LoginConfig("FORM","Enterprise Telemetry Monitor", "/login/login.html", "/login/login-error.html").addFirstAuthMethod("SSO"));
		}
		di.setClassLoader(guiApplication.getClass().getClassLoader());
		di.setContextPath("/gui");
		di.setResourceManager(new ClassPathResourceManager(guiApplication.getClass().getClassLoader(), "com/jecstar/etm/gui/resources/"));
		di.setDeploymentName("GUI - " + di.getContextPath());
		return di;
	}

	private SSLContext createSslContext(Configuration configuration) throws KeyManagementException, NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableKeyException {
		KeyStore keyStore = loadKeyStore(configuration.http.sslKeystoreLocation, configuration.http.sslKeystoreType, configuration.http.sslKeystorePassword);
		KeyStore trustStore = loadKeyStore(configuration.http.sslTruststoreLocation, configuration.http.sslTruststoreType, configuration.http.sslTruststorePassword);
		KeyManager[] keyManagers = buildKeyManagers(keyStore, configuration.http.sslKeystorePassword);
		TrustManager[] trustManagers = buildTrustManagers(configuration.http.sslTruststoreLocation == null ? null : trustStore, configuration.http.sslTruststorePassword);
		if (keyManagers == null || trustManagers == null) {
			return null;
		}
		SSLContext sslContext = SSLContext.getInstance(configuration.http.sslProtocol);
		sslContext.init(keyManagers, trustManagers, null);
		return sslContext;
	}

	private KeyStore loadKeyStore(final File location, String type, final String storePassword)
			throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		try (final InputStream stream = location == null ? null : new FileInputStream(location);) {
			KeyStore loadedKeystore = KeyStore.getInstance(type);
			loadedKeystore.load(stream, storePassword == null ? null : storePassword.toCharArray());
			return loadedKeystore;
		}
	}

	private KeyManager[] buildKeyManagers(final KeyStore keyStore, final String storePassword) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, storePassword == null ? null : storePassword.toCharArray());
		return keyManagerFactory.getKeyManagers();
	}

	private TrustManager[] buildTrustManagers(final KeyStore trustStore, final String storePassword) throws KeyStoreException, NoSuchAlgorithmException {
		if (trustStore == null) {
			return new TrustManager[] { new TrustAllTrustManager() };
		}
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);
		return trustManagerFactory.getTrustManagers();
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
