package com.jecstar.etm.gui.rest.services.settings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsAction;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.jecstar.etm.gui.rest.AbstractJsonService;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.configuration.License;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.json.EtmConfigurationConverterJsonImpl;

@Path("/settings")
public class SettingsService extends AbstractJsonService {
	
	private final String licenseIndexName = "etm_configuration";
	private final String licenseIndexType = "license";
	private final String licenseId = "default_configuration";
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	
	private static Client client;
	private static EtmConfiguration etmConfiguration;
	
	public static void initialize(Client client, EtmConfiguration etmConfiguration) {
		SettingsService.client = client;
		SettingsService.etmConfiguration = etmConfiguration;
	}
	
	@GET
	@Path("/license")
	@Produces(MediaType.APPLICATION_JSON)
	public String getLicense() {
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		License license = etmConfiguration.getLicense();
		if (license == null) {
			return null;
		}
		boolean added = false;
		StringBuilder result = new StringBuilder();
		result.append("{");
		added = addStringElementToJsonBuffer("owner", license.getOwner(), result, !added) || added;
		added = addLongElementToJsonBuffer("expiration_date", license.getExpiryDate().toEpochMilli(), result, !added) || added;
		added = addStringElementToJsonBuffer("time_zone", etmPrincipal.getTimeZone().getID(), result, !added) || added;
		added = addStringElementToJsonBuffer("license_type", license.getLicenseType().name(), result, !added) || added;
		result.append("}");
		return result.toString();
	}
	
	@PUT
	@Path("/license")
	@Produces(MediaType.APPLICATION_JSON)
	public String getLicense(String json) {
		Map<String, Object> requestValues = toMap(json); 
		String licenseKey = getString("key", requestValues);
		etmConfiguration.setLicenseKey(licenseKey);
		Map<String, Object> values = new HashMap<>();
		values.put(this.etmConfigurationConverter.getTags().getLicenseTag(), licenseKey);
		client.prepareUpdate(this.licenseIndexName, this.licenseIndexType, this.licenseId)
			.setDoc(values)
			.setDocAsUpsert(true)
			.setConsistencyLevel(WriteConsistencyLevel.valueOf(etmConfiguration.getWriteConsistency().name()))
			.setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
			.setRetryOnConflict(etmConfiguration.getRetryOnConflictCount())
			.get();
		EtmPrincipal etmPrincipal = getEtmPrincipal();
		License license = etmConfiguration.getLicense();
		boolean added = false;
		StringBuilder result = new StringBuilder();
		result.append("{");
		added = addStringElementToJsonBuffer("status", "success", result, !added) || added;
		added = addStringElementToJsonBuffer("owner", license.getOwner(), result, !added) || added;
		added = addLongElementToJsonBuffer("expiration_date", license.getExpiryDate().toEpochMilli(), result, !added) || added;
		added = addStringElementToJsonBuffer("time_zone", etmPrincipal.getTimeZone().getID(), result, !added) || added;
		added = addStringElementToJsonBuffer("license_type", license.getLicenseType().name(), result, !added) || added;
		result.append("}");
		return result.toString();
	}
	
	@GET
	@Path("/nodes/es")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEsNodes() {
		NodesStatsResponse nodesStatsResponse = new NodesStatsRequestBuilder(client, NodesStatsAction.INSTANCE).all().setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout())).get();
		try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            nodesStatsResponse.toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
        	throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
	}
}
