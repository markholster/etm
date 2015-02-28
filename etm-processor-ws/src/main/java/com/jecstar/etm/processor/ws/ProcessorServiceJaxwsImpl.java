package com.jecstar.etm.processor.ws;

import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.processor.TelemetryEventProcessor;

@Stateless(name="ProcessorService")
@WebService(serviceName="ProcessorService", targetNamespace="http://ws.etm.holster.com/processorservice", portName="ProcessorServicePort", endpointInterface="com.jecstar.etm.processor.ws.ProcessorService")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
@PermitAll
public class ProcessorServiceJaxwsImpl implements ProcessorService {

	
	@Inject
	@ProcessorConfiguration
	private TelemetryEventProcessor telemetryEventProcessor;

	private final TelemetryEvent telemetryEvent = new TelemetryEvent();

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean addTelemetryEvent(XmlTelemetryEvent xmlTelemetryEvent) {
		this.telemetryEvent.initialize();
		xmlTelemetryEvent.copyToTelemetryEvent(this.telemetryEvent);
		this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
	    return true;
    }

}
