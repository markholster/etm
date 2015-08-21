package com.jecstar.etm.processor.repository.elastic;

import java.io.IOException;

import org.elasticsearch.client.support.AbstractClient;

import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryMessageEvent;
import com.jecstar.etm.processor.repository.AbstractTelemetryEventRepository;
import com.jecstar.etm.processor.repository.EndpointConfigResult;

public class ElasticTelemetryEventRepository extends AbstractTelemetryEventRepository {

	private AbstractClient elasticClient;

	public ElasticTelemetryEventRepository(AbstractClient elasticClient) {
	    this.elasticClient = elasticClient;
    }
	
	@Override
    public void findEndpointConfig(String endpoint, EndpointConfigResult result, long cacheExpiryTime) {
	    // TODO Auto-generated method stub
    }

	@Override
    public void close() throws IOException {
	    // TODO Auto-generated method stub
    }

	@Override
    protected void startPersist(TelemetryEvent event) {
	    // TODO Auto-generated method stub
    }

	@Override
    protected void endPersist() {
	    // TODO Auto-generated method stub
    }

	@Override
    protected void addTelemetryMessageEvent(TelemetryMessageEvent event) {
	    // TODO Auto-generated method stub
    }

}
