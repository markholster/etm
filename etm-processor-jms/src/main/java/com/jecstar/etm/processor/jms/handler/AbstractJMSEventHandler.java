package com.jecstar.etm.processor.jms.handler;

import com.jecstar.etm.processor.handler.AbstractJsonHandler;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

abstract class AbstractJMSEventHandler extends AbstractJsonHandler {

    String getContent(Message message) throws JMSException {
        if (message instanceof TextMessage) {
            return ((TextMessage) message).getText();
        } else if (message instanceof BytesMessage) {
            return ((BytesMessage) message).readUTF();
        }
        return null;
    }
}
