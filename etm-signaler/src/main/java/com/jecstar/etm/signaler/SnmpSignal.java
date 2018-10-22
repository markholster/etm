package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.configuration.EtmSnmpConstants;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.signaler.domain.Signal;
import org.joda.time.DateTime;
import org.snmp4j.*;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.security.nonstandard.PrivAES192With3DESKeyExtension;
import org.snmp4j.security.nonstandard.PrivAES256With3DESKeyExtension;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;


/**
 * Class that sends SNMP traps/notifications.
 */
class SnmpSignal {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(SnmpSignal.class);

    private static final int RETRIES = 2;
    private static final int TIMEOUT = 5000;

    private final byte[] engineId;

    static {
        SNMP4JSettings.setEnterpriseID(EtmSnmpConstants.JECSTAR_PEN);
    }

    SnmpSignal(byte[] engineId) {
        this.engineId = engineId;
    }

    void sendExceedanceNotification(String clusterName, Signal signal, Notifier notifier, Map<DateTime, Double> thresholdExceedances, long systemStartTime) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unable to send SNMP signal.", e);
            }
            return;
        }
        if (Notifier.SnmpVersion.V1.equals(notifier.getSnmpVersion())) {
            sendSnmpV1Trap(clusterName, signal, notifier, thresholdExceedances, inetAddress, systemStartTime);
        } else if (Notifier.SnmpVersion.V2C.equals(notifier.getSnmpVersion())) {
            sendSnmpV2Notification(clusterName, signal, notifier, thresholdExceedances, inetAddress, systemStartTime);
        } else if (Notifier.SnmpVersion.V3.equals(notifier.getSnmpVersion())) {
            sendSnmpV3Notification(clusterName, signal, notifier, thresholdExceedances, inetAddress, systemStartTime);
        } else {
            if (log.isWarningLevelEnabled()) {
                log.logWarningMessage("Unsupported SNMP version '" + notifier.getSnmpVersion().name() + "'. Signal not sent.");
            }
        }
    }

    private void sendSnmpV1Trap(String clusterName, Signal signal, Notifier notifier, Map<DateTime, Double> thresholdExceedances, InetAddress inetAddress, long systemStartTime) {
        Snmp snmp = null;
        try {
            // Create Transport Mapping
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            transport.listen();

            // Create Target
            CommunityTarget comtarget = new CommunityTarget();
            comtarget.setCommunity(new OctetString(notifier.getSnmpCommunity()));
            comtarget.setVersion(SnmpConstants.version1);
            comtarget.setAddress(new UdpAddress(notifier.getHost() + "/" + notifier.getPort()));
            comtarget.setRetries(RETRIES);
            comtarget.setTimeout(TIMEOUT);

            // Create PDU for V1
            int ix = EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_OID.lastIndexOf(".");
            PDUv1 pdu = new PDUv1();
            pdu.setType(PDU.V1TRAP);
            pdu.setEnterprise(new OID(EtmSnmpConstants.ETM_NOTIFOCATION_OID));
            pdu.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
            pdu.setSpecificTrap(Integer.valueOf(EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_OID.substring(ix + 1)));
            pdu.setAgentAddress(new IpAddress(inetAddress));
            long sysUpTime = (System.currentTimeMillis() - systemStartTime) / 10;
            pdu.setTimestamp(sysUpTime);

            addSignalVariables(pdu, clusterName, signal, thresholdExceedances);

            // Send the PDU
            snmp = new Snmp(transport);
            snmp.send(pdu, comtarget);
            snmp.close();
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unable to send SNMP v1 trap.", e);
            }
        } finally {
            closeSnmp(snmp);
        }
    }

    private void sendSnmpV2Notification(String clusterName, Signal signal, Notifier notifier, Map<DateTime, Double> thresholdExceedances, InetAddress inetAddress, long systemStartTime) {
        Snmp snmp = null;
        try {
            // Create Transport Mapping
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            transport.listen();

            // Create Target
            CommunityTarget comtarget = new CommunityTarget();
            comtarget.setCommunity(new OctetString(notifier.getSnmpCommunity()));
            comtarget.setVersion(SnmpConstants.version2c);
            comtarget.setAddress(new UdpAddress(notifier.getHost() + "/" + notifier.getPort()));
            comtarget.setRetries(RETRIES);
            comtarget.setTimeout(TIMEOUT);

            // Create PDU for V2
            PDU pdu = new PDU();

            // need to specify the system up time
            long sysUpTime = (System.currentTimeMillis() - systemStartTime) / 10;
            pdu.setType(PDU.NOTIFICATION);
            pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(sysUpTime)));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_OID)));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(inetAddress)));

            addSignalVariables(pdu, clusterName, signal, thresholdExceedances);

            // Send the PDU
            snmp = new Snmp(transport);
            snmp.send(pdu, comtarget);
            snmp.close();
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unable to send SNMP v2 notification.", e);
            }
        } finally {
            closeSnmp(snmp);
        }
    }

    private void sendSnmpV3Notification(String clusterName, Signal signal, Notifier notifier, Map<DateTime, Double> thresholdExceedances, InetAddress inetAddress, long systemStartTime) {
        OctetString engineId = new OctetString(this.engineId);

        PrivacyProtocol privacyProtocol = null;
        AuthenticationProtocol authenticationProtocol = null;
        int securityLevel = SecurityLevel.NOAUTH_NOPRIV;
        Snmp snmp = null;
        try {
            Address targetAddress = GenericAddress.parse("udp:" + notifier.getHost()
                    + "/" + notifier.getPort());
            TransportMapping<?> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp();
            snmp.addTransportMapping(transport);
            USM usm = new USM(SecurityProtocols.getInstance().addDefaultProtocols(), engineId, 0);
            snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3(usm));
            if (notifier.getSnmpPrivacyProtocol() != null) {
                privacyProtocol = toPrivacyProtocol(notifier.getSnmpPrivacyProtocol());
                SecurityProtocols.getInstance().addPrivacyProtocol(privacyProtocol);
            }
            if (notifier.getSnmpAuthenticationProtocol() != null) {
                authenticationProtocol = toAuthenticationProtocol(notifier.getSnmpAuthenticationProtocol());
            }
            if (authenticationProtocol != null && privacyProtocol == null) {
                securityLevel = SecurityLevel.AUTH_NOPRIV;
            } else if (authenticationProtocol != null) {
                securityLevel = SecurityLevel.AUTH_PRIV;
            }
            SecurityModels.getInstance().addSecurityModel(usm);
            transport.listen();

            if (notifier.getUsername() != null) {
                snmp.getUSM().addUser(
                        new OctetString(notifier.getUsername()),
                        new UsmUser(new OctetString(notifier.getUsername()),
                                authenticationProtocol == null ? null : authenticationProtocol.getID(),
                                authenticationProtocol == null ? null : new OctetString(notifier.getPassword()),
                                privacyProtocol == null ? null : privacyProtocol.getID(),
                                privacyProtocol == null ? null : new OctetString(notifier.getSnmpPrivacyPassphrase())
                        )
                );
            }

            // Create Target
            UserTarget target = new UserTarget();
            target.setAddress(targetAddress);
            target.setRetries(RETRIES);
            target.setTimeout(TIMEOUT);
            target.setVersion(SnmpConstants.version3);
            target.setSecurityLevel(securityLevel);
            if (notifier.getUsername() != null) {
                target.setSecurityName(new OctetString(notifier.getUsername()));
            }

            // Create PDU for V3
            ScopedPDU pdu = new ScopedPDU();
            long sysUpTime = (System.currentTimeMillis() - systemStartTime) / 10;
            pdu.setType(ScopedPDU.NOTIFICATION);
            pdu.setContextName(new OctetString("EtmSignal"));
            pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(sysUpTime)));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_OID)));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(inetAddress)));
            addSignalVariables(pdu, clusterName, signal, thresholdExceedances);

            // Send the PDU
            snmp.send(pdu, target);
            snmp.close();
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unable to send SNMP v3 notification.", e);
            }
        } finally {
            closeSnmp(snmp);
        }
    }


    private PrivacyProtocol toPrivacyProtocol(Notifier.SnmpPrivacyProtocol snmpPrivacyProtocol) {
        switch (snmpPrivacyProtocol) {
            case DES:
                return new PrivDES();
            case TDES:
                return new Priv3DES();
            case AES128:
                return new PrivAES128();
            case AES192:
                return new PrivAES192();
            case AES256:
                return new PrivAES256();
            case AES192WITH3DES:
                return new PrivAES192With3DESKeyExtension();
            case AES256WITH3DES:
                return new PrivAES256With3DESKeyExtension();
            default:
                throw new IllegalArgumentException(snmpPrivacyProtocol.name());
        }
    }

    private AuthenticationProtocol toAuthenticationProtocol(Notifier.SnmpAuthenticationProtocol snmpAuthenticationProtocol) {
        switch (snmpAuthenticationProtocol) {
            case MD5:
                return new AuthMD5();
            case SHA:
                return new AuthSHA();
            case HMAC128SHA224:
                return new AuthHMAC128SHA224();
            case HMAC192SHA256:
                return new AuthHMAC192SHA256();
            case HMAC256SHA384:
                return new AuthHMAC256SHA384();
            case HMAC384SHA512:
                return new AuthHMAC384SHA512();
            default:
                throw new IllegalArgumentException(snmpAuthenticationProtocol.name());
        }
    }

    private String getVariableOid(String variableSuffix) {
        return EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_OID + variableSuffix;
    }

    private void addSignalVariables(PDU pdu, String clusterName, Signal signal, Map<DateTime, Double> thresholdExceedances) {
        pdu.add(new VariableBinding(new OID(getVariableOid(EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_CLUSTER_NAME_SUFFIX)), new OctetString(clusterName)));
        pdu.add(new VariableBinding(new OID(getVariableOid(EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_NAME_SUFFIX)), new OctetString(signal.getName())));
        pdu.add(new VariableBinding(new OID(getVariableOid(EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_THRESHOLD_SUFFIX)), new Integer32(signal.getThreshold())));
        pdu.add(new VariableBinding(new OID(getVariableOid(EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_LIMIT_SUFFIX)), new Integer32(signal.getLimit())));
        pdu.add(new VariableBinding(new OID(getVariableOid(EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_COUNT_SUFFIX)), new Integer32(thresholdExceedances.size())));
    }

    private void closeSnmp(Snmp snmp) {
        if (snmp != null) {
            try {
                snmp.close();
            } catch (IOException e) {
                if (log.isErrorLevelEnabled()) {
                    log.logErrorMessage("Unable to close SNMP transport.", e);
                }
            }
        }
    }

}
