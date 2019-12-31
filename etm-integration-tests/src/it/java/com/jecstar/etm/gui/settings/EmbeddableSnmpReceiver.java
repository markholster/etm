package com.jecstar.etm.gui.settings;

import com.consol.citrus.exceptions.CitrusRuntimeException;
import org.snmp4j.*;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import java.io.IOException;

/**
 * A super simple Snmp PDU receiver.
 */
public class EmbeddableSnmpReceiver implements CommandResponder {

    public static final String HOST = "127.0.0.1";
    public static final int PORT = 10162;
    public static final String COMMUNITY = "public";
    private Snmp snmp;
    private PDU pdu;


    public void startServer() {
        try {
            AbstractTransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping(new UdpAddress(HOST + "/" + PORT));

            ThreadPool threadPool = ThreadPool.create("DispatcherPool", 3);
            MessageDispatcher dispatcher = new MultiThreadedMessageDispatcher(
                    threadPool, new MessageDispatcherImpl());

            // add message processing models
            dispatcher.addMessageProcessingModel(new MPv1());
            dispatcher.addMessageProcessingModel(new MPv2c());

            // add all security protocols
            SecurityProtocols.getInstance().addDefaultProtocols();

            // Create Target
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(COMMUNITY));

            this.snmp = new Snmp(dispatcher, transport);
            this.snmp.addCommandResponder(this);

            transport.listen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopServer() {
        try {
            this.snmp.close();
        } catch (IOException e) {
        }
    }

    /**
     * This method will be called whenever a pdu is received on the given port
     * specified in the listen() method
     */
    public void processPdu(CommandResponderEvent cmdRespEvent) {
        this.pdu = cmdRespEvent.getPDU();
    }

    public PDU retrievePDU(int timeout) {
        long starTime = System.currentTimeMillis();
        while (this.pdu == null) {
            if (System.currentTimeMillis() - starTime > timeout) {
                throw new CitrusRuntimeException("Timeout waiting for PDU");
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return this.pdu;
    }
}