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
