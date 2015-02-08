package com.holster.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

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
	
	@GET
	@Path("/node/{nodeName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getNodeConfiguration(@PathParam("nodeName") String nodeName) {
		try {
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        List<String> liveNodes = this.configuration.getLiveNodes();
	        Properties properties = this.configuration.getNodeConfiguration("cluster".equals(nodeName) ? null : nodeName);
        	generator.writeStringField("name", nodeName);
        	generator.writeBooleanField("active", "cluster".equals(nodeName) ? true : liveNodes.contains(nodeName));
        	for (Object key : properties.keySet()) {
        		Object value = properties.get(key);
        		try {
        			Long longValue = Long.valueOf(value.toString());
        			generator.writeNumberField(key.toString(), longValue);
        		} catch (NumberFormatException e) {
        			if ("true".equals(value.toString()) || "false".equals(value.toString())) {
        				generator.writeBooleanField(key.toString(), new Boolean(value.toString()));
        			} else {
        				generator.writeStringField(key.toString(), value.toString());
        			}
        		}
        		
        		
        	}
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to get endpoint configs.", e);
        	}       
        }
		return null;	
	}
	
	@POST
	@Path("/node/{nodeName}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateNodeConfiguration(@PathParam("nodeName") String nodeName, String json) {
		try {
	        JsonParser jsonParser = this.jsonFactory.createJsonParser(json);
	        JsonToken token = jsonParser.nextToken();
	        while (token != null) {
	        	token = jsonParser.nextToken();
	        }
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	}
}
