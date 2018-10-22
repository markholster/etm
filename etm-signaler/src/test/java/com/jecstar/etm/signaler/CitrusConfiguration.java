package com.jecstar.etm.signaler;

import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.mail.server.MailServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CitrusConfiguration {

    public static final int MAILSERVER_PORT = 2025;

    @Bean(name = "mailServer")
    public MailServer mailServer() {
        return CitrusEndpoints.mail()
                .server()
                .port(MAILSERVER_PORT)
                .autoAccept(true)
                .autoStart(true)
                .build();
    }
}
