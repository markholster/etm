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

package com.jecstar.etm.launcher.migrations;

/**
 * Interface for all migrators.
 * <p>
 * A migrator is a piece of code that runs before ETM is fully started. It's purpose is to migrate the environment in
 * such a way that it is capable of running against that environment. For example, the upgrade from Elasticsearch 5.x to
 * Elasticsearch 6.x results in a change in the ETM mappings and hence needs to be converted at startup.
 */
public interface EtmMigrator {

    /**
     * Determine if this particular <code>EtmMigrator</code> instance should be executed.
     *
     * @return <code>true</code> when this <code>EtmMigrator</code> should be executed. <code>false</code> otherwise.
     */
    boolean shouldBeExecuted();

    /**
     * Run the migration.
     */
    void migrate();

}
