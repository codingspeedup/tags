package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.util.Collection;

public class TagsSettingsPanel implements Disposable {

    private final JPanel mainPanel;

    private final JBPasswordField geminiApiKeyField = new JBPasswordField();
    private final DefaultComboBoxModel<ComboEntry> geminiModelsComboModel = new DefaultComboBoxModel<>(new ComboEntry[]{ComboEntry.EMPTY_VALUE});
    private final ComboBox<ComboEntry> geminiModelField = new ComboBox<>(geminiModelsComboModel);

    private final JBTextField ollamaUrlField = new JBTextField();
    private final DefaultComboBoxModel<ComboEntry> ollamaModelsComboModel = new DefaultComboBoxModel<>(new ComboEntry[]{ComboEntry.EMPTY_VALUE});
    private final ComboBox<ComboEntry> ollamaModelField = new ComboBox<>(ollamaModelsComboModel);

    public TagsSettingsPanel() {
        mainPanel = FormBuilder.createFormBuilder()

                .addComponent(new TitledSeparator("Gemini Configuration"))
                .addLabeledComponent(new JBLabel("API key: "), geminiApiKeyField, 1, false)
                .addLabeledComponent(new JBLabel("Model: "), geminiModelField, 1, false)

                .addVerticalGap(10)
                .addComponent(new TitledSeparator("Ollama Configuration"))
                .addLabeledComponent(new JBLabel("URL: "), ollamaUrlField, 1, false)
                .addLabeledComponent(new JBLabel("Model: "), ollamaModelField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        new ComponentValidator(this).installOn(geminiApiKeyField);
        new ComponentValidator(this).installOn(ollamaUrlField);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    @Override
    public void dispose() {
        // Keep calm and dispose
    }

    public String getGeminiApiKey() {
        return new String(geminiApiKeyField.getPassword());
    }

    public void setGeminiApiKey(String text) {
        geminiApiKeyField.setText(text);
    }

    public void setGeminiError(String message) {
        var validator = ComponentValidator.getInstance(geminiApiKeyField).orElseThrow();
        var info = StringUtils.isBlank(message)
                ? null
                : new ValidationInfo(message, geminiApiKeyField);
        validator.updateInfo(info);
    }

    public String getGeminiModel() {
        var selectedItem = (ComboEntry) geminiModelField.getSelectedItem();
        if (selectedItem != null) {
            if (!ComboEntry.EMPTY_VALUE.equals(selectedItem)) {
                return selectedItem.getCode();
            }
        }
        return StringUtils.EMPTY;
    }

    public void setGeminiModel(String text) {
        if (StringUtils.isEmpty(text)) {
            geminiModelField.setSelectedIndex(0);
        } else {
            geminiModelField.setSelectedItem(new ComboEntry(text, null));
        }
    }

    public void setGeminiModels(Collection<ComboEntry> comboEntries) {
        geminiModelsComboModel.removeAllElements();
        comboEntries.forEach(geminiModelsComboModel::addElement);
    }

    public String getOllamaURL() {
        return ollamaUrlField.getText();
    }

    public void setOllamaURL(String text) {
        ollamaUrlField.setText(text);
    }

    public void setOllamaError(String message) {
        var validator = ComponentValidator.getInstance(ollamaUrlField).orElseThrow();
        var info = StringUtils.isBlank(message)
                ? null
                : new ValidationInfo(message, ollamaUrlField);
        validator.updateInfo(info);
    }

    public String getOllamaModel() {
        var selectedItem = (ComboEntry) ollamaModelField.getSelectedItem();
        if (selectedItem != null) {
            if (!ComboEntry.EMPTY_VALUE.equals(selectedItem)) {
                return selectedItem.getCode();
            }
        }
        return StringUtils.EMPTY;
    }

    public void setOllamaModel(String text) {
        if (StringUtils.isEmpty(text)) {
            ollamaModelField.setSelectedIndex(0);
        } else {
            ollamaModelField.setSelectedItem(new ComboEntry(text, null));
        }
    }

    public void setOllamaModels(Collection<ComboEntry> comboEntries) {
        ollamaModelsComboModel.removeAllElements();
        comboEntries.forEach(ollamaModelsComboModel::addElement);
    }


}
