package com.jecstar.etm.processor.ibmmq;

import java.io.IOException;
import java.util.Hashtable;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.ibmmq.configuration.Destination;
import com.jecstar.etm.processor.ibmmq.configuration.QueueManager;
import com.jecstar.etm.processor.ibmmq.handlers.ClonedMessageHandler;
import com.jecstar.etm.processor.ibmmq.handlers.IIBEventHandler;
import com.jecstar.etm.processor.ibmmq.handlers.XmlTelemetryEventHandler;
import com.jecstar.etm.processor.ibmmq.startup.AutoManagedTelemetryEventProcessor;

public class DestinationReader implements Runnable {
	
	private static final LogWrapper log = LogFactory.getLogger(DestinationReader.class);

	private final AutoManagedTelemetryEventProcessor processor;
	private final TelemetryEvent telemetryEvent = new TelemetryEvent();
	private final QueueManager queueManager;
	private final Destination destination;
	
	private final int waitInterval = 5000;
	private long lastCommitTime;
	
	private boolean stop = false;
	
	private MQQueueManager mqQueueManager;
	private MQQueue mqQueue;
	
	private int counter = 0;
	
	// Handlers
	private final XmlTelemetryEventHandler etmEventHandler;
	private final IIBEventHandler iibEventHandler;
	private final ClonedMessageHandler clonedMessageHandler;
	
