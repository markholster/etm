package com.jecstar.etm.launcher;

import static io.undertow.servlet.Servlets.servlet;

import javax.servlet.ServletException;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;
import com.jecstar.etm.processor.rest.RestTelemetryEventProcessorApplication;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

public class HttpServer {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(HttpServer.class);

	private Configuration configuration;
	private Undertow server;
	private GracefulShutdownHandler shutdownHandler;
	private boolean started;

	public HttpServer(Configuration configuration, TelemetryCommandProcessor processor) {
		this.configuration = configuration;
		final PathHandler root = Handlers.path();
		this.shutdownHandler = Handlers.gracefulShutdown(root);
		final ServletContainer container = ServletContainer.Factory.newInstance();
		this.server = Undertow.builder()
				.addHttpListener(this.configuration.httpPort + this.configuration.bindingPortOffset,
						this.configuration.bindingAddress)
				.setHandler(root).build();
		if (this.configuration.restProcessorEnabled) {
			RestTelemetryEventProcessorApplication processorApplication = new RestTelemetryEventProcessorApplication(
					processor);
			ResteasyDeployment deployment = new ResteasyDeployment();
			deployment.setApplication(processorApplication);
			DeploymentInfo di = undertowRestDeployment(deployment, "/");
			di.setClassLoader(processorApplication.getClass().getClassLoader());
			di.setContextPath("/rest/processor/");
			di.setDeploymentName("Rest event processor - " + di.getContextPath());
			DeploymentManager manager = container.addDeployment(di);
			manager.deploy();
			try {
				root.addPrefixPath(di.getContextPath(), manager.start());
			} catch (ServletException e) {
				log.logErrorMessage("Error deploying rest processor", e);
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
	
	private static DeploymentInfo undertowRestDeployment(ResteasyDeployment deployment, String mapping) {
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
		ServletInfo resteasyServlet = servlet("ResteasyServlet", HttpServletDispatcher.class).setAsyncSupported(true)
				.setLoadOnStartup(1).addMapping(mapping);
		if (prefix != null) {
			resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);
		}
		return new DeploymentInfo().addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
				.addServlet(resteasyServlet);
	}

}
