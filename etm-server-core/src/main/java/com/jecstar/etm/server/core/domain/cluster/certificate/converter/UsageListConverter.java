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

package com.jecstar.etm.server.core.domain.cluster.certificate.converter;

import com.jecstar.etm.server.core.converter.custom.NestedListEnumConverter;
import com.jecstar.etm.server.core.domain.cluster.certificate.Usage;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class UsageListConverter extends NestedListEnumConverter<Usage, List<Usage>> {

    private static final LogWrapper log = LogFactory.getLogger(UsageListConverter.class);

    public UsageListConverter() {
        super(s -> {
            try {
                Method safeValueOf = Usage.class.getDeclaredMethod("safeValueOf", String.class);
                return (Usage) safeValueOf.invoke(null, s);
            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                log.logErrorMessage("Unable to convert value '" + s + "'.", e);
                return null;
            }
        });
    }
}
