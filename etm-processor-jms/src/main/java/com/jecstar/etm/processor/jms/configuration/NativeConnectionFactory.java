package com.jecstar.etm.processor.jms.configuration;

import java.util.HashMap;
import java.util.Map;

public class NativeConnectionFactory extends AbstractConnectionFactory {

    public String className;

    public final Map<String, String> parameters = new HashMap<>();
}
