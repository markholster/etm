package com.jecstar.etm.domain.writer.json;

import java.util.stream.Collectors;

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.v1migrator.ConversionMessagingTelemetryEventBuilder;

public class ConversionMessagingTelemetryEventWriterJsonImpl extends MessagingTelemetryEventWriterJsonImpl {

	private ConversionMessagingTelemetryEventBuilder currentBuilder;
	
	public String write(ConversionMessagingTelemetryEventBuilder builder) {
		this.currentBuilder = builder;
		return super.write(builder.build());
	}
	
	@Override
	boolean doWrite(MessagingTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = super.doWrite(event, buffer, firstElement);
		if (this.currentBuilder != null && this.currentBuilder.getCorrelations() != null && this.currentBuilder.getCorrelations().size() > 0) {
			if (added) {
				buffer.append(", ");
			}
			buffer.append("\"").append(getTags().getCorrelationsTag()).append("\": [");
			buffer.append(this.currentBuilder.getCorrelations().stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(",")));
			buffer.append("]");
			added = true;

		}
		return added;
	}
}
