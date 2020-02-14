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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HandlerResults {

    private List<HandlerResult> results = new ArrayList<>();

    public void addHandlerResult(HandlerResult result) {
        this.results.add(result);
    }

    public boolean hasFailures() {
        return this.results.stream().anyMatch(HandlerResult::isFailed);
    }

    public boolean hasParseFailures() {
        return this.results.stream().anyMatch(HandlerResult::hasParseFailure);
    }

    public List<HandlerResult> getFailures() {
        return this.results.stream().filter(HandlerResult::isFailed).collect(Collectors.toList());
    }
}
