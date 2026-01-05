package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.application.ApplicationManager;

public interface TagsSettings {

    static TagsSettings getInstance() {
        return ApplicationManager.getApplication().getService(TagsSettingsState.class);
    }

    boolean isUseAzureOpenAiModel();

    String getAzureOpenAiApiKey();

    String getAzureOpenAiUrl();

    String getAzureOpenAiDeployment();

    String getAzureOpenAiApiVersion();

    String getGeminiApiKey();

    String getGeminiModel();

    String getOllamaUrl();

    String getOllamaModel();

}
