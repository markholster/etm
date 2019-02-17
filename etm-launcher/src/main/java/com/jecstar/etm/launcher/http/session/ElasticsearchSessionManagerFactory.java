package com.jecstar.etm.launcher.http.session;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SessionManagerFactory;

public class ElasticsearchSessionManagerFactory implements SessionManagerFactory {

    private final DataRepository dataRepository;
    private final EtmConfiguration etmConfiguration;

    public ElasticsearchSessionManagerFactory(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        this.dataRepository = dataRepository;
        this.etmConfiguration = etmConfiguration;
    }

    @Override
    public SessionManager createSessionManager(Deployment deployment) {
        return new ElasticsearchSessionManager(this.dataRepository, deployment.getDeploymentInfo().getSessionIdGenerator(), deployment.getDeploymentInfo().getDeploymentName(), this.etmConfiguration);
    }

}
