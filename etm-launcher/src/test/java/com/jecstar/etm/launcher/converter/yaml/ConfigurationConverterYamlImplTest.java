package com.jecstar.etm.launcher.converter.yaml;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.jecstar.etm.launcher.Configuration;
import com.jecstar.etm.launcher.converter.ConfigurationConverter;

/**
 * Test class for the <code>ConfigurationConverterYamlImpl</code> class.
 * 
 * @author Mark Holster
 */
public class ConfigurationConverterYamlImplTest {

	/**
	 * Test the conversion of a map to a configuration object.
	 */
	@Test
	public void testConvert() {
		ConfigurationConverter<Map<String, Object>> converter = new ConfigurationConverterYamlImpl();
		Map<String, Object> values = new HashMap<String, Object>();
		final String nodeName = "Test case";
		final File sslKeystoreLocation = new File("/tmp/test/keystore");
		final Integer httpPort = 1234;
		final Boolean restEnabled = false;
		
		values.put("node.name", nodeName);
		values.put("ssl.keystore.location", sslKeystoreLocation.getPath());
		values.put("http.port", httpPort);
		values.put("processor.rest.enabled", restEnabled);
		
		Configuration configuration = converter.convert(values);
		assertEquals(nodeName, configuration.instanceName);
		assertEquals(sslKeystoreLocation.getPath(), configuration.sslKeystoreLocation.getPath());
		assertEquals(httpPort.intValue(), configuration.httpPort);
		assertEquals(restEnabled.booleanValue(), configuration.restProcessorEnabled);
	}
}
