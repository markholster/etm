package com.jecstar.etm.gui.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonToken;
import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.FixedPositionExpressionParser;
import com.jecstar.etm.core.parsers.FixedValueExpressionParser;
import com.jecstar.etm.core.parsers.JsonExpressionParser;
import com.jecstar.etm.core.parsers.XPathExpressionParser;
import com.jecstar.etm.core.parsers.XsltExpressionParser;
import com.jecstar.etm.gui.rest.repository.EndpointConfiguration;
import com.jecstar.etm.gui.rest.repository.EndpointRepository;
import com.jecstar.etm.gui.rest.repository.NodeRepository;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.xpath.XPathFactoryImpl;

@Path("/admin")
public class AdminService {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(AdminService.class);

	private static final XPath xPath = new XPathFactoryImpl().newXPath();
	private static final TransformerFactory transformerFactory = new TransformerFactoryImpl();
	
	@GuiConfiguration
	@Inject
	private EtmConfiguration configuration;
	
	@Inject
	private EndpointRepository endpointRepository;
	
	@Inject
	private NodeRepository nodeRepository;


	private final JsonFactory jsonFactory = new JsonFactory();

	@GET
	@Path("/endpoints")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getEndpointNames() {
		try {
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createGenerator(writer);
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
	        JsonGenerator generator = this.jsonFactory.createGenerator(writer);
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
		this.endpointRepository.deleteEndpointConfiguration(endpointName);
        return "{ \"status\": \"success\" }";
	}
	
	@POST
	@Path("/endpoint/{endpointName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String saveEndpoint(@PathParam("endpointName") String endpointName, InputStream data) {
		try {
			JsonParser parser = this.jsonFactory.createParser(data);
			EndpointConfiguration endpointConfiguration = new EndpointConfiguration();
			JsonToken token = parser.nextToken();
			while (token != JsonToken.END_OBJECT && token != null) {
				String name = parser.getCurrentName();
				if ("name".equals(name)) {
					parser.nextToken();
					endpointConfiguration.name = parser.getText();
				} else if ("direction".equals(name)) {
					parser.nextToken();
					endpointConfiguration.direction = TelemetryEventDirection.valueOf(parser.getText()); 
				} else if ("application_parsers".equals(name)) {
					if (parser.nextToken() != JsonToken.START_ARRAY) {
						if (log.isErrorLevelEnabled()) {
							log.logErrorMessage("Unable to determine application parsers");
						}
						throw new WebApplicationException(Response.Status.BAD_REQUEST);	
					}
					parseExpressionParsers(parser, endpointConfiguration.applicationParsers, null);
				} else if ("eventname_parsers".equals(name)) {
					if (parser.nextToken() != JsonToken.START_ARRAY) {
						if (log.isErrorLevelEnabled()) {
							log.logErrorMessage("Unable to determine eventname parsers");
						}
						throw new WebApplicationException(Response.Status.BAD_REQUEST);	
					}
					parseExpressionParsers(parser, endpointConfiguration.eventNameParsers, null);
				} else if ("transactionname_parsers".equals(name)) {
					if (parser.nextToken() != JsonToken.START_ARRAY) {
						if (log.isErrorLevelEnabled()) {
							log.logErrorMessage("Unable to determine transactionname parsers");
						}
						throw new WebApplicationException(Response.Status.BAD_REQUEST);	
					}
					parseExpressionParsers(parser, endpointConfiguration.transactionNameParsers, null);
				} else if ("correlation_parsers".equals(name)) {
					if (parser.nextToken() != JsonToken.START_ARRAY) {
						if (log.isErrorLevelEnabled()) {
							log.logErrorMessage("Unable to determine correlation parsers");
						}
						throw new WebApplicationException(Response.Status.BAD_REQUEST);	
					}
					parseExpressionParsers(parser, null, endpointConfiguration.correlationParsers);
				}
				token = parser.nextToken();
			}
			this.endpointRepository.updateEnpointConfiguration(endpointConfiguration);
	        return "{ \"status\": \"success\" }";
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to update endpoint configuration '" + endpointName + "'.", e);
        	}       
        }
		return null;	
	}
	
