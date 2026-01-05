package io.github.codingspeedup.tags.plugin;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.Disposer;
import dev.langchain4j.model.catalog.ModelType;
import dev.langchain4j.model.googleai.GoogleAiGeminiModelCatalog;
import io.github.codingspeedup.tags.MyMessageBundle;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static io.github.codingspeedup.tags.integration.llms.AzureOpenAI.identifyVersion;
import static io.github.codingspeedup.tags.plugin.TagsSettingsSecretManager.AZURE_OPEN_AI_API_KEY;
import static io.github.codingspeedup.tags.plugin.TagsSettingsSecretManager.GEMINI_API_KEY;


public class TagsSettingsConfigurable implements Configurable {
    private static final Logger LOG = Logger.getInstance(TagsSettingsConfigurable.class);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private TagsSettingsPanel settingsComponent;

    @Override
    public String getDisplayName() {
        return MyMessageBundle.message("plugin.label") + " Settings";
    }

    @Override
    public JComponent createComponent() {
        var settings = TagsSettingsState.getInstance();
        settingsComponent = new TagsSettingsPanel();
        if (settings.isUseAzureOpenAiModel()) {
            validateAzureOpenAiConnection(
                    settings.getAzureOpenAiApiKey(),
                    settings.getAzureOpenAiUrl(),
                    settings.getAzureOpenAiDeployment(),
                    settings.getAzureOpenAiApiVersion());
        }
        settingsComponent.setGeminiModels(readGeminiModels(settings.getGeminiApiKey()));
        settingsComponent.setOllamaModels(readOllamaModels(settings.getOllamaUrl()));
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        var settings = TagsSettingsState.getInstance();

        var modified = settings.isUseAzureOpenAiModel() != settingsComponent.isUseAzureOpenAiModel();
        modified = modified || !StringUtils.equals(settings.getAzureOpenAiApiKey(), settingsComponent.getAzureOpenAiApiKey());
        modified = modified || !StringUtils.equals(settings.getAzureOpenAiUrl(), settingsComponent.getAzureOpenAiUrl());
        modified = modified || !StringUtils.equals(settings.getAzureOpenAiDeployment(), settingsComponent.getAzureOpenAiDeployment());
        modified = modified || !StringUtils.equals(settings.getAzureOpenAiApiVersion(), settingsComponent.getAzureOpenAiApiVersion());

        modified = modified || !StringUtils.equals(settings.getGeminiApiKey(), settingsComponent.getGeminiApiKey());
        modified = modified || !StringUtils.equals(settings.getGeminiModel(), settingsComponent.getGeminiModel());

        modified = modified || !StringUtils.equals(settings.getOllamaUrl(), settingsComponent.getOllamaUrl());
        modified = modified || !StringUtils.equals(settings.getOllamaModel(), settingsComponent.getOllamaModel());
        return modified;
    }

    @Override
    public void apply() {
        validateComponent();

        var settings = TagsSettingsState.getInstance();

        settings.useAzureOpenAiModel = settingsComponent.isUseAzureOpenAiModel();
        TagsSettingsSecretManager.saveSecret(AZURE_OPEN_AI_API_KEY, settingsComponent.getAzureOpenAiApiKey());
        settings.azureOpenAiUrl = settingsComponent.getAzureOpenAiUrl();
        settings.azureOpenAiDeployment = settingsComponent.getAzureOpenAiDeployment();
        settings.azureOpenAiApiVersion = settingsComponent.getAzureOpenAiApiVersion();

        TagsSettingsSecretManager.saveSecret(GEMINI_API_KEY, settingsComponent.getGeminiApiKey());
        settings.geminiModel = settingsComponent.getGeminiModel();

        settings.ollamaUrl = settingsComponent.getOllamaUrl();
        settings.ollamaModel = settingsComponent.getOllamaModel();
    }

    @Override
    public void reset() {
        var settings = TagsSettingsState.getInstance();

        settingsComponent.setUseAzureOpenAiModel(settings.isUseAzureOpenAiModel());
        settingsComponent.setAzureOpenAiApiKey(settings.getAzureOpenAiApiKey());
        settingsComponent.setAzureOpenAiUrl(settings.getAzureOpenAiUrl());
        settingsComponent.setAzureOpenAiDeployment(settings.getAzureOpenAiDeployment());
        settingsComponent.setAzureOpenAiApiVersion(settings.getAzureOpenAiApiVersion());

        settingsComponent.setGeminiApiKey(settings.getGeminiApiKey());
        settingsComponent.setGeminiModel(settings.getGeminiModel());

        settingsComponent.setOllamaUrl(settings.getOllamaUrl());
        settingsComponent.setOllamaModel(settings.getOllamaModel());
    }

