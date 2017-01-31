package com.jecstar.etm.v1migrator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteAction;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.yaml.snakeyaml.Yaml;

import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.builders.EndpointBuilder;
import com.jecstar.etm.domain.builders.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builders.MessagingTelemetryEventBuilder;


public class Startup {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		JsonConverter jsonConverter = new JsonConverter();
		Configuration config = loadConfiguration();
		Settings settings = Settings.settingsBuilder()
		        .put("cluster.name", config.inputClusterName).build();
		Client client = TransportClient.builder().settings(settings).build()
		        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(config.inputHostname), config.inputPort));
		InsertRequestHandler requestHandler = new InsertRequestHandler(config.bulkApiLocation);
		int count = 0;
		while (true) {
			SearchResponse searchResponse = client.prepareSearch("etm_event_all")
				.addSort(SortBuilders.fieldSort("creation_time").order(SortOrder.ASC))
				.setSize(config.bulkSize)
				.get();
			BulkRequestBuilder bulkRequest = client.prepareBulk().setRefresh(true);
			SearchHits hits = searchResponse.getHits();
			if (hits.getHits() == null || hits.getHits().length == 0) {
				break;
			}
			for (SearchHit hit : hits.getHits()) {
				count++;
				Map<String, Object> source = hit.getSource();
				MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder();
				builder.setId(hit.getId());
				Long timesamp = jsonConverter.getLong("creation_time", source);
				ZonedDateTime zonedDataTime = ZonedDateTime.ofInstant(new Date(timesamp).toInstant(), ZoneId.systemDefault());
				builder.addOrMergeEndpoint(
					new EndpointBuilder().setWritingEndpointHandler(
						new EndpointHandlerBuilder().setHandlingTime(zonedDataTime)
					)
				);
				builder.setName(jsonConverter.getString("name", source));
				builder.setCorrelationId(jsonConverter.getString("correlation_id", source));
				builder.setPayload(jsonConverter.getString("content", source));
				String type = jsonConverter.getString("type", source);
				if ("MESSAGE_REQUEST".equals(type)) {
					builder.setMessagingEventType(MessagingEventType.REQUEST);
				} else if ("MESSAGE_RESPONSE".equals(type)) {
					builder.setMessagingEventType(MessagingEventType.RESPONSE);
				} else {
					builder.setMessagingEventType(MessagingEventType.FIRE_FORGET);
				}
				requestHandler.addBuilder(builder, zonedDataTime);
				if (requestHandler.shouldFlush()) {
					if (!requestHandler.flush()) {
						System.exit(-1);
					} else {
						bulkRequest.get();
						bulkRequest = client.prepareBulk().setRefresh(true);
					}
				}
				bulkRequest.add(new DeleteRequestBuilder(client, DeleteAction.INSTANCE)
						.setIndex(hit.getIndex())
						.setType(hit.getType())
						.setId(hit.getId())
				);
				if (count % 10000 == 0) {
					System.out.println(count + " processed.");
				}
			}
			if (!requestHandler.flush()) {
				System.exit(-1);
			}
			bulkRequest.get();
		}
		System.out.println("Done migrating " + count + " events");
	}


	private static Configuration loadConfiguration() throws FileNotFoundException, IOException {
		try (Reader reader = new FileReader(new File("migration.yml"));) {
			Yaml yaml = new Yaml();
			return yaml.loadAs(reader, Configuration.class);
		} 

	}
}
