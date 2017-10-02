package com.jecstar.etm.processor.jms.configuration;

import java.util.HashMap;
import java.util.Map;

public class JNDIConnectionFactory extends AbstractConnectionFactory {

    public String initialContextFactory;

    public String providerURL;

    public String jndiName;

    public final Map<String, String> parameters = new HashMap<>();
}