	public DestinationReader(final AutoManagedTelemetryEventProcessor processor, final QueueManager queueManager, final Destination destination) {
		this.processor = processor;
		this.queueManager = queueManager;
		this.destination = destination;
		this.etmEventHandler = new XmlTelemetryEventHandler();
		this.iibEventHandler = new IIBEventHandler();
		this.clonedMessageHandler = new ClonedMessageHandler();
	}
	@Override
	public void run() {
		connect();
		MQGetMessageOptions getOptions = new MQGetMessageOptions();
		getOptions.waitInterval = this.waitInterval; // Wait interval in milliseconds.
		getOptions.options = this.destination.getDestinationGetOptions();
		this.lastCommitTime = System.currentTimeMillis();
		while (!this.stop) {
			try {
				MQMessage message = new MQMessage();
				this.mqQueue.get(message, getOptions);
				this.telemetryEvent.initialize();
				byte[] byteContent = new byte[message.getMessageLength()];
				message.readFully(byteContent);
				this.telemetryEvent.initialize();
				if ("etmevent".equalsIgnoreCase(this.destination.getMessageTypes()) && this.etmEventHandler.handleMessage(this.telemetryEvent, byteContent)) {
					this.processor.processTelemetryEvent(this.telemetryEvent);
				} else if ("ïibevent".equalsIgnoreCase(this.destination.getMessageTypes()) && this.iibEventHandler.handleMessage(this.telemetryEvent, byteContent)) {
					this.processor.processTelemetryEvent(this.telemetryEvent);
				} else if ("clone".equalsIgnoreCase(this.destination.getMessageTypes()) && this.clonedMessageHandler.handleMessage(this.telemetryEvent, byteContent)) {
					this.clonedMessageHandler.handleHeader(this.telemetryEvent, message);
					this.processor.processTelemetryEvent(this.telemetryEvent);
				} else {
					if(this.etmEventHandler.handleMessage(this.telemetryEvent, byteContent)) {
						this.processor.processTelemetryEvent(this.telemetryEvent);
					} else if (this.iibEventHandler.handleMessage(this.telemetryEvent, byteContent)) {
						this.processor.processTelemetryEvent(this.telemetryEvent);
					} else {
						if (this.clonedMessageHandler.handleMessage(this.telemetryEvent, byteContent)) {
							this.clonedMessageHandler.handleHeader(this.telemetryEvent, message);
							this.processor.processTelemetryEvent(this.telemetryEvent);
						}						
					}
				}
				message.clearMessage();
				if (++this.counter >= this.destination.getCommitSize()) {
					commit();
				}
			} catch (MQException e) {
				if (e.completionCode == 2 && e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) {
					// No message available, retry
					if (System.currentTimeMillis() - this.lastCommitTime > this.destination.getCommitInterval()) {
						commit();
					}
					continue;
				}
				switch (e.reasonCode) {
				case CMQC.MQRC_CONNECTION_BROKEN:
				case CMQC.MQRC_CONNECTION_QUIESCING:
				case CMQC.MQRC_CONNECTION_STOPPING:
				case CMQC.MQRC_Q_MGR_QUIESCING:
				case CMQC.MQRC_Q_MGR_STOPPING:
				case CMQC.MQRC_Q_MGR_NOT_AVAILABLE:
				case CMQC.MQRC_Q_MGR_NOT_ACTIVE:
				case CMQC.MQRC_CLIENT_CONN_ERROR:
				case CMQC.MQRC_CHANNEL_STOPPED_BY_USER:
				case CMQC.MQRC_HCONN_ERROR:
				case CMQC.MQRC_HOBJ_ERROR:
				case CMQC.MQRC_UNEXPECTED_ERROR:
					if (log.isInfoLevelEnabled()) {
						log.logInfoMessage("Detected MQ error with reason '" + e.reasonCode+ "'. Trying to reconnect.");
					}
					disconnect();
					try {
						Thread.sleep(this.waitInterval);
					} catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
					}
					connect();
					break;
					default:
						if (log.isInfoLevelEnabled()) {
							log.logInfoMessage("Detected MQ error with reason '" + e.reasonCode+ "'. Trying to reconnect.");
						}
				}
			} catch (IOException e) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Failed to read message content", e);
				}
			}
			if (Thread.interrupted()) {
				this.stop = true;
			}
		}
		disconnect();
	}
	
	private void commit() {
		if (this.mqQueueManager != null) {
			try {
				this.mqQueueManager.commit();
			} catch (MQException e) {
				if (log.isErrorLevelEnabled()) {
					log.logErrorMessage("Unable to execute commit on queuemanager.", e);
				}
			}
		}
		this.counter = 0;
		this.lastCommitTime = System.currentTimeMillis();		
	}
	
	private void connect() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Connecting to queuemanager");
		}
		try {
			Hashtable<String, Object> connectionProperties = new Hashtable<String, Object>();
			connectionProperties.put(CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES_CLIENT);
			connectionProperties.put(CMQC.HOST_NAME_PROPERTY, this.queueManager.getHost());
			if (this.queueManager.getChannel() != null) {
				connectionProperties.put(CMQC.CHANNEL_PROPERTY, this.queueManager.getChannel());
			}
			connectionProperties.put(CMQC.PORT_PROPERTY, this.queueManager.getPort());
			this.mqQueueManager = new MQQueueManager(this.queueManager.getName(), connectionProperties);
			this.mqQueue = this.mqQueueManager.accessQueue(this.destination.getName(), this.destination.getDestinationOpenOptions());
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Connected to queuemanager '" + this.queueManager.getName() + "'");
			}
		} catch (MQException e) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Failed to connect to queuemanager", e);
			}
		}
	}
	
	private void disconnect() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Disconnecting from queuemanager");
		}
		if (this.mqQueue != null) {
			try {
				this.mqQueue.close();
			} catch (MQException e) {
				e.printStackTrace();
			}
		}
		if (this.mqQueueManager != null) {
			try {
				counter = 0;
				this.lastCommitTime = System.currentTimeMillis();
				this.mqQueueManager.commit();
			} catch (MQException e1) {
				if (log.isErrorLevelEnabled()) {
					log.logErrorMessage("Unable to execute commit on queuemanager", e1);
				}
			}
			try {
				this.mqQueueManager.close();
			} catch (MQException e) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Unable to close queuemanager", e);
				}
			}
		}
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Disconnected from queuemanager");
		}
	}
	
	public void stop() {
		this.stop = true;
	}

}
