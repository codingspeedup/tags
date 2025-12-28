package io.github.codingspeedup.tags.settings;

import com.intellij.openapi.options.Configurable;
import org.apache.commons.lang3.Strings;

import javax.swing.*;

import static io.github.codingspeedup.tags.settings.PluginSecretManager.GEMINI_API_KEY;


public class PluginConfigurable implements Configurable {
    private PluginSettingsComponent settingsComponent;

    @Override
    public String getDisplayName() {
        return "My Plugin Settings";
    }

    @Override
    public JComponent createComponent() {
        settingsComponent = new PluginSettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        var settings = PluginSettingsState.getInstance();
        return !(Strings.CS.equals(settings.getGeminiModel(), settingsComponent.getGeminiModel())
                && Strings.CS.equals(settings.getGeminiApiKey(), settingsComponent.getGeminiApiKey())
        );
    }

    @Override
    public void apply() {
        var settings = PluginSettingsState.getInstance();
        settings.geminiModel = settingsComponent.getGeminiModel();
        PluginSecretManager.saveSecret(GEMINI_API_KEY, settingsComponent.getGeminiApiKey());
    }

    @Override
    public void reset() {
        var settings = PluginSettingsState.getInstance();
        settingsComponent.setGeminiModel(settings.getGeminiModel());
        settingsComponent.setGeminiApiKey(settings.getGeminiApiKey());
    }

}
