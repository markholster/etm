/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.launcher.http;

import com.jecstar.etm.gui.rest.IIBApi;
import com.jecstar.etm.server.core.Etm;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <code>URLResource</code> extension that injects the ETM main menu based on the roles the user is assigned to.
 *
 * @author Mark Holster
 */
public class MenuAwareURLResource extends URLResource {

    public enum MenuContext {
        SEARCH("/search/"),
        DASHBOARD("/dashboard/"),
        PREFERENCES("/preferences/"),
        SETTINGS("/settings/", "/iib/"),
        SIGNAL("/signal/");


        private final String[] pathPrefixes;

        MenuContext(String... pathPrefixes) {
            this.pathPrefixes = pathPrefixes;
        }

        boolean isInContext(String path) {
            final String lowercasePath = path.toLowerCase();
            for (String pathPrefix: this.pathPrefixes) {
                if (lowercasePath.startsWith(pathPrefix)) {
                    return true;
                }
            }
            return false;
        }

        public static MenuContext parseFromPath(String path) {
            for (MenuContext menuContext : values()) {
                if (menuContext.isInContext(path)) {
                    return menuContext;
                }
            }
            return null;
        }

    }

    private static final String MENU_PLACEHOLDER = "<li id=\"placeholder-for-MenuAwareURLResource\"></li>";
    private static final String USER_PLACEHOLDER = "<div class=\"dropdown-user-scroll scrollbar-outer\"></div>";
    private static final String SEARCH_HEADER_PLACEHOLDER = "<div id=\"search-nav\"></div>";
    private static final String NAVBAR_COLOR_PLACEHOLDER = "etm-navbar-color";
    private static final String SIDEBAR_COLOR_PLACEHOLDER = "etm-sidebar-color";

    private final EtmConfiguration etmConfiguration;
    private final String pathPrefixToContextRoot;
    private final MenuContext menuContext;
    private final URL url;
    private final String divider = "<hr style=\"margin-top: 0px; margin-bottom: 0px;\"/>";


