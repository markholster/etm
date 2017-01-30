package com.jecstar.etm.v1migrator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
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

import com.jecstar.etm.domain.builders.MessagingTelemetryEventBuilder;


public class Startup {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Configuration config = loadConfiguration();
		Settings settings = Settings.settingsBuilder()
		        .put("cluster.name", config.inputClusterName).build();
		Client client = TransportClient.builder().settings(settings).build()
		        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(config.inputHostname), config.inputPort));
		InsertRequestHandler requestHandler = new InsertRequestHandler(config.etm20BulkApiLocation);
		while (true) {
			SearchResponse searchResponse = client.prepareSearch("etm_event_all")
				.addSort(SortBuilders.fieldSort("timestamp").order(SortOrder.ASC))
				.setSize(config.bulkSize)
				.get();
			BulkRequestBuilder bulkRequest = client.prepareBulk().setRefresh(true);
			SearchHits hits = searchResponse.getHits();
			if (hits.getHits() == null || hits.getHits().length == 0) {
				break;
			}
			for (SearchHit hit : hits.getHits()) {
				Map<String, Object> source = hit.getSource();
				// TODO mapping!
				MessagingTelemetryEventBuilder builder = new MessagingTelemetryEventBuilder();
				builder.setId(hit.getId());
				if (!requestHandler.addBuilder(builder)) {
					System.exit(-1);
				}
				bulkRequest.add(new DeleteRequestBuilder(client, DeleteAction.INSTANCE)
						.setIndex(hit.getIndex())
						.setType(hit.getType())
						.setId(hit.getId())
				);
			}
			if (!requestHandler.flush()) {
				System.exit(-1);
			}
//			bulkRequest.get();
		}
	}

	private static Configuration loadConfiguration() throws FileNotFoundException, IOException {
		try (Reader reader = new FileReader(new File("migration.yml"));) {
			Yaml yaml = new Yaml();
			return yaml.loadAs(reader, Configuration.class);
		} 

	}
}
