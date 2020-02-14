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

package com.jecstar.etm.server.core.domain.principal;

/**
 * A class representing a user or group in Enterprise Telemetry Monitor.
 */
public interface EtmSecurityEntity {

    /**
     * Gives the type of <code>EtmSecurityEntity</code>.
     *
     * @return The type of <code>EtmSecurityEntity</code>.
     */
    String getType();

    /**
     * Gives the unique id of the <code>EtmSecurityEntity</code>.
     *
     * @return The id of the <code>EtmSecurityEntity</code>.
     */
    String getId();

    /**
     * Gives a name of the <code>EtmSecurityEntity</code> that can be used to display to an end user.
     *
     * @return The display name of the <code>EtmSecurityEntity</code>.
     */
    String getDisplayName();

    /**
     * Method that checks if the <code>EtmSecurityEntity</code> has access rights to the given datasource in the context fo Dashboards.
     *
     * @param datasourceName The name of the datasource.
     * @return <code>true</code> when the datasource is available to the user to configure Graphs on, <code>false</code> otherwise.
     */
    boolean isAuthorizedForDashboardDatasource(String datasourceName);

    /**
     * Method that checks if the <code>EtmSecurityEntity</code> has access rights to the given datasource in the context fo Signals.
     *
     * @param datasourceName The name of the datasource.
     * @return <code>true</code> when the datasource is available to the user to configure Signals on, <code>false</code> otherwise.
     */
    boolean isAuthorizedForSignalDatasource(String datasourceName);

    /**
     * Method that checks if the <code>EtmSecurityEntity</code> has access rights to the given notifier.
     *
     * @param notifierName The name of the notifier.
     * @return <code>true</code> when the notifier is available to the user, <code>false</code> otherwise.
     */
    boolean isAuthorizedForNotifier(String notifierName);

}
