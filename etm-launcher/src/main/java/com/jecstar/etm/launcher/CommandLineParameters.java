package com.jecstar.etm.launcher;

public class CommandLineParameters {

	private static final String PARAM_CONFIG_DIRECTORY = "--config-dir";
	
	private String configDirectory = "config";
	
	public CommandLineParameters(String[] arguments) {
		if (arguments == null || arguments.length == 0) {
			return;
		}
		for (String argument : arguments) {
			if (argument.startsWith(PARAM_CONFIG_DIRECTORY + "=")) {
				this.configDirectory = argument.substring(PARAM_CONFIG_DIRECTORY.length() + 1);
			}
		}
	}
	
	public String getConfigDirectory() {
		return this.configDirectory;
	}
}
