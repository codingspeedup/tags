package io.github.codingspeedup.tags.plugin.settings;

import com.azure.ai.openai.OpenAIServiceVersion;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import io.github.codingspeedup.tags.ai.boundary.EnvironmentSettingsProvider;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import static io.github.codingspeedup.tags.plugin.settings.SettingsSecretManager.AZURE_OPEN_AI_API_KEY;
import static io.github.codingspeedup.tags.plugin.settings.SettingsSecretManager.GEMINI_API_KEY;

@State(name = "TagsPluginSettings", storages = @Storage("tagsPluginSettings.xml"))
public class SettingsState implements PersistentStateComponent<SettingsState>, EnvironmentSettingsProvider {

    @Getter
    public boolean useAzureOpenAiModel = false;
    @Getter
    public String azureOpenAiUrl = "https://resource-group.cognitiveservices.azure.com";
    @Getter
    public String azureOpenAiDeployment = "gpt-4o-mini";
    @Getter
    public String azureOpenAiApiVersion = OpenAIServiceVersion.V2025_01_01_PREVIEW.getVersion();

    @Getter
    public String geminiModel = "";

    @Getter
    public String ollamaUrl = "http://localhost:11434";
    @Getter
    public String ollamaModel = "";

    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    @Override
    public SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NonNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public String getGeminiApiKey() {
        return SettingsSecretManager.getSecret(GEMINI_API_KEY);
    }

    public String getAzureOpenAiApiKey() {
        return SettingsSecretManager.getSecret(AZURE_OPEN_AI_API_KEY);
    }
}
