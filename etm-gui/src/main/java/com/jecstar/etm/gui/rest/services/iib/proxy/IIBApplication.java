package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.List;

public interface IIBApplication {

    String getName();

    List<IIBLibrary> getLibraries();

    IIBLibrary getLibraryByName(String libraryName);

    List<IIBMessageFlow> getMessageFlows();

    IIBMessageFlow getMessageFlowByName(String flowName);

    List<IIBSubFlow> getSubFlows();

    IIBSubFlow getSubFlowByName(String subFlowName);

    String getVersion();
}
