package com.jecstar.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@Path("/system")
public class SystemService {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(AdminService.class);


	@GuiConfiguration
	@Inject
	private EtmConfiguration configuration;

	private final JsonFactory jsonFactory = new JsonFactory();
	
	@GET
	@Path("/info")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getEndpointNames() {
		try {
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        generator.writeStringField("companyName", this.configuration.getCompanyName() != null ? this.configuration.getCompanyName() : "Unknown");
	        if (this.configuration.getLicenseExpriy() != null) {
	        	generator.writeNumberField("licenseExpiry", this.configuration.getLicenseExpriy().getTime());
	        }
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to get system info.", e);
        	}       
        }
		return null;	
	}
	
	@POST
	@Path("/license")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void updateLicense(String json) {
		try {
			JsonParser jsonParser = this.jsonFactory.createJsonParser(json);
			JsonToken token = jsonParser.nextToken();
			while (token != null) {
				token = jsonParser.nextToken();
				if (JsonToken.FIELD_NAME.equals(token)) {
					String key = jsonParser.getCurrentName();
					jsonParser.nextToken();
					if ("license.key".equals(key)) {
						String licenseKey = jsonParser.getText();
						this.configuration.setLicenseKey(licenseKey);
					}
				}
			}
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Error saving license key", e);
			}
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
}
