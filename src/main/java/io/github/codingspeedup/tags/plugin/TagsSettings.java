package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.application.ApplicationManager;

public interface TagsSettings {

    static TagsSettings getInstance() {
        return ApplicationManager.getApplication().getService(TagsSettingsState.class);
    }

    String getGeminiApiKey();

    String getGeminiModel();

    String getOllamaURL();

    String getOllamaModel();

}
