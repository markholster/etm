package com.jecstar.etm.server.core.domain.cluster.notifier;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.JsonNamespace;
import com.jecstar.etm.server.core.converter.custom.Base64Converter;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;
import com.jecstar.etm.server.core.domain.cluster.certificate.Usage;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.tls.ElasticBackedTrustManager;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

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

    @Override
    public ConnectionTestResult testConnection(DataRepository dataRepository) {
        Session mailSession;
        if (getUsername() != null || getPassword() != null) {
            mailSession = Session.getInstance(getConnectionProperties(dataRepository), new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(getUsername(), getPassword());
                }

            });
        } else {
            mailSession = Session.getInstance(getConnectionProperties(dataRepository));
        }
        try (var transport = mailSession.getTransport()) {
            transport.connect();
        } catch (MessagingException e) {
            return new ConnectionTestResult(e.getMessage());
        }
        return ConnectionTestResult.OK;
    }


    /**
     * Gives the <code>Properties</code> instances that can be used to setup a connection to the SMTP server configured in this <code>Notifier</code>.
     *
     * @param dataRepository The <code>DataRepository</code> that stores the trusted ssl certificats.
     * @return A <code>Properties</code> instance with the JavaMail connection configuration.
     */
    public Properties getConnectionProperties(DataRepository dataRepository) {
        var mailProps = new Properties();
        mailProps.put("mail.transport.protocol", "smtp");
        mailProps.put("mail.smtp.host", getHost());
        mailProps.put("mail.smtp.port", getPort());
        mailProps.put("mail.smtp.auth", getUsername() != null || getPassword() != null ? "true" : "false");
        if (EmailNotifier.SmtpConnectionSecurity.STARTTLS.equals(getSmtpConnectionSecurity())) {
            mailProps.put("mail.smtp.starttls.enable", "true");
            mailProps.put("mail.smtp.ssl.socketFactory", createSslSocketFactory(dataRepository));
        } else if (EmailNotifier.SmtpConnectionSecurity.SSL_TLS.equals(getSmtpConnectionSecurity())) {
            mailProps.put("mail.smtp.ssl.enable", "true");
            mailProps.put("mail.smtp.ssl.socketFactory", createSslSocketFactory(dataRepository));
        }
        return mailProps;
    }

    /**
     * Creates a new <code>SSLSocketFactory</code> that can be used for creating secure SMTP connection.s
     *
     * @param dataRepository The <code>DataRepository</code> that contains the trusted certificates.
     * @return A <code>SSLSocketFactory</code> for setting up secure SMTP connections.
     */
    private SSLSocketFactory createSslSocketFactory(DataRepository dataRepository) {
        try {
            var sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[]{new ElasticBackedTrustManager(Usage.SMTP, dataRepository)}, null);
            return sslcontext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

}
