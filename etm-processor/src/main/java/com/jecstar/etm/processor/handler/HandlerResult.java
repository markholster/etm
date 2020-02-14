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

package com.jecstar.etm.processor.handler;

public class HandlerResult {

    enum Status {PROCESSED, PARSE_FAILURE, FAILED;}

    private Status status;

    private Exception exception;
    private String mesage;

    public boolean isFailed() {
        return !Status.PROCESSED.equals(this.status);
    }

    public boolean hasParseFailure() {
        return Status.PARSE_FAILURE.equals(this.status);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Status: " + this.status.name());
        if (this.mesage != null) {
            result.append(", message: " + this.mesage);
        }
        if (this.exception != null) {
            result.append(", exception:" + this.exception);
        }
        return result.toString();
    }

    public static HandlerResult processed() {
        HandlerResult result = new HandlerResult();
        result.status = Status.PROCESSED;
        return result;
    }

    public static HandlerResult parserFailure(Exception exception) {
        HandlerResult result = new HandlerResult();
        result.status = Status.PARSE_FAILURE;
        result.exception = exception;
        return result;
    }

    public static HandlerResult failed(String message) {
        HandlerResult result = new HandlerResult();
        result.status = Status.FAILED;
        result.mesage = message;
        return result;
    }
}
