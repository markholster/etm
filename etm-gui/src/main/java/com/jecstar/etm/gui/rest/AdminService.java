package com.jecstar.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.configuration.Node;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.FixedPositionExpressionParser;
import com.jecstar.etm.core.parsers.FixedValueExpressionParser;
import com.jecstar.etm.core.parsers.XPathExpressionParser;
import com.jecstar.etm.core.parsers.XsltExpressionParser;
import com.jecstar.etm.gui.rest.repository.EndpointConfiguration;
import com.jecstar.etm.gui.rest.repository.EndpointRepository;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@Path("/admin")
public class AdminService {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(AdminService.class);

	@GuiConfiguration
	@Inject
	private EtmConfiguration configuration;
	
	@Inject
	private EndpointRepository endpointRepository;

	private final JsonFactory jsonFactory = new JsonFactory();

	@GET
	@Path("/endpoints")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getEndpointNames() {
		try {
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartArray();
	        List<String> endpointNames = this.endpointRepository.getEndpointNames();
	        for (String endpointName: endpointNames) {
	        	generator.writeString(endpointName);
	        }
	        generator.writeEndArray();
	        generator.close();
	        return writer.toString();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to get endpoint names.", e);
        	}       
        }
		return null;	
	}
	
	@GET
	@Path("/endpoint/{endpointName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getEndpoint(@PathParam("endpointName") String endpointName) {
		try {
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        EndpointConfiguration endpointConfiguration = this.endpointRepository.getEndpointConfiguration(endpointName);
	        generator.writeStringField("name", endpointConfiguration.name);
	        if (endpointConfiguration.direction != null) {
	        	generator.writeStringField("direction", endpointConfiguration.direction.name());
	        }
        	if (endpointConfiguration.applicationParsers != null && endpointConfiguration.applicationParsers.size() > 0) {
        		generator.writeArrayFieldStart("application_parsers");
        		for (ExpressionParser expressionParser : endpointConfiguration.applicationParsers) {
        			writeExpressionParser(generator, null, expressionParser);
        		}
        		generator.writeEndArray();
        	}
        	if (endpointConfiguration.eventNameParsers != null && endpointConfiguration.eventNameParsers.size() > 0) {
        		generator.writeArrayFieldStart("eventname_parsers");
        		for (ExpressionParser expressionParser : endpointConfiguration.eventNameParsers) {
        			writeExpressionParser(generator, null, expressionParser);
        		}
        		generator.writeEndArray();
        	}
        	if (endpointConfiguration.transactionNameParsers != null && endpointConfiguration.transactionNameParsers.size() > 0) {
        		generator.writeArrayFieldStart("transactionname_parsers");
        		for (ExpressionParser expressionParser : endpointConfiguration.transactionNameParsers) {
        			writeExpressionParser(generator, null, expressionParser);
        		}
        		generator.writeEndArray();
        	}
        	if (endpointConfiguration.correlationParsers != null && endpointConfiguration.correlationParsers.size() > 0) {
        		generator.writeArrayFieldStart("correlation_parsers");
        		for (String key : endpointConfiguration.correlationParsers.keySet()) {
        			writeExpressionParser(generator, key, endpointConfiguration.correlationParsers.get(key));
        		}
        		generator.writeEndArray();
        	}
        	// TODO SLA's
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to get endpoint configuration '" + endpointName + "'.", e);
        	}       
        }
		return null;	
	}
	
	@DELETE
	@Path("/endpoint/{endpointName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String deleteEndpoint(@PathParam("endpointName") String endpointName) {
		try {
			this.endpointRepository.deleteEndpointConfiguration(endpointName);
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to delete endpoint configuration '" + endpointName + "'.", e);
        	}       
        }
		return null;	
	}
	
	@POST
	@Path("/endpoint/{endpointName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String saveEndpoint(@PathParam("endpointName") String endpointName) {
		try {
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to update endpoint configuration '" + endpointName + "'.", e);
        	}       
        }
		return null;	
	}
	
	private void writeExpressionParser(JsonGenerator generator, String key, ExpressionParser expressionParser) throws JsonGenerationException, IOException {
		generator.writeStartObject();
		if (key != null) {
			generator.writeStringField("name", key);
		}
		if (expressionParser instanceof FixedPositionExpressionParser) {
			generator.writeStringField("type", "fixed_position");
			FixedPositionExpressionParser parser = (FixedPositionExpressionParser) expressionParser;
			if (parser.getLineIx() != null) {
				generator.writeNumberField("line", parser.getLineIx() + 1);
			}
			if (parser.getStartIx() != null) {
				generator.writeNumberField("start_pos", parser.getStartIx());
			}
			if (parser.getEndIx() != null) {
				generator.writeNumberField("end_pos", parser.getEndIx());
			}
		} else if (expressionParser instanceof FixedValueExpressionParser) {
			generator.writeStringField("type", "fixed_value");
			FixedValueExpressionParser parser = (FixedValueExpressionParser) expressionParser;
			generator.writeStringField("value", parser.getValue());
		} else if (expressionParser instanceof XPathExpressionParser) {
			generator.writeStringField("type", "xpath");
			XPathExpressionParser parser = (XPathExpressionParser) expressionParser;
			generator.writeStringField("expression", parser.getExpression());
		} else if (expressionParser instanceof XsltExpressionParser) {
			generator.writeStringField("type", "xslt");
			XsltExpressionParser parser = (XsltExpressionParser) expressionParser;
			generator.writeStringField("template", parser.getTemplate());
		} else {
			generator.writeStringField("type", "unknown");
		}
		generator.writeEndObject();
		
	}
	
	@GET
	@Path("/nodes")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getNodes() {
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
        		log.logErrorMessage("Unable to get nodes.", e);
        	}       
        }
		return null;	
	}
	
	@GET
	@Path("/node/{nodeName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getNode(@PathParam("nodeName") String nodeName) {
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
        		log.logErrorMessage("Unable to get node configurations.", e);
        	}       
        }
		return null;	
	}
	
	@POST
	@Path("/node/{nodeName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void updateNode(@PathParam("nodeName") String nodeName, String json) {
		try {
			Properties properties = new Properties();
	        JsonParser jsonParser = this.jsonFactory.createJsonParser(json);
	        JsonToken token = jsonParser.nextToken();
	        while (token != null) {
	        	token = jsonParser.nextToken();
	        	if (JsonToken.FIELD_NAME.equals(token)) {
	        		String key = jsonParser.getCurrentName();
	        		jsonParser.nextToken();
	        		properties.setProperty(key, jsonParser.getText());
	        	}
	        }
	        if (properties.size() > 0) {
	        	this.configuration.update("cluster".equals(nodeName) ? null : nodeName, properties);
	        }
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Error saving node configuration for node '" + nodeName + "'.", e);
        	}
        	throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
	}
}
