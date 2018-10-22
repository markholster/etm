package com.jecstar.etm.gui.rest;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.rest.AbstractJsonService;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

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
    protected EtmGroup getEtmGroup(Client client, String groupName) {
        if (groupName == null) {
            return null;
        }
        GetResponse getResponse = client.prepareGet(
                ElasticsearchLayout.CONFIGURATION_INDEX_NAME,
                ElasticsearchLayout.ETM_DEFAULT_TYPE,
                ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP_ID_PREFIX + groupName)
                .get();
        if (!getResponse.isExists()) {
            return null;
        }
        return this.principalConverter.readGroup(getResponse.getSourceAsMap());
    }

}
