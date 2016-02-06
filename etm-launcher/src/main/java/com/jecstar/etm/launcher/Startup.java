package com.jecstar.etm.launcher;

import java.io.File;

import org.apache.log4j.PropertyConfigurator;

public class Startup {


	public static void main(String[] args) {
		CommandLineParameters commandLineParameters = new CommandLineParameters(args);
		if (!commandLineParameters.isProceedNormalStartup()) {
			return;
		}
		PropertyConfigurator.configure(new File(commandLineParameters.getConfigDirectory(), "log4j.properties").getPath());
		new Launcher().launch(commandLineParameters);
	}		
}
