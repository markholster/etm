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

package com.jecstar.etm.gui.rest.export;

import java.util.Comparator;
import java.util.List;

public enum MultiSelect {

    LOWEST() {
        @Override
        Object select(List<Object> values) {
            if (values == null || values.isEmpty()) {
                return null;
            }
            values.sort(objectComparator);
            return values.get(0);
        }
    }, HIGHEST() {
        @Override
        Object select(List<Object> values) {
            if (values == null || values.isEmpty()) {
                return null;
            }
            values.sort(objectComparator);
            return values.get(values.size() - 1);
        }
    }, FIRST() {
        @Override
        Object select(List<Object> values) {
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.get(0);
        }
    }, LAST() {
        @Override
        Object select(List<Object> values) {
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.get(values.size() - 1);
        }
    };

    abstract Object select(List<Object> values);

    private static final Comparator<Object> objectComparator = (o1, o2) -> o1.toString().compareTo(o2.toString());
}
