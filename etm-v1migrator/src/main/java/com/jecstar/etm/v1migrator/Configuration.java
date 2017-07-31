package com.jecstar.etm.v1migrator;

class Configuration {

	public String inputClusterName;
	public String inputHostname;
	public int inputPort;
	public final int bulkSize = 100;
	public String bulkApiLocation;
	public final boolean deleteSource = false;
}
