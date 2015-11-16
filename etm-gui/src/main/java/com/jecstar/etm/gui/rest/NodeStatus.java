package com.jecstar.etm.gui.rest;

public class NodeStatus {

	public String id;
	public String hostname;
	public String address;
	public boolean master;
	public boolean client;
	public boolean data;
	
	// OS
	public int osAvailableProcessors;
	public long osRefreshInterval;
	// OS CPU
	public long osCpuCacheSize;
	public int osCpuCoresPerSocket;
	public int osCpuMhz;
	public String osCpuModel;
	public int osCpuTotalCores;
	public int osCpuTotalSockets;
	public String osCpuVendor;
	// OS Memory
	public long osMemTotal;
	// OS Swap
	public long osSwapTotal;
	
	// JVM
	public long jvmStartTime;
	public long jvmPid;
	public String jvmName;
	public String jvmVendor;
	public String jvmVersion;
	// JVM Memory
	public long jvmMemDirectMax;
	public long jvmMemHeapInit;
	public long jvmMemHeapMax;
	public long jvmMemNonHeapInit;
	public long jvmMemNonHeapMax;

}