    MenuAwareURLResource(EtmConfiguration etmConfiguration, String pathPrefixToContextRoot, URL url, String path) {
        super(url, path);
        this.etmConfiguration = etmConfiguration;
        this.pathPrefixToContextRoot = pathPrefixToContextRoot;
        this.menuContext = MenuContext.parseFromPath(path);
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
                        String htmlContent = buffer.lines().collect(Collectors.joining("\n"));
                        if (Etm.hasVersion()) {
                            // Appversion is only set when started from the start script. We add the version to the scripts path to make sure the browser cache isn't
                            // preventing the new functionality from being loaded.
                            htmlContent = htmlContent.replaceAll("/scripts/", "/scripts/" + Etm.getVersion() + "/");
                        }
                        int ix = htmlContent.indexOf(NAVBAR_COLOR_PLACEHOLDER);
                        if (ix != -1) {
                            htmlContent = htmlContent.replaceAll(NAVBAR_COLOR_PLACEHOLDER, "dark");
                        }
                        ix = htmlContent.indexOf(SIDEBAR_COLOR_PLACEHOLDER);
                        if (ix != -1) {
                            htmlContent = htmlContent.replaceAll(SIDEBAR_COLOR_PLACEHOLDER, "dark2");
                        }
                        ix = htmlContent.indexOf(USER_PLACEHOLDER);
                        if (ix != -1) {
                            htmlContent = htmlContent.replace(USER_PLACEHOLDER, "<div class=\"dropdown-user-scroll scrollbar-outer\">\n" +
                                    "                                <li>\n" +
                                    "                                    <div class=\"user-box\">\n" +
                                    "                                        <div class=\"u-text\">\n" +
                                    "                                            <h4>" + StringUtils.escapeToHtml(etmAccount.getPrincipal().getDisplayName()) + "</h4>\n" +
                                    "                                            <p class=\"text-muted\">" + StringUtils.escapeToHtml(etmAccount.getPrincipal().getEmailAddress()) + "</p><a href=\"" + pathPrefixToContextRoot + "preferences/\" class=\"btn btn-xs btn-secondary btn-sm\">View Profile</a>\n" +
                                    "                                        </div>\n" +
                                    "                                    </div>\n" +
                                    "                                </li>\n" +
                                    "                                <li>\n" +
                                    "                                    <div class=\"dropdown-divider\"></div>\n" +
                                    "                                    <a class=\"dropdown-item\" href=\"" + pathPrefixToContextRoot + "logout?source=./\">Signout</a>\n" +
                                    "                                </li>\n" +
                                    "                            </div>");
                        }
                        ix = htmlContent.indexOf(SEARCH_HEADER_PLACEHOLDER);
                        if (ix != -1 && etmAccount.getPrincipal().isInAnyRole(SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WRITE)) {
                            htmlContent = htmlContent.replace(SEARCH_HEADER_PLACEHOLDER, "                <div class=\"collapse\" id=\"search-nav\">\n" +
                                    "                    <form class=\"navbar-left navbar-form nav-search mr-md-3\" action=\"" + pathPrefixToContextRoot + "search/index.html\">\n" +
                                    "                        <div class=\"input-group\">\n" +
                                    "                            <div class=\"input-group-prepend\">\n" +
                                    "                                <button type=\"submit\" class=\"btn btn-search pr-1\">\n" +
                                    "                                    <i class=\"fa fa-search search-icon\"></i>\n" +
                                    "                                </button>\n" +
                                    "                            </div>\n" +
                                    "                            <input type=\"text\" name=\"q\" placeholder=\"Quick search ...\" class=\"form-control\" />\n" +
                                    "                        </div>\n" +
                                    "                    </form>\n" +
                                    "                </div>\n");
                        }
                        ix = htmlContent.indexOf(MENU_PLACEHOLDER);
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
                addSearchMenuOption(principal, html);
                addDashboardMenuOption(principal, html);
                addSignalMenuOption(principal, html);
                addPreferencesMenuOption(principal, html);
                addSettingsMenuOption(principal, html);
                // The signout menu option
                html.append("<hr /><li class=\"nav-item submenu\"><a href=\"").append(pathPrefixToContextRoot).append("logout?source=./\"><i class=\"fa fa-sign-out-alt\"></i><p>Sign out</p></a></li>");
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
        appendMenuOption(html, name, page, null, readOnly);
    }

    private void appendMenuOption(StringBuilder html, String name, String page, String iconClass, boolean readOnly) {
        String url = page;
        if (readOnly) {
            url += (page.contains("?") ? "&" : "?") + "readonly=true";
        }
        html.append("<li class=\"" + (this.url.getPath().endsWith(url) ? "active" : "") + "\"><a href=\"")
                .append(this.pathPrefixToContextRoot)
                .append(url + "\">");
        if (iconClass != null) {
            html.append("<i class=\"fa " + iconClass + "\"></i>");
        }
        html.append("<p>" + StringUtils.escapeToHtml(name) + "</p></a></li>");
    }

    /**
     * Add the search menu option
     *
     * @param principal The <code>EtmPrincipal</code>.
     * @param html      The html buffer.
     */
    private void addSearchMenuOption(EtmPrincipal principal, StringBuilder html) {
        if (!principal.isInAnyRole(SecurityRoles.ETM_EVENT_READ, SecurityRoles.ETM_EVENT_READ_WRITE)) {
            return;
        }
        if (MenuContext.SEARCH.equals(menuContext)) {
            html.append("<li class=\"nav-item active\">");
        } else {
            html.append("<li class=\"nav-item submenu\">");
        }
        html.append("<a href=\"").append(pathPrefixToContextRoot).append("search/\"><i class=\"fa fa-search\"></i><p>Search</p></a>");
        html.append("</li>");
    }

