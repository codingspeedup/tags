package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.options.Configurable;
import dev.langchain4j.model.catalog.ModelType;
import dev.langchain4j.model.googleai.GoogleAiGeminiModelCatalog;
import io.github.codingspeedup.tags.MyMessageBundle;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

import static io.github.codingspeedup.tags.plugin.TagsSettingsSecretManager.GEMINI_API_KEY;


public class TagsSettingsConfigurable implements Configurable {
    private TagsSettingsPanel settingsComponent;

    @Override
    public String getDisplayName() {
        return MyMessageBundle.message("plugin.label") + " Settings";
    }

    @Override
    public JComponent createComponent() {
        settingsComponent = new TagsSettingsPanel();
        settingsComponent.setGeminiModels(loadGeminiModels());
        return settingsComponent.getPanel();
    }

    private Collection<ComboEntry> loadGeminiModels() {
        var comboEntries = new ArrayList<ComboEntry>();
        comboEntries.add(ComboEntry.EMPTY_VALUE);
        try {
            var settings = TagsSettingsState.getInstance();
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
        var settings = TagsSettingsState.getInstance();
        return !(StringUtils.equals(settings.getGeminiModel(), settingsComponent.getGeminiModel())
                && StringUtils.equals(settings.getGeminiApiKey(), settingsComponent.getGeminiApiKey())
        );
    }

    @Override
    public void apply() {
        TagsSettingsSecretManager.saveSecret(GEMINI_API_KEY, settingsComponent.getGeminiApiKey());
        var settings = TagsSettingsState.getInstance();
        settings.geminiModel = settingsComponent.getGeminiModel();
    }

    @Override
    public void reset() {
        var settings = TagsSettingsState.getInstance();
        settingsComponent.setGeminiApiKey(settings.getGeminiApiKey());
        settingsComponent.setGeminiModel(settings.getGeminiModel());
    }

}
