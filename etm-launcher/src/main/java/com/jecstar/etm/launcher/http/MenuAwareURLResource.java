package com.jecstar.etm.launcher.http;

import com.jecstar.etm.gui.rest.IIBApi;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.util.StringUtils;
import io.undertow.UndertowLogger;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.util.StatusCodes;
import org.xnio.IoUtils;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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


	MenuAwareURLResource(EtmConfiguration etmConfiguration, String pathPrefixToContextRoot, MenuContext menuContext, URL url, String path) {
		super(url, path);
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
    
	
	
    private void serveImpl(final EtmAccount etmAccount, final Sender sender, final HttpServerExchange exchange, final long start, final long end, final boolean range, final IoCallback completionCallback) {

        class ServerTask implements Runnable, IoCallback {

            private InputStream inputStream;
            private byte[] buffer;

            private long toSkip = start;
            private final long remaining = end - start + 1;

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
                final String divider = "<li><div class=\"dropdown-divider\"></div></li>";
            	StringBuilder html = new StringBuilder();
            	EtmPrincipal principal = etmAccount.getPrincipal();
        		if (principal.isInAnyRole(SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WRITE)) {
        			if (MenuContext.SEARCH.equals(menuContext)) {
        				html.append("<li class=\"nav-item active\">");
        			} else {
        				html.append("<li class=\"nav-item\">");
        			}
        			html.append("<a class=\"nav-link\" href=\"").append(pathPrefixToContextRoot).append("search/\"><span class=\"fa fa-search fa-lg hidden-md-down\">&nbsp;</span>Search</a>");
        			html.append("</li>");
                }

                boolean hasDashboardsToShow = true;
                List<EtmGroup> groups = principal.getGroups().stream().sorted(Comparator.comparing(EtmGroup::getName)).collect(Collectors.toList());
                if (principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ) && !principal.isInRole(SecurityRoles.USER_DASHBOARD_READ_WRITE) && !principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)) {
                    // User has only read only access to group dashboards. Check if they are present, otherwise we can skip this menu option.
                    hasDashboardsToShow = groups.stream().anyMatch(g -> g.getDashboards().size() > 0);
                }
                if (principal.isInAnyRole(SecurityRoles.USER_DASHBOARD_READ_WRITE, SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE) && hasDashboardsToShow) {
                    boolean hasGroupMenu = false;
                    if (MenuContext.DASHBOARD.equals(menuContext)) {
                        html.append("<li class=\"nav-item active dropdown\">");
                    } else {
                        html.append("<li class=\"nav-item dropdown\">");
                    }
                    html.append("<a class=\"nav-link dropdown-toggle\" data-toggle=\"dropdown\" role=\"button\" aria-haspopup=\"true\" aria-expanded=\"false\" href=\"#\"><span class=\"fa fa-dashboard fa-lg hidden-md-down\">&nbsp;</span>Visualizations</a>");
                    html.append("<ul class=\"dropdown-menu\">");
                    if (principal.isInAnyRole(SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE)) {
                        // First display the group names
                        for (EtmGroup group : groups) {
                            if (group.getDashboards().size() == 0 && principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ)) {
                                // Skip a group when it has no dashboards and the user has read only access.
                                continue;
                            }
                            html.append("<li><a class=\"dropdown-item dropdown-toggle\" href=\"#\"><span class=\"fa fa-users hidden-md-down\">&nbsp;</span>" + StringUtils.escapeToHtml(group.getName())+ "</a>");
                            html.append("<ul class=\"dropdown-menu\">");
                            for (String dashboard : group.getDashboards()) {
                                appendMenuOption(html, dashboard, "dashboard/dashboards.html?name=" + StringUtils.urlEncode(dashboard) + "&group=" + StringUtils.urlEncode(group.getName()), !principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ_WRITE));
                            }
                            if (principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)) {
                                if (group.getDashboards().size() > 0) {
                                    html.append(divider);
                                }
                                appendMenuOption(html, "Graphs", "dashboard/graphs.html?group=" + StringUtils.urlEncode(group.getName()), false);
                                appendMenuOption(html, "New dashboard", "dashboard/dashboards.html?action=new&group=" + StringUtils.urlEncode(group.getName()), false);
                            }
                            html.append("</ul></li>");
                            hasGroupMenu = true;
                        }
                    }
                    if (principal.isInRole(SecurityRoles.USER_DASHBOARD_READ_WRITE)) {
                        if (hasGroupMenu) {
                            html.append(divider);
                            // Only add a submenu when there are group menus displayed.
                            html.append("<li><a class=\"dropdown-item dropdown-toggle\" href=\"#\">" + StringUtils.escapeToHtml(principal.getName())+ "</a>");
                            html.append("<ul class=\"dropdown-menu\">");
                        }
                        for (String dashboard : principal.getDashboards()) {
                            appendMenuOption(html, dashboard, "dashboard/dashboards.html?name=" + StringUtils.urlEncode(dashboard), false);
                        }
                        if (principal.getDashboards().size() > 0) {
                            html.append(divider);
                        }
                        appendMenuOption(html, "Graphs", "dashboard/graphs.html", false);
                        appendMenuOption(html, "New dashboard", "dashboard/dashboards.html?action=new", false);
                        if (hasGroupMenu) {
                            html.append("</ul>");
                        }
                        html.append("</li>");
                    }
                    html.append("</ul></li>");



