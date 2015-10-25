package com.jecstar.etm.launcher;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.jaxrs.JAXRSArchive;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.rest.RestTelemetryEventProcessor;

public class Main {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Main.class);

	
	public static void main(String[] args) {
		try {
			Container container = new Container();
			JAXRSArchive deployment = ShrinkWrap.create(JAXRSArchive.class, "etm-processor-rest.war");
	        deployment.addClass(RestTelemetryEventProcessor.class);
	        deployment.addAllDependencies();
			container.start();
			
			container.deploy(deployment);
		} catch (Exception e) {
			log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
		}
	}
}
