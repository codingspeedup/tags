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
class PluginSettingsStateImpl implements PersistentStateComponent<PluginSettingsStateImpl>, PluginSettingsState {

    @Getter
    public String geminiModel = "";

    public static PluginSettingsStateImpl getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettingsStateImpl.class);
    }

    @Override
    public PluginSettingsStateImpl getState() {
        return this;
    }

    @Override
    public void loadState(@NonNull PluginSettingsStateImpl state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public String getGeminiApiKey() {
        return PluginSecretManager.getSecret(GEMINI_API_KEY);
    }

}
