package io.github.codingspeedup.tags.settings;

import com.intellij.openapi.options.Configurable;
import dev.langchain4j.model.catalog.ModelType;
import dev.langchain4j.model.googleai.GoogleAiGeminiModelCatalog;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

import static io.github.codingspeedup.tags.settings.PluginSecretManager.GEMINI_API_KEY;


public class PluginConfigurable implements Configurable {
    private PluginSettingsComponent settingsComponent;

    @Override
    public String getDisplayName() {
        return "T.A.G.S.+ Settings";
    }

    @Override
    public JComponent createComponent() {
        settingsComponent = new PluginSettingsComponent();
        settingsComponent.setGeminiModels(loadGeminiModels());
        return settingsComponent.getPanel();
    }

    private Collection<ComboEntry> loadGeminiModels() {
        var comboEntries = new ArrayList<ComboEntry>();
        comboEntries.add(ComboEntry.EMPTY_VALUE);
        try {
            var settings = PluginSettingsState.getInstance();
            var modelCatalog = GoogleAiGeminiModelCatalog.builder().apiKey(settings.getGeminiApiKey()).build();
            var models = modelCatalog.listModels().stream()
                    .filter(item -> ModelType.CHAT.equals(item.type()))
                    .map(item -> new ComboEntry(item.name(), item.displayName() + " (" + item.name() + ")"))
                    .toList();
            comboEntries.addAll(models);
        } catch (Exception e) {
            comboEntries.add(new ComboEntry(StringUtils.EMPTY, e.getMessage()));
        }
        return comboEntries;
    }

    @Override
    public boolean isModified() {
        var settings = PluginSettingsState.getInstance();
        return !(StringUtils.equals(settings.getGeminiModel(), settingsComponent.getGeminiModel())
                && StringUtils.equals(settings.getGeminiApiKey(), settingsComponent.getGeminiApiKey())
        );
    }

    @Override
    public void apply() {
        PluginSecretManager.saveSecret(GEMINI_API_KEY, settingsComponent.getGeminiApiKey());
        var settings = PluginSettingsState.getInstance();
        settings.geminiModel = settingsComponent.getGeminiModel();
    }

    @Override
    public void reset() {
        var settings = PluginSettingsState.getInstance();
        settingsComponent.setGeminiApiKey(settings.getGeminiApiKey());
        settingsComponent.setGeminiModel(settings.getGeminiModel());
    }

}
