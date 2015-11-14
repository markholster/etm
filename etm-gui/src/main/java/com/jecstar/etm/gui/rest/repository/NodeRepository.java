package com.jecstar.etm.gui.rest.repository;

import java.util.List;
import java.util.Map;

import com.jecstar.etm.gui.rest.Node;

public interface NodeRepository {

	Map<String, Object> getNodeConfiguration(String nodeName);

	List<Node> getNodes();

	void update(String nodeName, Map<String, Object> values);
}
