package io.github.codingspeedup.tags.settings;

import com.intellij.openapi.application.ApplicationManager;

public interface PluginSettingsState {

    String getGeminiApiKey();

    String getGeminiModel();

    static PluginSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettingsStateImpl.class);
    }

}
