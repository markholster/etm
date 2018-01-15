package com.jecstar.etm.launcher;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

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
		Method[] declaredMethods = EtmConfiguration.class.getDeclaredMethods();
		Method[] overriddenMethods = ElasticBackedEtmConfiguration.class.getDeclaredMethods();
		for (Method method : declaredMethods) {
			String methodName = method.getName(); 
			if (methodName.startsWith("get") && !methodName.equals("getNodeName")
					&& !methodName.equals("getComponent")) {
				assertTrue(Arrays.stream(overriddenMethods).anyMatch(p -> p.getName().equals(methodName)),
						"Methode '" + methodName + "' not overriden in " + ElasticBackedEtmConfiguration.class.getName());
			}
		}
	}
}
