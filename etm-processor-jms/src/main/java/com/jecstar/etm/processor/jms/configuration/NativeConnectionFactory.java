package com.jecstar.etm.processor.jms.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeConnectionFactory extends AbstractConnectionFactory {

    public String className;

    public final List<Object> constructorParameters = new ArrayList<>();

    public final Map<String, String> parameters = new HashMap<>();
}
