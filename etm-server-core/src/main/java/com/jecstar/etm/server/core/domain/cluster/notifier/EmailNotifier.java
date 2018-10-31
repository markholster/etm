package com.jecstar.etm.server.core.domain.cluster.notifier;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.JsonNamespace;
import com.jecstar.etm.server.core.converter.custom.Base64Converter;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;

@JsonNamespace(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NOTIFIER)
public class EmailNotifier extends Notifier {

    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public static final String SMTP_CONNECTION_SECURITY = "smtp_connection_security";
    public static final String SMTP_FROM_ADDRESS = "smtp_from_address";
    public static final String SMTP_FROM_NAME = "smtp_from_name";

    @JsonField(HOST)
    private String host;
    @JsonField(PORT)
    private Integer port;
    @JsonField(USERNAME)
    private String username;
    @JsonField(value = PASSWORD, converterClass = Base64Converter.class)
    private String password;

    @JsonField(value = SMTP_CONNECTION_SECURITY, converterClass = EnumConverter.class)
    private SmtpConnectionSecurity smtpConnectionSecurity;
    @JsonField(SMTP_FROM_ADDRESS)
    private String smtpFromAddress;
    @JsonField(SMTP_FROM_NAME)
    private String smtpFromName;

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

}
