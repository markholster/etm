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

package com.jecstar.etm.gui.rest;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.rest.AbstractJsonService;
import com.jecstar.etm.server.core.util.DateUtils;
import com.jecstar.etm.server.core.util.IdGenerator;
import org.elasticsearch.action.get.GetResponse;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.time.format.DateTimeFormatter;

public abstract class AbstractGuiService extends AbstractJsonService {

    @Context
    protected SecurityContext securityContext;

    /**
     * A <code>DateTimeFormatter</code> that is configured to have week precision.
     */
    protected final DateTimeFormatter dateTimeFormatterIndexPerWeek = DateUtils.getIndexPerWeekFormatter();

    /**
     * An <code>IdGenerator</code> that will be used to create id's for audit logs.
     */
    protected final IdGenerator idGenerator = new IdGenerator();

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
