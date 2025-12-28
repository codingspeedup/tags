package io.github.codingspeedup.tags.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import static io.github.codingspeedup.tags.settings.PluginSecretManager.GEMINI_API_KEY;

@State(name = "TagsPluginSettings", storages = @Storage("tagsPluginSettings.xml"))
public class PluginSettingsState implements PersistentStateComponent<PluginSettingsState> {

    @Getter
    public String geminiModel = "";

    public static PluginSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettingsState.class);
    }

    @Override
    public PluginSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NonNull PluginSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getGeminiApiKey() {
        return PluginSecretManager.getSecret(GEMINI_API_KEY);
    }

}
