package com.jecstar.etm.processor.ibmmq.handler;

import java.io.IOException;

import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.Charsets;

abstract class AbstractEventHandler {
	
	String getContent(MQMessage message) throws IOException {
		byte[] byteContent = new byte[message.getMessageLength()];
		message.setDataOffset(0);
		message.readFully(byteContent);
		return Charsets.convert(byteContent, message.characterSet);
	}
}
