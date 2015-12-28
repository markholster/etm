package com.jecstar.etm.processor.ibmmq.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.ibmmq.XmlTelemetryEvent;

public class XmlTelemetryEventHandler implements MessageHandler<byte[]> {

	private static final LogWrapper log = LogFactory.getLogger(XmlTelemetryEventHandler.class);
	
	private final Unmarshaller unmarshaller;

	public XmlTelemetryEventHandler() {
		try {
	        JAXBContext jaxbContext = JAXBContext.newInstance(XmlTelemetryEvent.class);
	        this.unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
        	throw new EtmException(EtmException.UNMARSHALLER_CREATE_EXCEPTION, e);
        }
	}

	@Override
	public boolean handleMessage(TelemetryEvent telemetryEvent, byte[] message) {
		try (Reader reader = new InputStreamReader(new ByteArrayInputStream(message));) {
            XmlTelemetryEvent xmlTelemetryEvent = (XmlTelemetryEvent) this.unmarshaller.unmarshal(reader);
            xmlTelemetryEvent.copyToTelemetryEvent(telemetryEvent);
            return true;
        } catch (JAXBException e) {
        	if (log.isDebugLevelEnabled()) {
        		log.logDebugMessage("Unable to unmarshall event.", e);
        	}
        } catch (IOException e) {
        	if (log.isDebugLevelEnabled()) {
        		log.logDebugMessage("Failed to autoclose reader.", e);
        	}
		}
		return false;
	}

}
