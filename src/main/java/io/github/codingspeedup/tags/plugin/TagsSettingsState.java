package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import static io.github.codingspeedup.tags.plugin.TagsSettingsSecretManager.GEMINI_API_KEY;

@State(name = "TagsPluginSettings", storages = @Storage("tagsPluginSettings.xml"))
class TagsSettingsState implements PersistentStateComponent<TagsSettingsState>, TagsSettings {

    @Getter
    public String geminiModel = "";

    @Getter
    public String ollamaURL = "http://localhost:11434";

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

}
