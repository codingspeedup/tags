package io.github.codingspeedup.tags.plugin;

import com.azure.ai.openai.OpenAIServiceVersion;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import static io.github.codingspeedup.tags.plugin.TagsSettingsSecretManager.AZURE_OPEN_AI_API_KEY;
import static io.github.codingspeedup.tags.plugin.TagsSettingsSecretManager.GEMINI_API_KEY;

@State(name = "TagsPluginSettings", storages = @Storage("tagsPluginSettings.xml"))
class TagsSettingsState implements PersistentStateComponent<TagsSettingsState>, TagsSettings {

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

    public static TagsSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(TagsSettingsState.class);
    }

    @Override
    public TagsSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NonNull TagsSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public String getGeminiApiKey() {
        return TagsSettingsSecretManager.getSecret(GEMINI_API_KEY);
    }

    public String getAzureOpenAiApiKey() {
        return TagsSettingsSecretManager.getSecret(AZURE_OPEN_AI_API_KEY);
    }
}
