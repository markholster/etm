package com.jecstar.etm.processor.ws;

import javax.annotation.security.PermitAll;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.processor.TelemetryEventProcessor;

@Stateless(name="ProcessorService")
@WebService(serviceName="ProcessorService", targetNamespace="http://ws.etm.jecstar.com/processorservice", portName="ProcessorServicePort", endpointInterface="com.jecstar.etm.processor.ws.ProcessorService")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
@PermitAll
public class ProcessorServiceJaxwsImpl implements ProcessorService {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ProcessorServiceJaxwsImpl.class);
	
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
    
	@Schedule(minute="*", hour="*", second="0,30", persistent=false)
	public void flushDocuments() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Requesting flush of documents");
		}
		if (this.telemetryEventProcessor != null) {
			this.telemetryEventProcessor.requestDocumentsFlush();
		}
	}


}
