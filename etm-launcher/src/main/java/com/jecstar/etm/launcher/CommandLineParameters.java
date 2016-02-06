package com.jecstar.etm.launcher;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlWriter;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.http.BCrypt;

public class CommandLineParameters {

	private static final String PARAM_CONFIG_DIRECTORY = "--config-dir=";
	private static final String PARAM_CREATE_PASSWORD = "--create-passwordhash=";
	private static final String PARAM_DUMP_DEFAULT_CONFIG = "--dump-default-config";
	
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
			} else if (argument.startsWith(PARAM_DUMP_DEFAULT_CONFIG)) {
				this.proceedNormalStartup = false;
				try (Writer writer = new OutputStreamWriter(System.out);){
					YamlWriter yamlWriter = new YamlWriter(writer);
					yamlWriter.getConfig().setBeanProperties(false);
					yamlWriter.getConfig().writeConfig.setWriteDefaultValues(true);
					
					yamlWriter.write(new Configuration());
					yamlWriter.close();
				} catch (YamlException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
