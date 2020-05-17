package com.acme.bitbucket.notify.servlet;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static com.acme.bitbucket.notify.utils.Constants.HOST_KEY;
import static com.acme.bitbucket.notify.utils.Constants.PLUGIN_SETTINGS_KEY;

public class TwoBNotifierServlet extends HttpServlet{

    @ComponentImport
    private final AuthenticationContext authenticationContext;
    @ComponentImport
    private final SoyTemplateRenderer soyTemplateRenderer;
    @ComponentImport
    private final NavBuilder navBuilder;
    @ComponentImport
    private final PageBuilderService pageBuilderService;

    private static final Logger log = LoggerFactory.getLogger(TwoBNotifierServlet.class);
    private final PluginSettings pluginSettings;

    public TwoBNotifierServlet(AuthenticationContext authenticationContext, SoyTemplateRenderer soyTemplateRenderer,
                               NavBuilder navBuilder, @ComponentImport PluginSettingsFactory pluginSettingsFactory,
                               PageBuilderService pageBuilderService) {
        this.authenticationContext = authenticationContext;
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.navBuilder = navBuilder;
        this.pageBuilderService = pageBuilderService;
        this.pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_SETTINGS_KEY);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        Object defaultHost = pluginSettings.get(HOST_KEY);
        if (defaultHost == null) {
            defaultHost = "";
        }

        Map<String, Object> data = ImmutableMap.of("defaultHost", defaultHost);

        resp.setContentType("text/html;charset=UTF-8");
        if (authenticationContext.isAuthenticated()) {
            render(resp, "twoBNotifier.admin.settings", data);
        } else {
            resp.sendRedirect(
                    navBuilder.login().next(req.getServletPath() + req.getPathInfo()).buildAbsolute()
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        pluginSettings.put(HOST_KEY, req.getParameter(HOST_KEY));
        resp.setStatus(200);
    }

    private void render(HttpServletResponse response, String templateName, Map<String, Object> data) throws IOException
    {
        soyTemplateRenderer.render(
                response.getWriter(),
                "com.acme.bitbucket.2b-git-notify-plugin:twoBNotifier-admin-soy",
                templateName,
                data
        );
    }

}