package com.jecstar.etm.gui.rest;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.rest.AbstractJsonService;
import org.elasticsearch.action.get.GetResponse;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

public abstract class AbstractGuiService extends AbstractJsonService {

    @Context
    protected SecurityContext securityContext;

    private final EtmPrincipalConverterJsonImpl principalConverter = new EtmPrincipalConverterJsonImpl();

    protected EtmPrincipal getEtmPrincipal() {
        return (EtmPrincipal) this.securityContext.getUserPrincipal();
    }

    /**
     * Loads an <code>EtmGroup</code> based on it's name.
     *
     * @param groupName The name of the <code>EtmGroup</code> to load.
     * @return The <code>EtmGroup</code> with the given groupName, or <code>null</code> when no such group exists.
     */
    protected EtmGroup getEtmGroup(DataRepository dataRepository, String groupName) {
        if (groupName == null) {
            return null;
        }
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName));
        if (!getResponse.isExists()) {
            return null;
        }
        return this.principalConverter.readGroup(getResponse.getSourceAsMap());
    }

}
