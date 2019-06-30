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
