package com.jecstar.etm.processor.ibmmq.startup;

public class Main {
	
	public static void main(String[] args) {
		new Startup().launch(System.getProperty("app.home"), args);
	}
	
}