    /**
     * Add the dashboard menu option
     *
     * @param principal The <code>EtmPrincipal</code>.
     * @param html      The html buffer.
     */
    private void addDashboardMenuOption(EtmPrincipal principal, StringBuilder html) {
        boolean hasDashboardsToShow = true;
        List<EtmGroup> groups = principal.getGroups().stream().sorted(Comparator.comparing(EtmGroup::getMostSpecificName)).collect(Collectors.toList());
        if (principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ) && !principal.isInRole(SecurityRoles.USER_DASHBOARD_READ_WRITE) && !principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)) {
            // User has only read only access to group dashboards. Check if they are present, otherwise we can skip this menu option.
            hasDashboardsToShow = groups.stream().anyMatch(g -> g.getDashboards().size() > 0);
        }
        if (principal.isInAnyRole(SecurityRoles.USER_DASHBOARD_READ_WRITE, SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE) && hasDashboardsToShow) {
            boolean hasGroupMenu = false;
            if (MenuContext.DASHBOARD.equals(menuContext)) {
                html.append("<li class=\"nav-item active submenu\">");
            } else {
                html.append("<li class=\"nav-item\">");
            }
            html.append("<a class=\"" + (MenuContext.DASHBOARD.equals(menuContext) ? "" : "collapsed") + "\" data-toggle=\"collapse\" href=\"#sub_visualizations\" aria-expanded=\"" + (MenuContext.DASHBOARD.equals(menuContext) ? "true" : "false") + "\"><i class=\"fa fa-tachometer-alt\"></i><p>Visualizations</p><span class=\"caret\"></span></a>");
            html.append("<div id=\"sub_visualizations\" class=\"submenu " + (MenuContext.DASHBOARD.equals(menuContext) ? "collapse show" : "collapse") + "\"><ul class=\"nav nav-collapse\">");
            if (principal.isInAnyRole(SecurityRoles.GROUP_DASHBOARD_READ, SecurityRoles.GROUP_DASHBOARD_READ_WRITE)) {
                // First display the group names
                for (EtmGroup group : groups) {
                    if (group.getDashboards().size() == 0 && !principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)) {
                        // Skip a group when it has no dashboards and the user has read only access.
                        continue;
                    }
                    html.append("<li><a class=\"collapsed\" data-toggle=\"collapse\" href=\"#sub_vis_grp_" + group.hashCode() + "\" aria-expanded=\"false\"><i class=\"fa fa-users\"></i><p>" + StringUtils.escapeToHtml(group.getMostSpecificName()) + "</p><span class=\"caret\"></span></a>");
                    html.append("<div id=\"sub_vis_grp_" + group.hashCode() + "\" class=\"collapse\"><ul class=\"nav nav-collapse subnav\">");
                    var dashboards = new ArrayList<>(group.getDashboards());
                    Collections.sort(dashboards);
                    for (String dashboard : dashboards) {
                        appendMenuOption(html, dashboard, "dashboard/dashboards.html?name=" + StringUtils.urlEncode(dashboard) + "&group=" + StringUtils.urlEncode(group.getName()), !principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ_WRITE));
                    }
                    if (principal.isInRole(SecurityRoles.GROUP_DASHBOARD_READ_WRITE)) {
                        if (group.getDashboards().size() > 0) {
                            html.append(divider);
                        }
                        appendMenuOption(html, "Graphs", "dashboard/graphs.html?group=" + StringUtils.urlEncode(group.getName()), false);
                        appendMenuOption(html, "New dashboard", "dashboard/dashboards.html?action=new&group=" + StringUtils.urlEncode(group.getName()), false);
                    }
                    html.append("</ul></div></li>");
                    hasGroupMenu = true;
                }
            }
            if (principal.isInRole(SecurityRoles.USER_DASHBOARD_READ_WRITE)) {
                if (hasGroupMenu) {
//                  Only add a submenu when there are group menus displayed.
                    html.append(divider);
                    html.append("<li><a class=\"collapsed\" data-toggle=\"collapse\" href=\"#sub_vis_user\" aria-expanded=\"false\"><p>" + StringUtils.escapeToHtml(principal.getName()) + "</p><span class=\"caret\"></span></a>");
                    html.append("<div id=\"sub_vis_user\" class=\"collapse\"><ul class=\"nav nav-collapse subnav\">");
                }
                var dashboards = new ArrayList<>(principal.getDashboards());
                Collections.sort(dashboards);
                for (String dashboard : dashboards) {
                    appendMenuOption(html, dashboard, "dashboard/dashboards.html?name=" + StringUtils.urlEncode(dashboard), false);
                }
                if (principal.getDashboards().size() > 0) {
                    html.append(divider);
                }
                appendMenuOption(html, "Graphs", "dashboard/graphs.html", false);
                appendMenuOption(html, "New dashboard", "dashboard/dashboards.html?action=new", false);
                if (hasGroupMenu) {
                    html.append("</ul></div><li>");
                }
            }
            html.append("</ul></div>");
        }
    }

    /**
     * Add the signal menu option
     *
     * @param principal The <code>EtmPrincipal</code>.
     * @param html      The html buffer.
     */
    private void addSignalMenuOption(EtmPrincipal principal, StringBuilder html) {
        boolean hasGroupMenu = principal.isInAnyRole(SecurityRoles.GROUP_SIGNAL_READ, SecurityRoles.GROUP_SIGNAL_READ_WRITE) && principal.getGroups().size() > 0;
        if (!hasGroupMenu && !principal.isInRole(SecurityRoles.USER_SIGNAL_READ_WRITE)) {
            // The user has no read or read write right on the groups (or is not a member of a group) and has no rights on the user object.
            return;
        }
        if (hasGroupMenu) {
            if (MenuContext.SIGNAL.equals(menuContext)) {
                html.append("<li class=\"nav-item active submenu\">");
            } else {
                html.append("<li class=\"nav-item\">");
            }
            html.append("<a class=\"" + (MenuContext.SIGNAL.equals(menuContext) ? "" : "collapsed") + "\" data-toggle=\"collapse\" href=\"#sub_signals\" aria-expanded=\"" + (MenuContext.SIGNAL.equals(menuContext) ? "true" : "false") + "\"><i class=\"fa fa-bell\"></i><p>Signals</p><span class=\"caret\"></span></a>");
            html.append("<div id=\"sub_signals\" class=\"submenu " + (MenuContext.SIGNAL.equals(menuContext) ? "collapse show" : "collapse") + "\"><ul class=\"nav nav-collapse\">");
            List<EtmGroup> groups = principal.getGroups().stream().sorted(Comparator.comparing(EtmGroup::getMostSpecificName)).collect(Collectors.toList());
            for (EtmGroup group : groups) {
                appendMenuOption(html, group.getMostSpecificName(), "signal/signals.html?group=" + StringUtils.urlEncode(group.getName()), "fa-users", !principal.isInRole(SecurityRoles.GROUP_SIGNAL_READ_WRITE));
            }
            if (principal.isInRole(SecurityRoles.USER_SIGNAL_READ_WRITE)) {
                if (hasGroupMenu) {
                    html.append(divider);
                }
                appendMenuOption(html, principal.getName(), "signal/signals.html", false);
            }
            html.append("</ul><div></li>");
        } else if (principal.isInRole(SecurityRoles.USER_SIGNAL_READ_WRITE)) {
            if (MenuContext.SIGNAL.equals(menuContext)) {
                html.append("<li class=\"nav-item active\">");
            } else {
                html.append("<li class=\"nav-item submenu\">");
            }
            html.append("<a href=\"").append(pathPrefixToContextRoot).append("signal/signals.html\"><i class=\"fa fa-bell\"></i><p>Signals</p></a>");
            html.append("</li>");
        }
    }

    /**
     * Add the preferences menu option
     *
     * @param principal The <code>EtmPrincipal</code>.
     * @param html      The html buffer.
     */
    private void addPreferencesMenuOption(EtmPrincipal principal, StringBuilder html) {
        if (!principal.isInAnyRole(SecurityRoles.ALL_ROLES_ARRAY)) {
            return;
        }
        if (MenuContext.PREFERENCES.equals(menuContext)) {
            html.append("<li class=\"nav-item active\">");
        } else {
            html.append("<li class=\"nav-item\">");
        }
        html.append("<a href=\"").append(pathPrefixToContextRoot).append("preferences/\"><i class=\"fa fa-user\"></i><p>Preferences</p></a>");
        html.append("</li>");
    }

    private void addSettingsMenuOption(EtmPrincipal principal, StringBuilder html) {
        if (!principal.isInAnyRole(
                SecurityRoles.IIB_NODE_READ,
                SecurityRoles.IIB_NODE_READ_WRITE,
                SecurityRoles.IIB_EVENT_READ,
                SecurityRoles.IIB_EVENT_READ_WRITE,
                SecurityRoles.AUDIT_LOG_READ,
                SecurityRoles.CLUSTER_SETTINGS_READ,
                SecurityRoles.CLUSTER_SETTINGS_READ_WRITE,
                SecurityRoles.GROUP_SETTINGS_READ,
                SecurityRoles.GROUP_SETTINGS_READ_WRITE,
                SecurityRoles.IMPORT_PROFILES_READ,
                SecurityRoles.IMPORT_PROFILES_READ_WRITE,
                SecurityRoles.INDEX_STATISTICS_READ,
                SecurityRoles.LICENSE_READ,
                SecurityRoles.LICENSE_READ_WRITE,
                SecurityRoles.NODE_SETTINGS_READ,
                SecurityRoles.NODE_SETTINGS_READ_WRITE,
                SecurityRoles.NOTIFIERS_READ,
                SecurityRoles.NOTIFIERS_READ_WRITE,
                SecurityRoles.PARSER_SETTINGS_READ,
                SecurityRoles.PARSER_SETTINGS_READ_WRITE,
                SecurityRoles.USER_SETTINGS_READ,
                SecurityRoles.USER_SETTINGS_READ_WRITE
        )) {
            return;
        }
        if (MenuContext.SETTINGS.equals(menuContext)) {
            html.append("<li class=\"nav-item active submenu\">");
        } else {
            html.append("<li class=\"nav-item\">");
        }

        html.append("<a class=\"" + (MenuContext.SETTINGS.equals(menuContext) ? "" : "collapsed") + "\" data-toggle=\"collapse\" href=\"#sub_settings\" aria-expanded=\"" + (MenuContext.SETTINGS.equals(menuContext) ? "true" : "false") + "\"><i class=\"fa fa-wrench\"></i><p>Settings</p><span class=\"caret\"></span></a>");
        html.append("<div id=\"sub_settings\" class=\"submenu " + (MenuContext.SETTINGS.equals(menuContext) ? "collapse show" : "collapse") + "\"><ul class=\"nav nav-collapse\">");
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
        if (principal.isInAnyRole(SecurityRoles.IMPORT_PROFILES_READ, SecurityRoles.IMPORT_PROFILES_READ_WRITE)) {
            addedBeforeDivider = true;
            appendMenuOption(html, "Import profiles", "settings/import_profiles.html", !principal.isInRole(SecurityRoles.IMPORT_PROFILES_READ_WRITE));
        }
        if (principal.isInAnyRole(SecurityRoles.NOTIFIERS_READ, SecurityRoles.NOTIFIERS_READ_WRITE)) {
            addedBeforeDivider = true;
            appendMenuOption(html, "Notifiers", "settings/notifiers.html", !principal.isInRole(SecurityRoles.NOTIFIERS_READ_WRITE));
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
                html.append("<a class=\"alert-danger\" href=\"").append(pathPrefixToContextRoot).append(page + "\">");
                html.append("<i class=\"fa fa-ban\">&nbsp;</i>");
            } else if (MenuAwareURLResource.this.etmConfiguration.isLicenseAlmostExpired()) {
                html.append("<a class=\"alert-warning\" href=\"").append(pathPrefixToContextRoot).append(page + "\">");
                html.append("<i class=\"fa fa-exclamation-triangle\"></i>");
            } else {
                html.append("<a href=\"").append(pathPrefixToContextRoot).append(page + "\">");
            }
            html.append("<p>License</p></a></li>");
        }

        // Remove last divider if no content after it.
        if (html.lastIndexOf(divider) == html.length() - divider.length()) {
            html.delete(html.lastIndexOf(divider), html.length());
        }
        html.append("</ul></div></li>");
    }

}
