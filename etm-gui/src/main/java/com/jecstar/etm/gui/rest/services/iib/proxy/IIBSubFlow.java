package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.List;

public interface IIBSubFlow {

	String getName();
	List<IIBNode> getNodes();
	String getVersion();
}