//                    if (principal.isInRole(SecurityRoles.USER_DASHBOARD_READ_WRITE)) {
//                        appendMenuOption(html, "Graphs", "dashboard/graphs.html", false);
//                    }
//                    html.append("<li><a class=\"dropdown-item dropdown-toggle\" href=\"#\">Dashboards</a>");
//                    html.append("<ul class=\"dropdown-menu\">");
//
//                    // TODO als group read & write -> Altijd optie tot nieuw dashboard in group
//                    // als group read & group heeft geen dashboards -> group overslaan
//                    // Als user read_write dan zelfde menu structuur, maar met username i.p.v. groupname.
//
//
//                    List<EtmGroup> groups = principal.getGroups().stream().sorted(Comparator.comparing(EtmGroup::getName)).collect(Collectors.toList());
//                    for (EtmGroup group : groups) {
//                        if (group.getDashboards().size() != 0 || principal.isInAnyRole(SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE)) {
//                            html.append("<li><a class=\"dropdown-item dropdown-toggle\" href=\"#\"><span class=\"fa fa-users hidden-md-down\">&nbsp;</span>" + StringUtils.escapeToHtml(group.getName())+ "</a>");
//                            html.append("<ul class=\"dropdown-menu\">");
//                            for (String dashboard : group.getDashboards()) {
//                                appendMenuOption(html, dashboard, "dashboard/dashboards.html?name=" + StringUtils.urlEncode(dashboard) + "&group=" + StringUtils.urlEncode(group.getName()), !principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ_WRITE));
//                            }
//                            if (principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)) {
//                                if (group.getDashboards().size() != 0) {
//                                    html.append(divider);
//                                }
//                                appendMenuOption(html, "New dashboard", "dashboard/dashboards.html?action=new&group=" + StringUtils.urlEncode(group.getName()), false);
//                            }
//                            html.append("</ul></li>");
//                        }
//                    }
//                    if (principal.getDashboards().size() != 0) {
//                        for (String dashboard : principal.getDashboards()) {
//                            appendMenuOption(html, dashboard, "dashboard/dashboards.html?name=" + StringUtils.urlEncode(dashboard), !principal.isInRole(SecurityRoles.DASHBOARD_READ_WRITE));
//                        }
//                        if (principal.isInRole(SecurityRoles.DASHBOARD_READ_WRITE)) {
//                            html.append(divider);
//                            appendMenuOption(html, "New dashboard", "dashboard/dashboards.html?action=new", false);
//                        }
//                    }
//                    html.append("</ul></li>");
//                    html.append("</ul></li>");
                }
                if (principal.isInAnyRole(SecurityRoles.ALL_ROLES_ARRAY)) {
        			if (MenuContext.PREFERENCES.equals(menuContext)) {
        				html.append("<li class=\"nav-item active\">");
        			} else {
        				html.append("<li class=\"nav-item\">");
        			}
        			html.append("<a class=\"nav-link\" href=\"").append(pathPrefixToContextRoot).append("preferences/\"><span class=\"fa fa-user fa-lg hidden-md-down\">&nbsp;</span>Preferences</a>");
        			html.append("</li>");
        		}
        		if (principal.isInAnyRole(
        				SecurityRoles.IIB_NODE_READ,
						SecurityRoles.IIB_NODE_READ_WRITE,
                        SecurityRoles.IIB_EVENT_READ,
                        SecurityRoles.IIB_EVENT_READ_WRITE,
                        SecurityRoles.AUDIT_LOG_READ,
                        SecurityRoles.CLUSTER_SETTINGS_READ,
                        SecurityRoles.CLUSTER_SETTINGS_READ_WRITE,
                        SecurityRoles.ENDPOINT_SETTINGS_READ,
                        SecurityRoles.ENDPOINT_SETTINGS_READ_WRITE,
                        SecurityRoles.GROUP_SETTINGS_READ,
                        SecurityRoles.GROUP_SETTINGS_READ_WRITE,
                        SecurityRoles.INDEX_STATISTICS_READ,
                        SecurityRoles.LICENSE_READ,
                        SecurityRoles.LICENSE_READ_WRITE,
                        SecurityRoles.NODE_SETTINGS_READ,
                        SecurityRoles.NODE_SETTINGS_READ_WRITE,
                        SecurityRoles.PARSER_SETTINGS_READ,
                        SecurityRoles.PARSER_SETTINGS_READ_WRITE,
                        SecurityRoles.USER_SETTINGS_READ,
                        SecurityRoles.USER_SETTINGS_READ_WRITE
				)) {
                    if (MenuContext.SETTINGS.equals(menuContext)) {
                        html.append("<li class=\"nav-item active dropdown\">");
                    } else {
                        html.append("<li class=\"nav-item dropdown\">");
                    }
                    html.append("<a class=\"nav-link dropdown-toggle\" data-toggle=\"dropdown\" role=\"button\" aria-haspopup=\"true\" aria-expanded=\"false\" href=\"#\"><span class=\"fa fa-wrench fa-lg hidden-md-down\">&nbsp;</span>Settings</a>");
                    html.append("<ul class=\"dropdown-menu\">");
        			boolean addedBeforeDivider = false;
        			if (principal.isInAnyRole(SecurityRoles.USER_SETTINGS_READ, SecurityRoles.USER_SETTINGS_READ_WRITE)) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "Users", "settings/users.html", !principal.isInRole(SecurityRoles.USER_SETTINGS_READ_WRITE));
                    }
                    if (principal.isInAnyRole(SecurityRoles.GROUP_SETTINGS_READ, SecurityRoles.GROUP_SETTINGS_READ_WRITE)) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "Groups", "settings/groups.html", !principal.isInRole(SecurityRoles.GROUP_SETTINGS_READ_WRITE));
                    }

                    // DIVIDER
                    if (addedBeforeDivider) {
                        addedBeforeDivider = false;
                        html.append(divider);
                    }
                    if (principal.isInAnyRole(SecurityRoles.CLUSTER_SETTINGS_READ, SecurityRoles.CLUSTER_SETTINGS_READ_WRITE)) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "Cluster", "settings/cluster.html", !principal.isInRole(SecurityRoles.CLUSTER_SETTINGS_READ_WRITE));
                    }
                    if (principal.isInAnyRole(SecurityRoles.NODE_SETTINGS_READ, SecurityRoles.NODE_SETTINGS_READ_WRITE)) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "Nodes", "settings/nodes.html", !principal.isInRole(SecurityRoles.NODE_SETTINGS_READ_WRITE));
                    }
                    if (principal.isInAnyRole(SecurityRoles.PARSER_SETTINGS_READ, SecurityRoles.PARSER_SETTINGS_READ_WRITE)) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "Parsers", "settings/parsers.html", !principal.isInRole(SecurityRoles.PARSER_SETTINGS_READ_WRITE));
                    }
                    if (principal.isInAnyRole(SecurityRoles.ENDPOINT_SETTINGS_READ, SecurityRoles.ENDPOINT_SETTINGS_READ_WRITE)) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "Endpoints", "settings/endpoints.html", !principal.isInRole(SecurityRoles.ENDPOINT_SETTINGS_READ_WRITE));
                    }

                    // DIVIDER
                    if (addedBeforeDivider) {
                        addedBeforeDivider = false;
                        html.append(divider);
                    }
                    if (principal.isInAnyRole(SecurityRoles.AUDIT_LOG_READ)) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "Audit logs", "settings/auditlogs.html", false);
                    }
                    if (principal.isInAnyRole(SecurityRoles.INDEX_STATISTICS_READ)) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "Index statistics", "settings/indexstats.html", false);
                    }

                    // DIVIDER
                    if (addedBeforeDivider) {
                        addedBeforeDivider = false;
                        html.append(divider);
                    }
                    if (principal.isInAnyRole(SecurityRoles.IIB_NODE_READ, SecurityRoles.IIB_NODE_READ_WRITE) && IIBApi.IIB_PROXY_ON_CLASSPATH) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "IIB nodes", "iib/nodes.html", !principal.isInRole(SecurityRoles.IIB_NODE_READ_WRITE));
                    }
                    if (principal.isInAnyRole(SecurityRoles.IIB_EVENT_READ, SecurityRoles.IIB_EVENT_READ_WRITE) && IIBApi.IIB_PROXY_ON_CLASSPATH) {
                        addedBeforeDivider = true;
                        appendMenuOption(html, "IIB events", "iib/events.html", !principal.isInRole(SecurityRoles.IIB_EVENT_READ_WRITE));
                    }

                    // DIVIDER
                    if (addedBeforeDivider) {
                        addedBeforeDivider = false;
                        html.append(divider);
                    }
        			if (principal.isInAnyRole(SecurityRoles.LICENSE_READ, SecurityRoles.LICENSE_READ_WRITE)) {
        			    html.append("<li>");
        			    String page = "settings/license.html";
        			    if (!principal.isInRole(SecurityRoles.LICENSE_READ_WRITE)) {
                            page += "?readonly=true";
                        }
        				if (MenuAwareURLResource.this.etmConfiguration.isLicenseExpired()) {
        					html.append("<a class=\"dropdown-item alert-danger\" href=\"").append(pathPrefixToContextRoot).append(page + "\">");
        					html.append("<span class=\"fa fa-ban\">&nbsp;</span>");
        				} else if (MenuAwareURLResource.this.etmConfiguration.isLicenseAlmostExpired()) {
        					html.append("<a class=\"dropdown-item alert-warning\" href=\"").append(pathPrefixToContextRoot).append(page + "\">");
        					html.append("<span class=\"fa fa-exclamation-triangle\">&nbsp;</span>");
        				} else {
        					html.append("<a class=\"dropdown-item\" href=\"").append(pathPrefixToContextRoot).append(page + "\">");
        				}
        				html.append("License</a></li>");
        			}

        			// Remove last divider if no content after it.
                    if (html.lastIndexOf(divider) == html.length() - divider.length()) {
        			    html.delete(html.lastIndexOf(divider), html.length());
                    }
        			html.append("</ul></li>");
        		}
    			html.append("<li class=\"nav-item\"><a class=\"nav-link\" href=\"").append(pathPrefixToContextRoot).append("logout?source=./\"><span class=\"fa fa-sign-out fa-lg hidden-md-down\">&nbsp;</span>Sign out</a></li>");
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

    private void appendMenuOption(StringBuilder html, String name, String page, boolean readOnly) {
	    String url = page;
	    if (readOnly) {
            url += (page.contains("?") ? "&" : "?") +"readonly=true";
        }
        html.append("<li><a class=\"dropdown-item\" href=\"")
            .append(this.pathPrefixToContextRoot)
            .append(url + "\">" + name + "</a></li>");
    }
}
