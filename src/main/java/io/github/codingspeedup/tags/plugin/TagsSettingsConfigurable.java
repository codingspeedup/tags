package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.Messages;
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
        var settings = TagsSettingsState.getInstance();
        settingsComponent = new TagsSettingsPanel();
        try {
            settingsComponent.setGeminiModels(readGeminiModels(settings.getGeminiApiKey()));
        } catch (Exception e) {
            reportValidationException(e);
        }
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        var settings = TagsSettingsState.getInstance();
        var modified = !StringUtils.equals(settings.getGeminiApiKey(), settingsComponent.getGeminiApiKey());
        modified = modified || !StringUtils.equals(settings.getGeminiModel(), settingsComponent.getGeminiModel());
        return modified;
    }

    @Override
    public void apply() {
        try {
            validateComponent();
            TagsSettingsSecretManager.saveSecret(GEMINI_API_KEY, settingsComponent.getGeminiApiKey());
            var settings = TagsSettingsState.getInstance();
            settings.geminiModel = settingsComponent.getGeminiModel();
        } catch (Exception e) {
            reportValidationException(e);
        }
    }

    @Override
    public void reset() {
        var settings = TagsSettingsState.getInstance();
        settingsComponent.setGeminiApiKey(settings.getGeminiApiKey());
        settingsComponent.setGeminiModel(settings.getGeminiModel());
    }

    private void validateComponent() {
        var settings = TagsSettingsState.getInstance();
        if (!StringUtils.equals(settings.getGeminiApiKey(), settingsComponent.getGeminiApiKey())) {
            var geminiModels = readGeminiModels(settingsComponent.getGeminiApiKey());
            settingsComponent.setGeminiModels(geminiModels);
        }
    }

    private void reportValidationException(Exception e) {
        Messages.showErrorDialog(
                settingsComponent.getPanel(),
                e.getMessage(),
                "Validation Error"
        );
    }

    private Collection<ComboEntry> readGeminiModels(String geminiApiKey) {
        var comboEntries = new ArrayList<ComboEntry>();
        comboEntries.add(ComboEntry.EMPTY_VALUE);
        if (StringUtils.isNotBlank(geminiApiKey)) {
            try {
                var modelCatalog = GoogleAiGeminiModelCatalog.builder().apiKey(geminiApiKey).build();
                modelCatalog.listModels().stream()
                        .filter(item -> ModelType.CHAT.equals(item.type()))
                        .map(item -> new ComboEntry(item.name(), item.displayName() + " (" + item.name() + ")"))
                        .forEach(comboEntries::add);
            } catch (Exception e) {
                throw new RuntimeException("Reading Gemini model catalog:\n" + e.getMessage());
            }
        }
        return comboEntries;
    }

}
