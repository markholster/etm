package com.jecstar.etm.processor.internal.persisting;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.builders.BusinessTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.EndpointBuilder;
import com.jecstar.etm.domain.writers.json.JsonWriter;

public class BusinessEventLogger {

	private static final String BUSINESS_EVENT_ETM_STARTED = "{\"component\": \"etm\", \"action\": \"started\"}";
	private static final String BUSINESS_EVENT_ETM_STOPPED = "{\"component\": \"etm\", \"action\": \"stopped\"}";
	private static final String BUSINESS_EVENT_IBM_MQ_PROCESSOR_EMERGENCY_SHUTDOWN = "{\"component\": \"ibm mq processor\", \"action\": \"emergency shutdown\", \"reason\": \"{0}\"}";
	private static final JsonWriter jsonWriter = new JsonWriter();
	
	private static InternalBulkProcessorWrapper internalBulkProcessorWrapper;
	private static EndpointBuilder etmEndpoint;
	
	public static void initialize(InternalBulkProcessorWrapper bulkProcessorWrapper, EndpointBuilder etmEndpoint) {
		BusinessEventLogger.internalBulkProcessorWrapper = bulkProcessorWrapper;
		BusinessEventLogger.etmEndpoint = etmEndpoint;
	}

	public static void logEtmStartup() {
		BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder().setPayload(BUSINESS_EVENT_ETM_STARTED)
				.setPayloadFormat(PayloadFormat.JSON)
				.setName("Enterprise Telemetry Monitor started")
				.addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
				.build();
		BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
	}
	
	public static void logEtmShutdown() {
		BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder().setPayload(BUSINESS_EVENT_ETM_STOPPED)
				.setPayloadFormat(PayloadFormat.JSON)
				.setName("Enterprise Telemetry Monitor stopped")
				.addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
				.build();
		BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
	}

	public static void logMqProcessorEmergencyShutdown(Error e) {
		BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder().setPayload(BUSINESS_EVENT_IBM_MQ_PROCESSOR_EMERGENCY_SHUTDOWN.replace("{0}", jsonWriter.escapeToJson(e.getMessage(), false)))
				.setPayloadFormat(PayloadFormat.JSON)
				.setName("IBM MQ processor emergency shutdown.")
				.addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
				.build();
		BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
	}
}
