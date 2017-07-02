package com.jecstar.etm.launcher.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.xnio.IoUtils;

import com.jecstar.etm.gui.rest.IIBApi;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.EtmPrincipalRole;

import io.undertow.UndertowLogger;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.util.StatusCodes;

/**
 * <code>URLResource</code> extension that injects the ETM main menu based on the roles the user is assigned to.
 * 
 * @author Mark Holster
 */
public class MenuAwareURLResource extends URLResource {
	
	public enum MenuContext {SEARCH, DASHBOARD, PREFERENCES, SETTINGS} 
	
	private static final String MENU_PLACEHOLDER = "<li id=\"placeholder-for-MenuAwareURLResource\"></li>";
	
	private final EtmConfiguration etmConfiguration;
	private final String pathPrefixToContextRoot;
	private final MenuContext menuContext;
	private final URL url;


	public MenuAwareURLResource(EtmConfiguration etmConfiguration, String pathPrefixToContextRoot, MenuContext menuContext, URL url, URLConnection connection, String path) {
		super(url, connection, path);
		this.etmConfiguration = etmConfiguration;
		this.pathPrefixToContextRoot = pathPrefixToContextRoot;
		this.menuContext = menuContext;
		this.url = url;
	}
	
	@Override
	public void serve(Sender sender, HttpServerExchange exchange, IoCallback completionCallback) {
		if (exchange.getSecurityContext() == null || exchange.getSecurityContext().getAuthenticatedAccount() == null) {
			super.serve(sender, exchange, completionCallback);
		} else {
			serveImpl((EtmAccount) exchange.getSecurityContext().getAuthenticatedAccount(), sender, exchange, -1, -1, false, completionCallback);
		}
	}
	
    @Override
    public void serveRange(Sender sender, HttpServerExchange exchange, long start, long end, IoCallback completionCallback) {
		if (exchange.getSecurityContext() == null || exchange.getSecurityContext().getAuthenticatedAccount() == null) {
			super.serveRange(sender, exchange, start, end, completionCallback);
		} else {
			serveImpl((EtmAccount) exchange.getSecurityContext().getAuthenticatedAccount(), sender, exchange, start, end, true, completionCallback);
		}
    }
    
    @Override
    public Long getContentLength() {
    	// The content length is unknown at this point, because the underlying html file may be changed based on the roles of the requesting user. 
    	return null;
    }
    
    @Override
    public File getFile() {
    	// The File is unknown at this point, because the underlying html file may be changed based on the roles of the requesting user. 
    	return null;
    }
    
    @Override
    public Path getFilePath() {
    	// The Path is unknown at this point, because the underlying html file may be changed based on the roles of the requesting user. 
    	return null;
    }
    
	
	
