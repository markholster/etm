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

package com.jecstar.etm.server.core.domain.cluster.notifier;

public class ConnectionTestResult {

    public static final ConnectionTestResult OK = new ConnectionTestResult();

    private static byte SUCCESS = 0;
    private static byte FAILED = 1;

    private final byte status;
    private final String errorMessage;

    private ConnectionTestResult() {
        this.status = SUCCESS;
        this.errorMessage = null;
    }

    public ConnectionTestResult(String errorMessage) {
        this.status = FAILED;
        this.errorMessage = errorMessage;
    }

    public boolean isFailed() {
        return this.status == FAILED;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
