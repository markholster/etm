package com.jecstar.etm.launcher.migrations.v3;

import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetStoredScriptRequestBuilder;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptResponse;

public class Elasticsearch65PainlessSupport extends AbstractEtmMigrator {

    private final DataRepository dataRepository;

    public Elasticsearch65PainlessSupport(final DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public boolean shouldBeExecuted() {
        GetStoredScriptResponse response = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-event"));
        return response != null && !response.getSource().getSource().endsWith("mainMethod(ctx, params);");
    }

    @Override
    public void migrate() {
        // Nothing to do, the system must be reinitialized to update the painless scripts.
    }
}