	private void parseExpressionParsers(JsonParser parser, List<ExpressionParser> expressionParsersList, Map<String, ExpressionParser> expressionParserMap) throws JsonParseException, IOException {
		String exprName = null;
		String type = null;
		Integer line = null;
		Integer startPos = null;
		Integer endPos = null;
		String expression = null;
		
		JsonToken token = parser.nextToken();
		while (token != JsonToken.END_ARRAY && token != null) {
			String name = parser.getCurrentName();
			if ("name".equals(name)) {
				parser.nextToken();
				exprName = parser.getText();
			} else if ("type".equals(name)) {
				parser.nextToken();
				type = parser.getText();
			} else if ("line".equals(name)) {
				parser.nextToken();
				line = parser.getIntValue();
			} else if ("start_pos".equals(name)) {
				parser.nextToken();
				startPos = parser.getIntValue();
			} else if ("end_pos".equals(name)) {
				parser.nextToken();
				endPos = parser.getIntValue();
			} else if ("value".equals(name) || "expression".equals(name) || "template".equals(name) || "path".equals(name)) {
				parser.nextToken();
				expression = parser.getText();
			} 
			token = parser.nextToken();
			if (token == JsonToken.END_OBJECT) {
				ExpressionParser expressionParser = null;
				if ("fixed_position".equals(type)) {
					if (line != null) {
						line = line - 1;
					}
					if (startPos != null) {
						startPos = startPos - 1;
					}
					if (endPos != null) {
						endPos = endPos - 1;
					}
					expressionParser = new FixedPositionExpressionParser(line, startPos, endPos);
				} else if ("fixed_value".equals(type)) {
					expressionParser = new FixedValueExpressionParser(expression);
				} else if ("xpath".equals(type)) {
					expressionParser = new XPathExpressionParser(xPath, expression);
				} else if ("xslt".equals(type)) {
					expressionParser = new XsltExpressionParser(transformerFactory, expression);
				} else if ("json".equals(type)) {
					expressionParser = new JsonExpressionParser(expression);
				} else {
					if (log.isErrorLevelEnabled()) {
						log.logErrorMessage("Unable to determine expression parser type '" + type + "'.");
					}
					throw new WebApplicationException(Response.Status.BAD_REQUEST);	
				}
				if (expressionParsersList != null) {
					expressionParsersList.add(expressionParser);
				}
				if (expressionParserMap != null) {
					expressionParserMap.put(exprName, expressionParser);
				}
			}
		}					
		
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
				generator.writeNumberField("start_pos", parser.getStartIx() + 1);
			}
			if (parser.getEndIx() != null) {
				generator.writeNumberField("end_pos", parser.getEndIx() + 1);
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
		} else if (expressionParser instanceof JsonExpressionParser) {
			generator.writeStringField("type", "json");
			JsonExpressionParser parser = (JsonExpressionParser) expressionParser;
			generator.writeStringField("path", parser.getPath());
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
	        JsonGenerator generator = this.jsonFactory.createGenerator(writer);
	        generator.writeStartArray();
	        List<Node> nodes = this.nodeRepository.getNodes();
	        for (Node node : nodes) {
	        	generator.writeStartObject();
	        	generator.writeStringField("name", node.name);
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
	        JsonGenerator generator = this.jsonFactory.createGenerator(writer);
	        generator.writeStartObject();
//	        List<String> liveNodes = this.configuration.getLiveNodes();
	        Map<String, Object> nodeConfiguration = this.nodeRepository.getNodeConfiguration(nodeName);
        	generator.writeStringField("name", nodeName);
//        	generator.writeBooleanField("active", "cluster".equals(nodeName) ? true : liveNodes.contains(nodeName));
        	for (String key : nodeConfiguration.keySet()) {
        		Object value = nodeConfiguration.get(key);
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
			Map<String, Object> values = new HashMap<String, Object>();
	        JsonParser jsonParser = this.jsonFactory.createParser(json);
	        JsonToken token = jsonParser.nextToken();
	        while (token != null) {
	        	token = jsonParser.nextToken();
	        	if (JsonToken.FIELD_NAME.equals(token)) {
	        		String key = jsonParser.getCurrentName();
	        		jsonParser.nextToken();
	        		NumberType numberType = jsonParser.getNumberType();
	        		if (numberType == null) {
	        			values.put(key, jsonParser.getText());
	        		} else {
	        			switch (numberType) {
	        			case INT:
	        				values.put(key, jsonParser.getIntValue());
	        				break;
	        			case FLOAT:
	        				values.put(key, jsonParser.getFloatValue());
	        				break;
						case LONG:
							values.put(key, jsonParser.getLongValue());
							break;
						case DOUBLE:
							values.put(key, jsonParser.getDoubleValue());
							break;
						default:
							values.put(key, jsonParser.getText());
							break;
						}
	        		}
	        		
	        	}
	        }
	        if (values.size() > 0) {
	        	this.nodeRepository.update(nodeName, values);
	        }
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Error saving node configuration for node '" + nodeName + "'.", e);
        	}
        	throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
	}
}