    @Override
    public void disposeUIResources() {
        if (settingsComponent != null) {
            Disposer.dispose(settingsComponent);
            settingsComponent = null;
        }
    }

    private void validateComponent() {
        var settings = TagsSettingsState.getInstance();
        if (settingsComponent.isUseAzureOpenAiModel()) {
            validateAzureOpenAiConnection(
                    settingsComponent.getAzureOpenAiApiKey(),
                    settingsComponent.getAzureOpenAiUrl(),
                    settingsComponent.getAzureOpenAiDeployment(),
                    settingsComponent.getAzureOpenAiApiVersion()
            );
        }
        if (!StringUtils.equals(settings.getGeminiApiKey(), settingsComponent.getGeminiApiKey())) {
            settingsComponent.setGeminiModels(readGeminiModels(settingsComponent.getGeminiApiKey()));
        }
        if (!StringUtils.equals(settings.getOllamaUrl(), settingsComponent.getOllamaUrl())) {
            settingsComponent.setOllamaModels(readOllamaModels(settingsComponent.getOllamaUrl()));
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void validateAzureOpenAiConnection(String apiKey, String endpoint, String deploymentName, String apiVersion) {
        settingsComponent.setAzureOpenAiError(null);
        try {
            var client = new OpenAIClientBuilder()
                    .credential(new AzureKeyCredential(apiKey))
                    .serviceVersion(identifyVersion(apiVersion).orElse(null))
                    .endpoint(endpoint)
                    .buildClient();

            var chatCompletionsOptions = new ChatCompletionsOptions(List.of(new ChatRequestUserMessage("Hi")))
                    .setMaxTokens(1);

            var chatCompletions = client.getChatCompletions(deploymentName, chatCompletionsOptions);
            Objects.nonNull(chatCompletions.getChoices().get(0).getMessage().getContent());
        } catch (Exception e) {
            LOG.error("Checking Azure OpenAI connection", e);
            settingsComponent.setAzureOpenAiError(e.getMessage());
        }
    }

    private Collection<ComboEntry> readGeminiModels(String geminiApiKey) {
        settingsComponent.setGeminiError(null);
        var comboEntries = new ArrayList<ComboEntry>();
        if (StringUtils.isNotBlank(geminiApiKey)) {
            try {
                var modelCatalog = GoogleAiGeminiModelCatalog.builder().apiKey(geminiApiKey).build();
                modelCatalog.listModels().stream()
                        .filter(item -> ModelType.CHAT.equals(item.type()))
                        .map(item -> new ComboEntry(item.name(), item.displayName() + " (" + item.name() + ")"))
                        .forEach(comboEntries::add);
            } catch (Exception e) {
                LOG.error("Reading Gemini models", e);
            }
            if (comboEntries.isEmpty()) {
                settingsComponent.setGeminiError("No models could be extracted");
            }
        }
        comboEntries.add(0, ComboEntry.EMPTY_VALUE);
        return comboEntries;
    }

    private Collection<ComboEntry> readOllamaModels(String ollamaURL) {
        settingsComponent.setOllamaError(null);
        var comboEntries = new ArrayList<ComboEntry>();
        if (StringUtils.isNotBlank(ollamaURL)) {
            try {
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format("%s/api/tags", ollamaURL)))
                        .GET()
                        .build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    var jsonBody = JsonParser.parseString(response.body()).getAsJsonObject();
                    jsonBody.getAsJsonArray("models").forEach(jsonModel -> {
                        var name = jsonModel.getAsJsonObject().get("name").getAsString();
                        var model = jsonModel.getAsJsonObject().get("model").getAsString();
                        comboEntries.add(new ComboEntry(model, name + " (" + model + ")"));
                    });
                }
            } catch (Exception e) {
                LOG.error("Reading Ollama models", e);
            }
            if (comboEntries.isEmpty()) {
                settingsComponent.setOllamaError("No models could be extracted");
            }
        }
        comboEntries.add(0, ComboEntry.EMPTY_VALUE);
        return comboEntries;
    }

}