    public void serveImpl(final EtmAccount etmAccount, final Sender sender, final HttpServerExchange exchange, final long start, final long end, final boolean range, final IoCallback completionCallback) {

        class ServerTask implements Runnable, IoCallback {

            private InputStream inputStream;
            private byte[] buffer;

            long toSkip = start;
            long remaining = end - start + 1;

            @Override
            public void run() {
                if (range && remaining == 0) {
                    //we are done, just return
                    IoUtils.safeClose(inputStream);
                    completionCallback.onComplete(exchange, sender);
                    return;
                }
                
                if (inputStream == null) {
                    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                        String htmlContent =  buffer.lines().collect(Collectors.joining("\n"));
                        int ix = htmlContent.indexOf(MENU_PLACEHOLDER);
                        if (ix == -1) {
                        	inputStream = new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8));
                        } else {
                        	inputStream = new ByteArrayInputStream(htmlContent.replace(MENU_PLACEHOLDER, buildHtmlMenu(etmAccount)).getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (IOException e1) {
                        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                        return;
					}
                    buffer = new byte[1024];//TODO: we should be pooling these
                }
                try {
                    int res = inputStream.read(buffer);
                    if (res == -1) {
                        //we are done, just return
                        IoUtils.safeClose(inputStream);
                        completionCallback.onComplete(exchange, sender);
                        return;
                    }
                    int bufferStart = 0;
                    int length = res;
                    if (range && toSkip > 0) {
                        //skip to the start of the requested range
                        //not super efficient, but what can you do
                        while (toSkip > res) {
                            toSkip -= res;
                            res = inputStream.read(buffer);
                            if (res == -1) {
                                //we are done, just return
                                IoUtils.safeClose(inputStream);
                                completionCallback.onComplete(exchange, sender);
                                return;
                            }
                        }
                        bufferStart = (int) toSkip;
                        length -= toSkip;
                        toSkip = 0;
                    }
                    if (range && length > remaining) {
                        length = (int) remaining;
                    }
                    sender.send(ByteBuffer.wrap(buffer, bufferStart, length), this);
                } catch (IOException e) {
                    onException(exchange, sender, e);
                }
            }
            
            
			@Override
            public void onComplete(final HttpServerExchange exchange, final Sender sender) {
                if (exchange.isInIoThread()) {
                    exchange.dispatch(this);
                } else {
                    run();
                }
            }

            @Override
            public void onException(final HttpServerExchange exchange, final Sender sender, final IOException exception) {
                UndertowLogger.REQUEST_IO_LOGGER.ioException(exception);
                IoUtils.safeClose(inputStream);
                if (!exchange.isResponseStarted()) {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                }
                completionCallback.onException(exchange, sender, exception);
            }

            private CharSequence buildHtmlMenu(EtmAccount etmAccount) {
            	StringBuilder html = new StringBuilder();
            	EtmPrincipal principal = etmAccount.getPrincipal();
        		if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.SEARCHER)) {
        			if (MenuContext.SEARCH.equals(menuContext)) {
        				html.append("<li class=\"nav-item active\">");
        			} else {
        				html.append("<li class=\"nav-item\">");
        			}
        			html.append("<a class=\"nav-link\" href=\"" + pathPrefixToContextRoot + "search/\"><span class=\"fa fa-search fa-lg hidden-md-down\">&nbsp;</span>Search</a>");
        			html.append("</li>");
        		}
        		if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.CONTROLLER)) {
        			if (MenuContext.DASHBOARD.equals(menuContext)) {
        				html.append("<li class=\"nav-item active dropdown\">");
        			} else {
        				html.append("<li class=\"nav-item dropdown\">");
        			}
        			html.append("<a class=\"nav-link dropdown-toggle\" data-toggle=\"dropdown\" role=\"button\" aria-haspopup=\"true\" aria-expanded=\"false\" href=\"#\"><span class=\"fa fa-dashboard fa-lg hidden-md-down\">&nbsp;</span>Visualizations</a>");
        			html.append("<div class=\"dropdown-menu\">");
        			html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "dashboard/graphs.html\">Graphs</a>");
        			html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "dashboard/dashboards.html\">Dashboards</a>");
        			html.append("</div></li>");
        		}
        		if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.CONTROLLER, EtmPrincipalRole.SEARCHER, EtmPrincipalRole.IIB_ADMIN)) {
        			if (MenuContext.PREFERENCES.equals(menuContext)) {
        				html.append("<li class=\"nav-item active\">");
        			} else {
        				html.append("<li class=\"nav-item\">");
        			}
        			html.append("<a class=\"nav-link\" href=\"" + pathPrefixToContextRoot + "preferences/\"><span class=\"fa fa-user fa-lg hidden-md-down\">&nbsp;</span>Preferences</a>");
        			html.append("</li>");
        		}
        		if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.IIB_ADMIN)) {
        			if (MenuContext.SETTINGS.equals(menuContext)) {
        				html.append("<li class=\"nav-item active dropdown\">");
        			} else {
        				html.append("<li class=\"nav-item dropdown\">");
        			}
        			html.append("<a class=\"nav-link dropdown-toggle\" data-toggle=\"dropdown\" role=\"button\" aria-haspopup=\"true\" aria-expanded=\"false\" href=\"#\"><span class=\"fa fa-wrench fa-lg hidden-md-down\">&nbsp;</span>Settings</a>");
        			html.append("<div class=\"dropdown-menu\">");
        			boolean added = false;
        			if (principal.isInRole(EtmPrincipalRole.ADMIN)) {
        				added = true;
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "settings/users.html\">Users</a>");
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "settings/groups.html\">Groups</a>");
        				
        				html.append("<div class=\"dropdown-divider\"></div>");
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "settings/cluster.html\">Cluster</a>");
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "settings/nodes.html\">Nodes</a>");
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "settings/parsers.html\">Parsers</a>");
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "settings/endpoints.html\">Endpoints</a>");
        				
        				html.append("<div class=\"dropdown-divider\"></div>");
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "settings/auditlogs.html\">Audit logs</a>");
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "settings/indexstats.html\">Index statistics</a>");
        			}
        			if (principal.isInAnyRole(EtmPrincipalRole.ADMIN, EtmPrincipalRole.IIB_ADMIN) && IIBApi.IIB_PROXY_ON_CLASSPATH) {
        				if (added) {
        					html.append("<div class=\"dropdown-divider\"></div>");
        				}
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "iib/nodes.html\">IIB Nodes</a>");
        				html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "iib/events.html\">IIB Events</a>");
        			}
        			if (principal.isInRole(EtmPrincipalRole.ADMIN)) {
        				html.append("<div class=\"dropdown-divider\"></div>");
        				if (MenuAwareURLResource.this.etmConfiguration.isLicenseExpired()) {
        					html.append("<a class=\"dropdown-item alert-danger\" href=\"" + pathPrefixToContextRoot + "settings/license.html\">");
        					html.append("<span class=\"fa fa-ban\">&nbsp;</span>");
        				} else if (MenuAwareURLResource.this.etmConfiguration.isLicenseAlmostExpired()) {
        					html.append("<a class=\"dropdown-item alert-warning\" href=\"" + pathPrefixToContextRoot + "settings/license.html\">");
        					html.append("<span class=\"fa fa-exclamation-triangle\">&nbsp;</span>");
        				} else {
        					html.append("<a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "settings/license.html\">");
        				}
        				html.append("License</a>");
        			}
        			html.append("</div></li>");
        		}
    			html.append("<li class=\"nav-item\"><a class=\"nav-link\" href=\"" + pathPrefixToContextRoot + "logout?source=./\"><span class=\"fa fa-sign-out fa-lg hidden-md-down\">&nbsp;</span>Sign out</a></li>");
				return html.toString();
			}
        }

        ServerTask serveTask = new ServerTask();
        if (exchange.isInIoThread()) {
            exchange.dispatch(serveTask);
        } else {
            serveTask.run();
        }
    }


}
