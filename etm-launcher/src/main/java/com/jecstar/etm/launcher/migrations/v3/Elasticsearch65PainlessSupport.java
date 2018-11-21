package com.jecstar.etm.launcher.migrations.v3;

import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptAction;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptRequestBuilder;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptResponse;
import org.elasticsearch.client.Client;

public class Elasticsearch65PainlessSupport extends AbstractEtmMigrator {

    private final Client client;

    public Elasticsearch65PainlessSupport(Client client) {
        this.client = client;
    }

    @Override
    public boolean shouldBeExecuted() {
        GetStoredScriptResponse response = new GetStoredScriptRequestBuilder(this.client, GetStoredScriptAction.INSTANCE)
                .setId("etm_update-event")
                .get();
        return !response.getSource().getSource().endsWith("mainMethod(params);");
    }

    @Override
    public void migrate() {
        // Nothing to do, the system must be reinitialized to update the painless scripts.
    }
}
