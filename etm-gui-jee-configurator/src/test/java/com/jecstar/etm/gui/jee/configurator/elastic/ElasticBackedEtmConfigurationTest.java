package com.jecstar.etm.gui.jee.configurator.elastic;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.jecstar.etm.core.configuration.EtmConfiguration;

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
				Assert.assertTrue("Methode '" + methodName + "' not overriden in " + ElasticBackedEtmConfiguration.class.getName(),
						Arrays.stream(overriddenMethods).anyMatch(p -> p.getName().equals(methodName)));
			}
		}
	}
}
