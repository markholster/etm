package com.jecstar.etm.signaler;

import com.consol.citrus.TestCaseMetaInfo;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.junit.jupiter.CitrusExtension;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.mail.message.MailMessage;
import com.jecstar.etm.server.core.domain.cluster.notifier.EmailNotifier;
import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.signaler.domain.Signal;
import com.jecstar.etm.signaler.domain.converter.SignalConverter;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.*;

/**
 * Test class for the <code>EmailSignal</code> class.
 */
@ExtendWith(CitrusExtension.class)
public class EmailSignalTest {


    @Test
    @CitrusTest
    public void testSendExceedanceNotification(@CitrusResource TestRunner runner) {
        runner.author("Mark Holster");
        runner.description("Test sending the exceedance notification by email");
        runner.status(TestCaseMetaInfo.Status.FINAL);
        runner.creationDate(
                Date.from(LocalDate.of(2018, Month.OCTOBER, 9)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                )
        );

        EmailNotifier notifier = new EmailNotifier();
        notifier.setNotifierType(Notifier.NotifierType.EMAIL);
        notifier.setHost("127.0.0.1");
        notifier.setPort(CitrusConfiguration.MAILSERVER_PORT);
        notifier.setSmtpFromAddress("signal@jecstar.com");

        List<String> recipients = new ArrayList<>();
        recipients.add("info@jecstar.com");

        Map<String, Object> signalDataMap = new HashMap<>();
        signalDataMap.put(Signal.EMAIL_RECIPIENTS, recipients);
        signalDataMap.put(Signal.NAME, "Signal Test");
        signalDataMap.put(Signal.THRESHOLD, 1);
        signalDataMap.put(Signal.LIMIT, 3);
        signalDataMap.put(Signal.EMAIL_ALL_ETM_GROUP_MEMBERS, false);

        SignalConverter signalConverter = new SignalConverter();
        Signal signal = signalConverter.read(signalDataMap);


        DateTime now = DateTime.now();
        Map<DateTime, Double> thresholdExceedances = new HashMap<>();
        thresholdExceedances.put(now.minusHours(1), 4D);
        thresholdExceedances.put(now.minusHours(2), 5D);
        thresholdExceedances.put(now.minusHours(3), 6D);
        thresholdExceedances.put(now.minusHours(4), 7D);


        try (EmailSignal emailSignal = new EmailSignal()) {
            emailSignal.sendExceedanceNotification(null, null, "Etm Test Cluster", signal, notifier, thresholdExceedances, null);

            runner.receive(b -> b.endpoint("mailServer")
                    .message(MailMessage.request()
                            .from("signal@jecstar.com")
                            .to("info@jecstar.com")
                            .cc("")
                            .bcc("")
                            .subject("[Etm Test Cluster] - Signal: Signal Test")
                            .body("Hi,\n" +
                                    "\n" +
                                    "The threshold (1) of signal 'Signal Test' has exceeded 4 times which tops the configured limit of 3.\n" +
                                    "\n" +
                                    "The following exceedances are recorded:\n" +
                                    emailSignal.defaultDateFormat.format(now.minusHours(4).toDate()) + ": " + emailSignal.defaultNumberFormat.format(thresholdExceedances.get(now.minusHours(4))) + "\n" +
                                    emailSignal.defaultDateFormat.format(now.minusHours(3).toDate()) + ": " + emailSignal.defaultNumberFormat.format(thresholdExceedances.get(now.minusHours(3))) + "\n" +
                                    emailSignal.defaultDateFormat.format(now.minusHours(2).toDate()) + ": " + emailSignal.defaultNumberFormat.format(thresholdExceedances.get(now.minusHours(2))) + "\n" +
                                    emailSignal.defaultDateFormat.format(now.minusHours(1).toDate()) + ": " + emailSignal.defaultNumberFormat.format(thresholdExceedances.get(now.minusHours(1))) + "\n" +
                                    "\n" +
                                    "Kind regards,\n" +
                                    "Your Enterprise Telemetry Monitor administrator", "text/plain; charset=us-ascii"))
            );

            runner.send(b -> b.endpoint("mailServer")
                    .message(MailMessage.response(250, "OK"))
            );
        }

    }


    @Test
    @CitrusTest
    public void testSendNoLongerExceededNotification(@CitrusResource TestRunner runner) {
        runner.author("Mark Holster");
        runner.description("Test sending the signal no longer exceeded notification by email");
        runner.status(TestCaseMetaInfo.Status.FINAL);
        runner.creationDate(
                Date.from(LocalDate.of(2018, Month.OCTOBER, 9)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                )
        );


        EmailNotifier notifier = new EmailNotifier();
        notifier.setNotifierType(Notifier.NotifierType.EMAIL);
        notifier.setHost("127.0.0.1");
        notifier.setPort(CitrusConfiguration.MAILSERVER_PORT);
        notifier.setSmtpFromAddress("signal@jecstar.com");

        List<String> recipients = new ArrayList<>();
        recipients.add("info@jecstar.com");

        Map<String, Object> signalDataMap = new HashMap<>();
        signalDataMap.put(Signal.EMAIL_RECIPIENTS, recipients);
        signalDataMap.put(Signal.NAME, "Signal Test");
        signalDataMap.put(Signal.THRESHOLD, 1);
        signalDataMap.put(Signal.EMAIL_ALL_ETM_GROUP_MEMBERS, false);

        SignalConverter signalConverter = new SignalConverter();
        Signal signal = signalConverter.read(signalDataMap);

        try (EmailSignal emailSignal = new EmailSignal()) {
            emailSignal.sendNoLongerExceededNotification(null, null, "Etm Test Cluster", signal, notifier, null);

            runner.receive(b -> b.endpoint("mailServer")
                    .message(MailMessage.request()
                            .from("signal@jecstar.com")
                            .to("info@jecstar.com")
                            .cc("")
                            .bcc("")
                            .subject("[Etm Test Cluster] - Signal fixed: Signal Test")
                            .body("Hi,\n" +
                                    "\n" +
                                    "The threshold (1) of signal 'Signal Test' is no longer exceeded.\n" +
                                    "\n" +
                                    "Kind regards,\n" +
                                    "Your Enterprise Telemetry Monitor administrator", "text/plain; charset=us-ascii"))
            );

            runner.send(b -> b.endpoint("mailServer")
                    .message(MailMessage.response(250, "OK"))
            );
        }

    }
}
