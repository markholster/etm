package com.jecstar.etm.processor.ibmmq.handlers;


public enum NodeTypes {
	// See https://www-01.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/as36001_.htm
	ComIbmMQInputNode,
	ComIbmMQOutputNode,
	ComIbmMQGetNode,
	
	ComIbmJMSClientInputNode,
	ComIbmJMSClientOutputNode,
	ComIbmJMSClientReceive,
	ComIbmJMSClientReplyNode,
	
	ComIbmHTTPAsyncRequest,
	ComIbmHTTPAsyncResponse,
	ComIbmWSInputNode,
	ComIbmWSReplyNode,
	ComIbmWSRequestNode,
	
	ComIbmSOAPInputNode,
	ComIbmSOAPReplyNode,
	ComIbmSOAPRequestNode,
	ComIbmSOAPAsyncRequestNode,
	ComIbmSOAPAsyncResponseNode	

}
