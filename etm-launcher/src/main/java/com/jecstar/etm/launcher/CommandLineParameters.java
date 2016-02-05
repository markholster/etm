package com.jecstar.etm.launcher;

import com.jecstar.etm.launcher.http.BCrypt;

public class CommandLineParameters {

	
	private static final String PARAM_CONFIG_DIRECTORY = "--config-dir=";
	private static final String PARAM_CREATE_PASSWORD = "--create-password=";
	
	private String configDirectory = "config";

	private boolean proceedNormalStartup = true;
	
	public CommandLineParameters(String[] arguments) {
		if (arguments == null || arguments.length == 0) {
			return;
		}
		for (String argument : arguments) {
			if (argument.startsWith(PARAM_CONFIG_DIRECTORY)) {
				this.configDirectory = argument.substring(PARAM_CONFIG_DIRECTORY.length());
			} else if (argument.startsWith(PARAM_CREATE_PASSWORD)) {
				this.proceedNormalStartup = false;
				System.out.println(BCrypt.hashpw(argument.substring(PARAM_CREATE_PASSWORD.length()), BCrypt.gensalt()));
			}
		}
	}
	
	public boolean isProceedNormalStartup() {
		return this.proceedNormalStartup;
	}
	
	public String getConfigDirectory() {
		return this.configDirectory;
	}
	
}
