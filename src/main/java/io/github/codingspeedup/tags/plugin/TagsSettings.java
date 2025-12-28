package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.application.ApplicationManager;

public interface TagsSettings {

    String getGeminiApiKey();

    String getGeminiModel();

    static TagsSettings getInstance() {
        return ApplicationManager.getApplication().getService(TagsSettingsState.class);
    }

}
