package com.holster.etm.processor.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

@WebService(name="ProcessorService")
public interface ProcessorService {

	@WebMethod(operationName="addTelemetryEvent", action="http://ws.etm.holster.com/processorservice/addTelemetryEvent")
    @XmlElement(required=true)
    @WebResult(name="success")
	public boolean addTelemetryEvent(@WebParam(name="TelemetryEvent") @XmlElement(required=true)XmlTelemetryEvent xmlTelemetryEvent);
}
