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

package com.jecstar.etm.launcher.migrations.v4;

import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.DeleteStoredScriptRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.GetStoredScriptRequestBuilder;

/**
 * Migrator that cleans the index templates that are obsolete after the 4.1 migration.
 */
public class Version42Migrator extends AbstractEtmMigrator {

    private final DataRepository dataRepository;

    public Version42Migrator(final DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public boolean shouldBeExecuted() {
        var scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-search-template"));
        if (scriptResponse != null) {
            return true;
        }
        scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_remove-search-template"));
        if (scriptResponse != null) {
            return true;
        }
        scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-search-history"));
        if (scriptResponse != null) {
            return true;
        }
        return false;
    }

    @Override
    public void migrate() {
        if (!shouldBeExecuted()) {
            return;
        }
        System.out.println("Start removing old script templates.");
        long current = 0, lastPrint = 0, total = 3;
        var scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-search-template"));
        if (scriptResponse != null) {
            this.dataRepository.deleteStoredScript(new DeleteStoredScriptRequestBuilder("etm_update-search-template"));
        }
        lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
        scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_remove-search-template"));
        if (scriptResponse != null) {
            this.dataRepository.deleteStoredScript(new DeleteStoredScriptRequestBuilder("etm_remove-search-template"));
        }
        lastPrint = printPercentageWhenChanged(lastPrint, ++current, total);
        scriptResponse = this.dataRepository.getStoredScript(new GetStoredScriptRequestBuilder("etm_update-search-history"));
        if (scriptResponse != null) {
            this.dataRepository.deleteStoredScript(new DeleteStoredScriptRequestBuilder("etm_update-search-history"));
        }
        printPercentageWhenChanged(lastPrint, ++current, total);
        System.out.println("Done removing old script templates.");
    }

}
