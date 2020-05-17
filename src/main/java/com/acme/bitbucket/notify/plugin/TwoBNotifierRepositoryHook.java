package com.acme.bitbucket.notify.plugin;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.plugin.spring.scanner.annotation.component.BitbucketComponent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.acme.bitbucket.notify.utils.Constants.HOST_KEY;
import static com.acme.bitbucket.notify.utils.Constants.PLUGIN_SETTINGS_KEY;

@BitbucketComponent("twoBNotifierRepositoryHook")
public class TwoBNotifierRepositoryHook implements PostRepositoryHook<RepositoryHookRequest> {

    private static final Logger logger = LoggerFactory.getLogger(TwoBNotifierRepositoryHook.class);
    private final PluginSettings pluginSettings;

    public TwoBNotifierRepositoryHook(@ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_SETTINGS_KEY);
    }

    @Override
    public void postUpdate(@Nonnull PostRepositoryHookContext postRepositoryHookContext, @Nonnull RepositoryHookRequest repositoryHookRequest) {
        String state;
        RepositoryHookTrigger trigger = repositoryHookRequest.getTrigger();
        if (trigger.equals(StandardRepositoryHookTrigger.BRANCH_CREATE)) {
            state = "created";
        } else if (trigger.equals(StandardRepositoryHookTrigger.BRANCH_DELETE)) {
            state = "deleted";
        } else {
            return;
        }

        List<String> branchNames = repositoryHookRequest.getRefChanges().stream()
                .map(rf -> rf.getRef().getDisplayId())
                .collect(Collectors.toList());

        Map<String, String> params = new HashMap<>();
        params.put("projectId", repositoryHookRequest.getRepository().getProject().getKey());
        params.put("repositoryName", repositoryHookRequest.getRepository().getName());
        params.put("state", state);

        branchNames.forEach(b -> {
            params.put("branchName", b);
            sendMessage(params);
        });

    }

    private void sendMessage(Map<String, String> body) {
        Object host = pluginSettings.get(HOST_KEY);
        if (host == null) {
            return;
        }

        try {
            HttpURLConnection connection = getConnection(String.format("%s/api/internal/bitbucket", host.toString()));
            String requestBody = getRequestBody(body);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            try(OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            connection.connect();
            if (connection.getResponseCode() != 200) {
                logger.error(String.format("Code: %s, Message: %s", connection.getResponseCode(), connection.getResponseMessage()));
            }

        } catch (IOException e) {
            logger.error("Failed to send request", e);
        }
    }

    private String getRequestBody(Map<String, String> body) {
        StringBuilder builder = new StringBuilder("{ ");
        body.forEach((k, v) -> {
            if (builder.length() > 2) {
                builder.append(", ");
            }

            String jsonPart = String.format("\"%s\": \"%s\"", k, v);
            builder.append(jsonPart);
        });

        return builder.append(" }").toString();
    }

    private HttpURLConnection getConnection(String baseUrl) throws IOException {
        URL url = new URL(baseUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        HttpURLConnection.setFollowRedirects(true);

        connection.setReadTimeout(45000);
        connection.setInstanceFollowRedirects(true);
        connection.setDoOutput(true);  // To be able to send data

        return connection;
    }
}
