package com.jecstar.etm.processor.ibmmq;

import java.io.IOException;
import java.util.Hashtable;

import com.ibm.mq.MQDestination;
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

	private final String configurationName;
	private final StringBuilder byteArrayBuilder = new StringBuilder();
	private final AutoManagedTelemetryEventProcessor processor;
	private final TelemetryEvent telemetryEvent = new TelemetryEvent();
	private final QueueManager queueManager;
	private final Destination destination;
	
	private final int waitInterval = 5000;
	private long lastCommitTime;
	
	private boolean stop = false;
	
	private MQQueueManager mqQueueManager;
	private MQDestination mqDestination;
	
	private int counter = 0;
	
	// Handlers
	private final XmlTelemetryEventHandler etmEventHandler;
	private final IIBEventHandler iibEventHandler;
	private final ClonedMessageHandler clonedMessageHandler;

	
	public DestinationReader(String configurationName, final AutoManagedTelemetryEventProcessor processor, final QueueManager queueManager, final Destination destination) {
		this.configurationName = configurationName;
		this.processor = processor;
		this.queueManager = queueManager;
		this.destination = destination;
		this.etmEventHandler = new XmlTelemetryEventHandler();
		this.iibEventHandler = new IIBEventHandler(processor.getEtmConfiguration());
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
			MQMessage message = null;
			try {
				message = new MQMessage();
				boolean continueProcessing = true;
				try {
					this.mqDestination.get(message, getOptions, this.destination.getMaxMessageSize());
				} catch (MQException e) {
					continueProcessing = handleMQException(e);
				}
				if (!continueProcessing) {
					continue;
				}
				this.telemetryEvent.initialize();
				byte[] byteContent = new byte[message.getMessageLength()];
				message.readFully(byteContent);
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Read message with id '" + byteArrayToString(message.messageId) + "'.");
				}				
				if ("etmevent".equalsIgnoreCase(this.destination.getMessageTypes()) && this.etmEventHandler.handleMessage(this.telemetryEvent, byteContent)) {
					this.processor.processTelemetryEvent(this.telemetryEvent);
				} else if ("iibevent".equalsIgnoreCase(this.destination.getMessageTypes()) && this.iibEventHandler.handleMessage(this.telemetryEvent, byteContent)) {
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
				this.counter++;
				if (shouldCommit()) {
					commit();
				}
			} catch (Error e) {
				if (log.isFatalLevelEnabled()) {
					log.logFatalMessage("Error detected while processing messages. Stopping reader to prevent further unexpected behaviour.", e);
				}
				this.stop = true;
			} catch (Exception e) {
				if (log.isWarningLevelEnabled()) {
					log.logWarningMessage("Failed to process message. Trying to put it on the backout queue.", e);
				}
				tryBackout(message);
				this.counter++;
				if (shouldCommit()) {
					commit();
				}
			} finally {
				if (message != null) {
					try {
						message.clearMessage();
					} catch (IOException e) {
					}
				}
			}
			if (Thread.interrupted()) {
				this.stop = true;
			}
		}
		commit();
		disconnect();
	}
	
	private boolean handleMQException(MQException e) {
		if (e.completionCode == CMQC.MQCC_FAILED && e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) {
			// No message available, retry
			if (shouldCommit()) {
				commit();
			}
			return true;
		}
		switch (e.reasonCode) {
		case CMQC.MQRC_TRUNCATED_MSG_ACCEPTED:
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Accepted a truncated message.");
			}
			return true;
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
			return false;
			default:
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Detected MQ error with reason '" + e.reasonCode+ "'. Ignoring message.");
				}
				return false;
		}
	}
	private void commit() {
		if (this.mqQueueManager != null) {
			try {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Committing messages from queuemanager '" + this.mqQueueManager.getName() + "'.");
				}
				this.mqQueueManager.commit();
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Messages from queuemanager '" + this.mqQueueManager.getName() + "' committed.");
				}
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
			if ("topic".equals(this.destination.getType())) {
				this.mqDestination = this.mqQueueManager.accessTopic(this.destination.getName(), null, CMQC.MQSO_CREATE, null, "Enterprise Telemetry Monitor - " + this.configurationName);
			} else {
				this.mqDestination = this.mqQueueManager.accessQueue(this.destination.getName(), this.destination.getDestinationOpenOptions());
			}
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Connected to queuemanager '" + this.queueManager.getName() + "' and " + this.destination.getType() + " '" + this.destination.getName() + "'");
			}
		} catch (MQException e) {
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Failed to connect to queuemanager '" + this.queueManager.getName() + "' and/or " + this.destination.getType() + " '" + this.destination.getName() + "'" , e);
			}
		}
	}
	
	private void disconnect() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Disconnecting from queuemanager");
		}
		if (this.mqDestination != null) {
			try {
				this.mqDestination.close();
			} catch (MQException e) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Unable to close " + this.destination.getType() , e);
				}
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
	
	private boolean shouldCommit() {
		return (this.counter >= this.destination.getCommitSize()) || (System.currentTimeMillis() - this.lastCommitTime > this.destination.getCommitInterval());
	}
	
	private boolean tryBackout(MQMessage message) {
		MQQueue backoutQueue = null;
		try {
			String backoutQueueName = this.mqDestination.getAttributeString(CMQC.MQCA_BACKOUT_REQ_Q_NAME, 48);
			if (backoutQueueName != null && backoutQueueName.trim().length() > 0) {
				backoutQueue = this.mqQueueManager.accessQueue(backoutQueueName.trim(), CMQC.MQOO_OUTPUT + CMQC.MQOO_FAIL_IF_QUIESCING);
				backoutQueue.put(message);
				backoutQueue.close();
				return true;
			} else {
				if (log.isWarningLevelEnabled()) {
					log.logWarningMessage("No backout queue defined on destination '" + this.mqDestination.getName() + "'. Unable to backout messages.");
				}				
			}
		} catch (MQException e) {
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Failed to put message with id '" + byteArrayToString(message.messageId) + "' to the configured backout queue", e);
			}
		} finally {
			if (backoutQueue != null) {
				try {
					backoutQueue.close();
				} catch (MQException e1) {
				}
			}
		}
		return false;
	}
	
	public void stop() {
		this.stop = true;
	}
	
	private String byteArrayToString(byte[] bytes) {
		if (bytes == null) {
			return null;
		}		
		this.byteArrayBuilder.setLength(0);
		boolean allZero = true;
		for (int i = 0; i < bytes.length; i++) {
			this.byteArrayBuilder.append(String.format("%02x", bytes[i]));
			if (bytes[i] != 0) {
				allZero = false;
			}
		}
		return allZero ? null : this.byteArrayBuilder.toString();
	}

}
