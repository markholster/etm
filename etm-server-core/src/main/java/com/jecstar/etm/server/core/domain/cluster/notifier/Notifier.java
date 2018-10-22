package com.jecstar.etm.server.core.domain.cluster.notifier;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.JsonNamespace;
import com.jecstar.etm.server.core.converter.custom.Base64Converter;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

/**
 * A notifier that is able to send notifications to the end user or another system.
 */
@JsonNamespace(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER)
public class Notifier {

    public enum NotifierType {
        ETM_BUSINESS_EVENT, EMAIL, SNMP;

        public static NotifierType safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return NotifierType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum SmtpConnectionSecurity {

        SSL_TLS, STARTTLS;

        public static SmtpConnectionSecurity safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return SmtpConnectionSecurity.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum SnmpVersion {
        V1, V2C, V3;

        public static SnmpVersion safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return SnmpVersion.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum SnmpAuthenticationProtocol {
        HMAC128SHA224, HMAC192SHA256, HMAC256SHA384, HMAC384SHA512, MD5, SHA;

        public static SnmpAuthenticationProtocol safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return SnmpAuthenticationProtocol.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum SnmpPrivacyProtocol {
        TDES, AES128, AES192, AES192WITH3DES, AES256, AES256WITH3DES, DES;

        public static SnmpPrivacyProtocol safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return SnmpPrivacyProtocol.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static final String NAME = "name";
    public static final String NOTIFIER_TYPE = "type";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    // SMTP specific fields
    public static final String SMTP_CONNECTION_SECURITY = "smtp_connection_security";
    public static final String SMTP_FROM_ADDRESS = "smtp_from_address";
    public static final String SMTP_FROM_NAME = "smtp_from_name";
    // SNMP specific fields
    public static final String SNMP_VERSION = "snmp_version";
    public static final String SNMP_COMMUNITY = "snmp_community";
    public static final String SNMP_AUTHENTICATION_PROTOCOL = "snmp_authentication_protocol";
    public static final String SNMP_PRIVACY_PROTOCOL = "snmp_privacy_protocol";
    public static final String SNMP_PRIVACY_PASSPHRASE = "snmp_privacy_passphrase";

    @JsonField(NAME)
    private String name;
    @JsonField(value = NOTIFIER_TYPE, converterClass = EnumConverter.class)
    private NotifierType notifierType;

    // Email fields
    @JsonField(HOST)
    private String host;
    @JsonField(PORT)
    private Integer port;
    @JsonField(USERNAME)
    private String username;
    @JsonField(value = PASSWORD, converterClass = Base64Converter.class)
    private String password;

    // SMTP specific fields
    @JsonField(value = SMTP_CONNECTION_SECURITY, converterClass = EnumConverter.class)
    private SmtpConnectionSecurity smtpConnectionSecurity;
    @JsonField(SMTP_FROM_ADDRESS)
    private String smtpFromAddress;
    @JsonField(SMTP_FROM_NAME)
    private String smtpFromName;

    // SNMP specific fields
    @JsonField(value = SNMP_VERSION, converterClass = EnumConverter.class)
    private SnmpVersion snmpVersion;
    @JsonField(value = SNMP_COMMUNITY, converterClass = Base64Converter.class)
    private String snmpCommunity;
    @JsonField(value = SNMP_AUTHENTICATION_PROTOCOL, converterClass = EnumConverter.class)
    private SnmpAuthenticationProtocol snmpAuthenticationProtocol;
    @JsonField(value = SNMP_PRIVACY_PROTOCOL, converterClass = EnumConverter.class)
    private SnmpPrivacyProtocol snmpPrivacyProtocol;
    @JsonField(value = SNMP_PRIVACY_PASSPHRASE, converterClass = Base64Converter.class)
    private String snmpPrivacyPassphrase;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NotifierType getNotifierType() {
        return this.notifierType;
    }

    public void setNotifierType(NotifierType notifierType) {
        this.notifierType = notifierType;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return this.port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public SmtpConnectionSecurity getSmtpConnectionSecurity() {
        return this.smtpConnectionSecurity;
    }

    public void setSmtpConnectionSecurity(SmtpConnectionSecurity smtpConnectionSecurity) {
        this.smtpConnectionSecurity = smtpConnectionSecurity;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSmtpFromAddress() {
        return this.smtpFromAddress;
    }

    public void setSmtpFromAddress(String smtpFromAddress) {
        this.smtpFromAddress = smtpFromAddress;
    }

    public String getSmtpFromName() {
        return this.smtpFromName;
    }

    public void setSmtpFromName(String smtpFromName) {
        this.smtpFromName = smtpFromName;
    }

    public SnmpVersion getSnmpVersion() {
        return this.snmpVersion;
    }

    public void setSnmpVersion(SnmpVersion snmpVersion) {
        this.snmpVersion = snmpVersion;
    }

    public String getSnmpCommunity() {
        return this.snmpCommunity;
    }

    public void setSnmpCommunity(String snmpCommunity) {
        this.snmpCommunity = snmpCommunity;
    }

    public SnmpAuthenticationProtocol getSnmpAuthenticationProtocol() {
        return this.snmpAuthenticationProtocol;
    }

    public void setSnmpAuthenticationProtocol(SnmpAuthenticationProtocol snmpAuthenticationProtocol) {
        this.snmpAuthenticationProtocol = snmpAuthenticationProtocol;
    }

    public SnmpPrivacyProtocol getSnmpPrivacyProtocol() {
        return this.snmpPrivacyProtocol;
    }

    public void setSnmpPrivacyProtocol(SnmpPrivacyProtocol snmpPrivacyProtocol) {
        this.snmpPrivacyProtocol = snmpPrivacyProtocol;
    }

    public String getSnmpPrivacyPassphrase() {
        return snmpPrivacyPassphrase;
    }

    public void setSnmpPrivacyPassphrase(String snmpPrivacyPassphrase) {
        this.snmpPrivacyPassphrase = snmpPrivacyPassphrase;
    }


}
