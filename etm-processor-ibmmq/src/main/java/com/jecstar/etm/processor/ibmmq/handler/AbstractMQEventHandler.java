package com.jecstar.etm.processor.ibmmq.handler;

import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.Charsets;
import com.jecstar.etm.processor.handler.AbstractJsonHandler;

import java.io.IOException;

abstract class AbstractMQEventHandler extends AbstractJsonHandler {

    AbstractMQEventHandler(String defaultImportProfile) {
        super(defaultImportProfile);
    }

    String getContent(MQMessage message) throws IOException {
        byte[] byteContent = new byte[message.getMessageLength()];
        message.setDataOffset(0);
        message.readFully(byteContent);
        return Charsets.convert(byteContent, message.characterSet);
    }
}
