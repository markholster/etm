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
        ETM_BUSINESS_EVENT, EMAIL;

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

    public enum ConnectionSecurity {

        SSL_TLS, STARTTLS;

        public static ConnectionSecurity safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return ConnectionSecurity.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static final String NAME = "name";
    public static final String NOTIFIER_TYPE = "type";
    public static final String SMTP_HOST = "smtp_host";
    public static final String SMTP_PORT = "smtp_port";
    public static final String CONNECTION_SECURITY = "connection_security";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String FROM_ADDRESS = "from_address";
    public static final String FROM_NAME = "from_name";

    @JsonField(NAME)
    private String name;
    @JsonField(value = NOTIFIER_TYPE, converterClass = EnumConverter.class)
    private NotifierType notifierType;


    // Email fields
    @JsonField(SMTP_HOST)
    private String smtpHost;
    @JsonField(SMTP_PORT)
    private Integer smtpPort;
    @JsonField(value = CONNECTION_SECURITY, converterClass = EnumConverter.class)
    private ConnectionSecurity connectionSecurity;
    @JsonField(USERNAME)
    private String username;
    @JsonField(value = PASSWORD, converterClass = Base64Converter.class)
    private String password;
    @JsonField(FROM_ADDRESS)
    private String fromAddress;
    @JsonField(FROM_NAME)
    private String fromName;

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

    public String getSmtpHost() {
        return this.smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return this.smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public ConnectionSecurity getConnectionSecurity() {
        return this.connectionSecurity;
    }

    public void setConnectionSecurity(ConnectionSecurity connectionSecurity) {
        this.connectionSecurity = connectionSecurity;
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

    public String getFromAddress() {
        return this.fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return this.fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}
