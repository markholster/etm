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

package com.jecstar.etm.processor.elastic.apm.domain.converter;

import com.jecstar.etm.processor.elastic.apm.domain.Framework;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;

/**
 * Converter class for the <code>Framework</code> class.
 */
public class FrameworkConverter extends NestedObjectConverter<Framework> {

    public FrameworkConverter() {
        super(f -> new Framework());
    }
}