/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.cluster.notifier.EmailNotifier;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.EtmSecurityEntity;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.signaler.domain.Signal;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Class that sends an email to all recipients of a <code>Signal</code>.
 */
class EmailSignal implements Closeable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(EmailSignal.class);

    private final JsonConverter jsonConverter = new JsonConverter();
    private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags etmPrincipalTags = this.etmPrincipalConverter.getTags();
    final DateTimeFormatter defaultDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    final NumberFormat defaultNumberFormat = NumberFormat.getInstance();
    private final Map<String, SessionAndTransport> serverConnections = new HashMap<>();

    /**
     * Send an exceedance notification of a <code>Signal</code> by email.
     *
     * @param dataRepository      The <code>DataRepository</code>.
     * @param etmConfiguration     The <code>EtmConfiguration</code> instance.
     * @param clusterName          The name of the ETM cluster.
     * @param signal               The <code>Signal</code> of which the threshold is exceeded more often that the configured limit.
     * @param notifier             The <code>Notifier</code> to be used to send the email.
     * @param thresholdExceedances A <code>Map</code> with dates and their values when the threshold was exceeded.
     * @param etmSecurityEntity    An <code>EtmSecurityEntity</code> to which the <code>Signal</code> belongs.
     */
    void sendExceedanceNotification(DataRepository dataRepository,
                                    EtmConfiguration etmConfiguration,
                                    String clusterName,
                                    Signal signal,
                                    EmailNotifier notifier,
                                    Map<ZonedDateTime, Double> thresholdExceedances,
                                    EtmSecurityEntity etmSecurityEntity
    ) {
        try {
            SessionAndTransport sessionAndTransport = getSessionAndTransport(dataRepository, notifier);
            for (String recipient : determineRecipients(dataRepository, etmConfiguration, signal, etmSecurityEntity)) {
                final String subject = "[" + clusterName + "] - Signal: " + signal.getName();
                InternetAddress toAddress = new InternetAddress(recipient);
                DateTimeFormatter dateFormat = this.defaultDateFormat;
                NumberFormat numberFormat = this.defaultNumberFormat;
                EtmPrincipal etmRecipient = getUserByEmail(dataRepository, etmConfiguration, recipient);
                if (etmRecipient != null) {
                    dateFormat = etmRecipient.getISO8601DateFormat();
                    numberFormat = etmRecipient.getNumberFormat();
                    toAddress = new InternetAddress(recipient, etmRecipient.getName());
                }
                StringBuilder messageContent = new StringBuilder();
                if (etmRecipient != null) {
                    messageContent.append("Hi " + etmRecipient.getName() + ",\r\n\r\n");
                } else {
                    messageContent.append("Hi,\r\n\r\n");
                }
                messageContent.append("The threshold (" + signal.getThreshold().getValue() + ") of signal '" + signal.getName()
                        + "' has exceeded " + thresholdExceedances.size() + " times which tops the configured max frequency of exceedance of "
                        + signal.getNotifications().getMaxFrequencyOfExceedance() + ".\r\n");
                messageContent.append("\r\n");
                messageContent.append("The following exceedances are recorded:\r\n");
                ArrayList<ZonedDateTime> dateTimes = new ArrayList<>(thresholdExceedances.keySet());
                Collections.sort(dateTimes);
                for (ZonedDateTime dateTime : dateTimes) {
                    messageContent.append(dateFormat.format(dateTime) + ": " + numberFormat.format(thresholdExceedances.get(dateTime)) + "\r\n");
                }
                messageContent.append("\r\n");
                messageContent.append("Kind regards,\r\n");
                messageContent.append("Your Enterprise Telemetry Monitor administrator");

                MimeMessage message = createMimeMessage(sessionAndTransport.session, notifier, toAddress, subject, messageContent.toString());
                sessionAndTransport.transport.sendMessage(message, message.getAllRecipients());
            }
        } catch (UnsupportedEncodingException | MessagingException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unable to send signal mail.", e);
            }
        }
    }


    /**
     * Send a no longer exceedance notification of a <code>Signal</code> by email.
     *
     * @param dataRepository    The <code>DataRepository</code>.
     * @param etmConfiguration  The <code>EtmConfiguration</code> instance.
     * @param clusterName       The name of the ETM cluster.
     * @param signal            The <code>Signal</code> of which the threshold is no longer exceeded.
     * @param notifier          The <code>Notifier</code> to be used to send the email.
     * @param etmSecurityEntity An <code>etmSecurityEntity</code> to which the <code>Signal</code> belongs. <code>null</code> if the
     */
    void sendNoLongerExceededNotification(DataRepository dataRepository,
                                          EtmConfiguration etmConfiguration,
                                          String clusterName,
                                          Signal signal,
                                          EmailNotifier notifier,
                                          EtmSecurityEntity etmSecurityEntity
    ) {
        try {
            SessionAndTransport sessionAndTransport = getSessionAndTransport(dataRepository, notifier);
            for (String recipient : determineRecipients(dataRepository, etmConfiguration, signal, etmSecurityEntity)) {
                final String subject = "[" + clusterName + "] - Signal fixed: " + signal.getName();
                InternetAddress toAddress = new InternetAddress(recipient);
                EtmPrincipal etmRecipient = getUserByEmail(dataRepository, etmConfiguration, recipient);
                if (etmRecipient != null) {
                    toAddress = new InternetAddress(recipient, etmRecipient.getName());
                }
                StringBuilder messageContent = new StringBuilder();
                if (etmRecipient != null) {
                    messageContent.append("Hi " + etmRecipient.getName() + ",\r\n\r\n");
                } else {
                    messageContent.append("Hi,\r\n\r\n");
                }
                messageContent.append("The threshold (" + signal.getThreshold().getValue() + ") of signal '" + signal.getName()
                        + "' is no longer exceeded.\r\n");
                messageContent.append("\r\n");
                messageContent.append("Kind regards,\r\n");
                messageContent.append("Your Enterprise Telemetry Monitor administrator");

                MimeMessage message = createMimeMessage(sessionAndTransport.session, notifier, toAddress, subject, messageContent.toString());
                sessionAndTransport.transport.sendMessage(message, message.getAllRecipients());
            }
        } catch (UnsupportedEncodingException | MessagingException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unable to send signal mail.", e);
            }
        }

    }

    /**
     * Creates a default <code>MimeMessage</code>.
     *
     * @param session   The <code>Session</code> used to create the <code>MimeMessage</code>
     * @param notifier  The <code>Notifier</code> used to set the from address.
     * @param toAddress The recipient.
     * @param subject   The subject of the email.
     * @param body      The text body of the email.
     * @return A saved <code>MimeMessage</code>.
     * @throws UnsupportedEncodingException If the name of sender cannot be encoded in the default encoding.
     * @throws MessagingException           When any other failure occurs.
     */
    private MimeMessage createMimeMessage(Session session, EmailNotifier notifier, InternetAddress toAddress, String subject, String body) throws UnsupportedEncodingException, MessagingException {
        MimeMessage message = new MimeMessage(session);
        if (notifier.getName() != null) {
            message.setFrom(new InternetAddress(notifier.getSmtpFromAddress(), notifier.getSmtpFromName()));
        } else {
            message.setFrom(new InternetAddress(notifier.getSmtpFromAddress()));
        }

        message.addRecipient(Message.RecipientType.TO, toAddress);
        message.setSubject(subject);
        message.setText(body);
        message.saveChanges();
        return message;
    }


    /**
     * Determine all email recipients of a <code>Signal</code> and the owning <code>EtmGroup</code>.
     *
     * @param dataRepository   The <code>DataRepository</code>.
     * @param etmConfiguration  The <code>EtmConfiguration</code> instance.
     * @param signal            The <code>Signal</code> to retrieve the recipients for.
     * @param etmSecurityEntity The <code>EtmSecurityEntity</code> if the given <code>Signal</code> is owned by an <code>EtmGroup</code>.
     * @return A <code>Set</code> with email addresses that should receive an email.
     */
    private Set<String> determineRecipients(DataRepository dataRepository, EtmConfiguration etmConfiguration, Signal signal, EtmSecurityEntity etmSecurityEntity) {
        EtmGroup etmGroup = null;
        if (etmSecurityEntity instanceof EtmGroup) {
            etmGroup = (EtmGroup) etmSecurityEntity;
        }
        Set<String> recipients = new HashSet<>(signal.getNotifications().getEmailRecipients());
        if (signal.getNotifications().getEmailAllEtmGroupMembers() != null && signal.getNotifications().getEmailAllEtmGroupMembers() && etmGroup != null) {
            ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                    .setQuery(QueryBuilders.boolQuery()
                            .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                            .must(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getGroupsTag(), etmGroup.getName()))
                    )
                    .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                    .setFetchSource(new String[]{ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getEmailTag()}, null),
                    null
            );
            for (var searchHit : scrollableSearch) {
                Map<String, Object> userValues = this.jsonConverter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, searchHit.getSourceAsMap());
                String email = this.jsonConverter.getString(this.etmPrincipalTags.getEmailTag(), userValues);
                if (email != null) {
                    recipients.add(email);
                }
            }
        }
        return recipients;
    }

    /**
     * Get a user by it's email address.
     *
     * @param dataRepository  The <code>DataRepository</code>.
     * @param etmConfiguration The <code>EtmConfiguration</code> instance.
     * @param email            The email address.
     * @return The first <code>EtmPrincipal</code> with the given email address, or <code>null</code> if no such principal found.
     */
    private EtmPrincipal getUserByEmail(DataRepository dataRepository, EtmConfiguration etmConfiguration, String email) {
        if (dataRepository == null || etmConfiguration == null || email == null) {
            // Should only be reached in test cases.
            return null;
        }
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getEmailTag(), email))
                )
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setFetchSource(true);
        SearchResponse searchResponse = dataRepository.search(searchRequestBuilder);
        if (searchResponse.getHits().getHits().length > 0) {
            return this.etmPrincipalConverter.readPrincipal(searchResponse.getHits().getAt(0).getSourceAsString());
        }
        return null;
    }

    /**
     * Gives a <code>SessionAndTransport</code> instance that belongs to a certain <code>Notifier</code>.
     *
     * @param dataRepository The <code>DataRepository</code> that contains the trusted certificates.
     * @param notifier The <code>Notifier</code> to retrieve the <code>SessionAndTransport</code> for.
     * @return The <code>SessionAndTransport</code> instance.
     */
    private SessionAndTransport getSessionAndTransport(final DataRepository dataRepository, final EmailNotifier notifier) {
        if (this.serverConnections.containsKey(notifier.getName())) {
            return this.serverConnections.get(notifier.getName());
        }
        Session mailSession;
        if (notifier.getUsername() != null || notifier.getPassword() != null) {
            mailSession = Session.getInstance(notifier.getConnectionProperties(dataRepository), new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(notifier.getUsername(), notifier.getPassword());
                }

            });
        } else {
            mailSession = Session.getInstance(notifier.getConnectionProperties(dataRepository));
        }
        try {
            Transport transport = mailSession.getTransport();
            transport.connect();
            SessionAndTransport sessionAndTransport = new SessionAndTransport(mailSession, transport);
            this.serverConnections.put(notifier.getName(), sessionAndTransport);
            return sessionAndTransport;
        } catch (MessagingException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    @Override
    public void close() {
        this.serverConnections.values().forEach(SessionAndTransport::close);
        this.serverConnections.clear();
    }

    /**
     * Class that holds a mail <code>Session</code> and a corresponding <code>Transport</code>
     */
    private class SessionAndTransport implements Closeable {

        private final Session session;
        private final Transport transport;

        SessionAndTransport(Session session, Transport transport) {
            this.session = session;
            this.transport = transport;
        }


        @Override
        public void close() {
            try {
                this.transport.close();
            } catch (MessagingException e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Failed to close smtp connection", e);
                }
            }
        }
    }

}
