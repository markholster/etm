package com.jecstar.etm.gui.rest.repository;

import java.util.Map;

public interface NodeRepository {

	Map<String, Object> getNodeConfiguration(String nodeName);
}
