package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.List;

public interface IIBLibrary {

	public String getName();
	
	List<IIBMessageFlow> getMessageFlows();
	IIBMessageFlow getMessageFlowByName(String flowName);
	
	List<IIBSubFlow> getSubFlows();
	IIBSubFlow getSubFlowByName(String subFlowName);
	String getVersion();
}
