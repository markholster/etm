package com.jecstar.etm.launcher.http;

import java.io.IOException;
import java.net.URL;

import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.Resource;

public class MenuAwareClassPathResourceManager extends ClassPathResourceManager {

	private final String prefix;
	private final ClassLoader classLoader;

	public MenuAwareClassPathResourceManager(ClassLoader classLoader, String prefix) {
		super(classLoader, prefix);
		this.prefix = prefix;
		this.classLoader = classLoader;
	}

	
	@Override
	public Resource getResource(String path) throws IOException {
		if (path.toLowerCase().endsWith(".html")) {
	        String modPath = path;
	        if(modPath.startsWith("/")) {
	            modPath = path.substring(1);
	        }
	        final String realPath = this.prefix + modPath;
	        final URL resource = this.classLoader.getResource(realPath);
	        if(resource == null) {
	            return null;
	        } else {
	            return new MenuAwareURLResource(resource, resource.openConnection(), path);
	        }

		}
		return super.getResource(path);
	}
}
