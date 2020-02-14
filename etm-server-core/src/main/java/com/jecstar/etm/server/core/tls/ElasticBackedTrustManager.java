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

package com.jecstar.etm.server.core.tls;

import com.jecstar.etm.server.core.domain.cluster.certificate.Certificate;
import com.jecstar.etm.server.core.domain.cluster.certificate.Usage;
import com.jecstar.etm.server.core.domain.cluster.certificate.converter.CertificateConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ElasticBackedTrustManager extends AbstractConfigurableTrustManager {


    private final DataRepository dataRepository;
    private final CertificateConverter converter = new CertificateConverter();

    public ElasticBackedTrustManager(Usage usage, DataRepository dataRepository) {
        super(usage);
        this.dataRepository = dataRepository;
    }

    @Override
    protected Set<Certificate> loadCertificates(Usage usage) {
        var searchRequestBuilder = new SearchRequestBuilder().setIndices(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setFetchSource(true)
                .setQuery(new BoolQueryBuilder()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE))
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE + "." + Certificate.USAGE, usage.name()))
                );
        var scrollableSearch = new ScrollableSearch(this.dataRepository, searchRequestBuilder);
        if (!scrollableSearch.hasNext()) {
            return Collections.emptySet();
        }
        var result = new HashSet<Certificate>();
        for (var searchHit : scrollableSearch) {
            result.add(this.converter.read(searchHit.getSourceAsMap()));
        }
        return result;
    }
}
