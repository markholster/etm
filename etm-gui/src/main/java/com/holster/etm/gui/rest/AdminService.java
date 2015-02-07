package com.holster.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.core.configuration.Node;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.jee.configurator.core.GuiConfiguration;

@Path("/admin")
public class AdminService {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(AdminService.class);

	@GuiConfiguration
	@Inject
	private EtmConfiguration configuration;

	private final JsonFactory jsonFactory = new JsonFactory();

	@GET
	@Path("/nodes")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getEndpointConfigs() {
		try {
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartArray();
	        List<Node> nodes = this.configuration.getNodes();
	        for (Node node : nodes) {
	        	generator.writeStartObject();
	        	generator.writeStringField("name", node.getName());
	        	generator.writeBooleanField("active", node.isActive());
	        	generator.writeEndObject();
	        }
	        generator.writeEndArray();
	        generator.close();
	        return writer.toString();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to get endpoint configs.", e);
        	}       
        }
		return null;	
	}
}
