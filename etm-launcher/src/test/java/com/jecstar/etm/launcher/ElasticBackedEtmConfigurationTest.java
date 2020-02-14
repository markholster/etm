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

package com.jecstar.etm.launcher;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the <code>ElasticBackedEtmConfiguration</code> class.
 *
 * @author mark
 */
public class ElasticBackedEtmConfigurationTest {

    /**
     * Test if the <code>ElasticBackedEtmConfiguration</code> overrides all
     * getter methods of the <code>EtmConfiguration</code> class.
     */
    @Test
    public void testAllConfigurationKeysDefined() {
        final List<String> methodsToIgnore = Arrays.asList(
                "protected boolean isValidLicenseKey(java.lang.String)"
                , "public java.lang.String getNodeName()"
                , "public void addConfigurationChangeListener(com.jecstar.etm.server.core.domain.configuration.ConfigurationChangeListener)"
                , "public void removeConfigurationChangeListener(com.jecstar.etm.server.core.domain.configuration.ConfigurationChangeListener)"
                , "public synchronized boolean mergeAndNotify(com.jecstar.etm.server.core.domain.configuration.EtmConfiguration)"
                , "public com.jecstar.etm.server.core.domain.configuration.LicenseRateLimiter getLicenseRateLimiter()"
                , "public transient [Ljava.lang.String; mergeRemoteIndices([Ljava.lang.String;)"
        );

        Method[] declaredMethods = EtmConfiguration.class.getDeclaredMethods();
        Method[] overriddenMethods = ElasticBackedEtmConfiguration.class.getDeclaredMethods();
        for (Method method : declaredMethods) {
            final String methodSignature = createMethodSignature(method);
            if (methodsToIgnore.contains(methodSignature) || method.getName().startsWith("set")) {
                continue;
            }
            if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
                assertTrue(Arrays.stream(overriddenMethods).map(this::createMethodSignature).anyMatch(p -> p.equals(methodSignature)),
                        "Method '" + methodSignature + "' not overridden in " + ElasticBackedEtmConfiguration.class.getName());
            }
        }
    }

    private String createMethodSignature(Method method) {
        return Modifier.toString(method.getModifiers()) + " " + method.getReturnType().getName() + " " + method.getName() + "(" + Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(", ")) + ")";
    }
}
