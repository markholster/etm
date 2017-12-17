package com.jecstar.etm.processor.jms.handler;

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.HandlerResults;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import javax.jms.JMSException;
import javax.jms.Message;

public class EtmEventHandler extends AbstractJMSEventHandler {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(EtmEventHandler.class);

	private final TelemetryCommandProcessor telemetryCommandProcessor; 	

	public EtmEventHandler(TelemetryCommandProcessor telemetryCommandProcessor) {
		this.telemetryCommandProcessor = telemetryCommandProcessor;
	}

    @Override
    protected TelemetryCommandProcessor getProcessor() {
        return this.telemetryCommandProcessor;
    }

    @SuppressWarnings("unchecked")
	public HandlerResults handleMessage(Message message) {
		HandlerResults results = new HandlerResults();
		try {
			results = handleData(getContent(message));
		} catch (JMSException e) {
			String messageId = "unknown";
			try {
				messageId = message.getJMSMessageID();
			} catch (JMSException e1) {}
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Message with id '" + messageId + "' seems to have an invalid format", e);
			}
			results.addHandlerResult(com.jecstar.etm.processor.handler.HandlerResult.parserFailure(e));
		}
		return results;
	}

}
