package com.jecstar.etm.gui.rest;

public class IIBApi {

	public static boolean IIB_PROXY_ON_CLASSPATH;
	public static boolean IIB_V10_ON_CLASSPATH;
	
	static {
		try {
			Class.forName("com.ibm.broker.config.proxy.BrokerProxy");
			IIB_PROXY_ON_CLASSPATH = true;
		} catch (ClassNotFoundException e) {
			IIB_PROXY_ON_CLASSPATH = false;
		}
		try {
			Class.forName("com.ibm.broker.config.proxy.SubFlowProxy");
			IIB_V10_ON_CLASSPATH = true;
		} catch (ClassNotFoundException e) {
			IIB_V10_ON_CLASSPATH = false;
		}
		
	}
}
