package com.jecstar.etm.gui.rest;

public class IIBApi {

	public static boolean IIB_PROXY_AVAILABLE;
	public static boolean IIB_SUBFLOW_PROXY_AVAILABLE;
	public static boolean IIB_STATIC_LIBRARY_PROXY_AVAILABLE;
	
	static {
		try {
			Class.forName("com.ibm.broker.config.proxy.BrokerProxy");
			IIB_PROXY_AVAILABLE = true;
		} catch (ClassNotFoundException e) {
			IIB_PROXY_AVAILABLE = false;
		}
		try {
			Class.forName("com.ibm.broker.config.proxy.SubFlowProxy");
			IIB_SUBFLOW_PROXY_AVAILABLE = true;
		} catch (ClassNotFoundException e) {
			IIB_SUBFLOW_PROXY_AVAILABLE = false;
		}
		
		try {
			Class.forName("com.ibm.broker.config.proxy.StaticLibraryProxy");
			IIB_STATIC_LIBRARY_PROXY_AVAILABLE = true;
		} catch (ClassNotFoundException e) {
			IIB_STATIC_LIBRARY_PROXY_AVAILABLE = false;
		}

		
		
	}
}
