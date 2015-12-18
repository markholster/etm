package com.jecstar.etm.processor.processor;

import java.util.List;
import java.util.UUID;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.EventCommand;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.XPathExpressionParser;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryEvent> {

	
	private final long ordinal;
	private final long numberOfConsumers;
	
	private final EtmConfiguration etmConfiguration;
	private final TelemetryEventRepository telemetryEventRepository;
	private final EndpointConfigResult endpointConfigResult;
	private final Timer timer;
	
	// ACHMEA CUSTOM
	private final XPathExpressionParser ibfExpression;
	private final XPathExpressionParser oudAchmeaExpression;
	private final XPathExpressionParser soapBodyExpression;
	
	public EnhancingEventHandler(final EtmConfiguration etmConfiguration, final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers, final Timer timer) {
		this.etmConfiguration = etmConfiguration;
		this.telemetryEventRepository = telemetryEventRepository;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.endpointConfigResult = new EndpointConfigResult();
		this.timer = timer;
		this.ibfExpression = new XPathExpressionParser("/*[local-name()='Envelope']/*[local-name()='Header']/*[local-name()='IBFheader']/*[local-name()='MessageType']");
		this.oudAchmeaExpression = new XPathExpressionParser("/*/*[local-name()='Header']/*[local-name()='Berichttype']/*[local-name()='identificatie']");		
		this.soapBodyExpression = new XPathExpressionParser("local-name(/*[local-name()='Envelope']/*[local-name()='Body']/*)");
	}

	@Override
	public void onEvent(final TelemetryEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		if (event.ignore) {
			return;
		}
		if (!EventCommand.PROCESS.equals(event.eventCommand) || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
		final Context timerContext = this.timer.time();
		if (event.id == null) {
			event.id = UUID.randomUUID().toString();
		}
		if (event.creationTime.getTime() == 0) {
			event.creationTime.setTime(System.currentTimeMillis());
		}
		customAchmeaDetermineEventType(event);
		try {
			this.endpointConfigResult.initialize();
			this.telemetryEventRepository.findEndpointConfig(event.endpoint, this.endpointConfigResult);
			// First determine the application name.
			if (event.application == null) {
				if (event.application == null) {
					event.application = parseValue(this.endpointConfigResult.applicationParsers, event.content);
				}			
			}
			if (event.name == null || event.direction == null || event.transactionName == null) {
				if (event.name == null && event.content != null) {
					event.name = parseValue(this.endpointConfigResult.eventNameParsers, event.content);
				}
				if (event.direction == null) {
					event.direction = this.endpointConfigResult.eventDirection;
				}
				if (event.transactionName == null) {
					event.transactionName = parseValue(this.endpointConfigResult.transactionNameParsers, event.content);
				}
	 		}
			if (!this.endpointConfigResult.correlationDataParsers.isEmpty()) {
				this.endpointConfigResult.correlationDataParsers.forEach((k,v) -> {
					String parsedValue = parseValue(v, event.content);
					if (parsedValue != null) {
						event.correlationData.put(k, parsedValue);
					}
				});
			}
			customAchmeaDetermineId(event);
		} finally {
			timerContext.stop();
		}
	}
	
	/**
	 * Achmea maatwerk -> Zolang er niet van WMB events gebruik gemaakt wordt.
	 * Deze methode bepaalt het message type aan de hand van de bericht inhoud.
	 */
	private void customAchmeaDetermineEventType(TelemetryEvent event) {
		String companyName = this.etmConfiguration.getLicense().getOwner();
		if (!companyName.startsWith("Achmea")) {
			return;
		}
		if (TelemetryEventType.MESSAGE_DATAGRAM.equals(event.type) && event.content != null) {
			String ibfType = this.ibfExpression.evaluate(event.content);
			if (ibfType != null && ibfType.trim().length() > 0) {
				ibfType = ibfType.trim();
				if ("request".equalsIgnoreCase(ibfType)) {
					event.type = TelemetryEventType.MESSAGE_REQUEST;
					return;
				} else if ("response".equalsIgnoreCase(ibfType)) {
					event.type = TelemetryEventType.MESSAGE_RESPONSE;
					return;
				} else if ("datagram".equalsIgnoreCase(ibfType)) {
					event.type = TelemetryEventType.MESSAGE_DATAGRAM;
					return;
				}
			}
			
			String oudAchmeaIdentificatie = this.oudAchmeaExpression.evaluate(event.content);
			if (oudAchmeaIdentificatie != null && oudAchmeaIdentificatie.trim().length() > 0) {
				oudAchmeaIdentificatie = oudAchmeaIdentificatie.trim().toLowerCase();
				if (oudAchmeaIdentificatie.endsWith("v")) {
					event.type = TelemetryEventType.MESSAGE_REQUEST;
					return;
				} else if (oudAchmeaIdentificatie.endsWith("a")) {
					event.type = TelemetryEventType.MESSAGE_RESPONSE;
					return;
				}
			}
			
			String soapBodyChild = this.soapBodyExpression.evaluate(event.content);
			if (soapBodyChild != null && soapBodyChild.trim().length() > 0) {
				soapBodyChild = soapBodyChild.trim().toLowerCase();
				if (soapBodyChild.endsWith("request")) {
					event.type = TelemetryEventType.MESSAGE_REQUEST;
					return;
				} else if (soapBodyChild.endsWith("response")) {
					event.type = TelemetryEventType.MESSAGE_RESPONSE;
					return;
				} else if (soapBodyChild.equals("fault")) {
					event.type = TelemetryEventType.MESSAGE_RESPONSE;
					return;
				}
			}
		}
    }
	
	/**
	 * Achmea Maatwerk. Een event met de naam WmbException heeft hetzelfde id
	 * als het oorspronkelijke bericht en overschrijft het oorspronkelijk
	 * bericht hierdoor. Dit maatwerk voorkomt dat.
	 */
	private void customAchmeaDetermineId(TelemetryEvent event) {
		String companyName = this.etmConfiguration.getLicense().getOwner();
		if (!companyName.startsWith("Achmea")) {
			return;
		}
		if (event.content != null && event.content.trim().startsWith("WmbException")) {
			event.id += "Exc";
		}
	}
	

	private String parseValue(List<ExpressionParser> expressionParsers, String content) {
		if (content == null || expressionParsers == null) {
			return null;
		}
		for (ExpressionParser expressionParser : expressionParsers) {
			String value = parseValue(expressionParser, content);
			if (value != null) {
				return value;
			}
		}
		return null;
    }
	
	private String parseValue(ExpressionParser expressionParser, String content) {
		if (expressionParser == null || content == null) {
			return null;
		}
		String value = expressionParser.evaluate(content);
		if (value != null && value.trim().length() > 0) {
			return value;
		}
		return null;
	}
}
