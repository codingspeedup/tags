package io.github.codingspeedup.tags.plugin.settings;

import com.azure.ai.openai.OpenAIServiceVersion;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public class SettingsView implements Disposable {

    private final JPanel mainPanel;

    private final JBCheckBox azureOpenAiEnabled = new JBCheckBox("Use this model");
    private final JBPasswordField azureOpenAiApiKeyField = new JBPasswordField();
    private final JBTextField azureOpenAiUrlField = new JBTextField();
    private final JBTextField azureOpenAiDeploymentField = new JBTextField();
    private final DefaultComboBoxModel<String> azureOpenAiApiVersionComboModel = new DefaultComboBoxModel<>();
    private final ComboBox<String> azureOpenAiApiVersionField = new ComboBox<>(azureOpenAiApiVersionComboModel);

    private final JBCheckBox geminiEnabled = new JBCheckBox("Use this model");
    private final JBPasswordField geminiApiKeyField = new JBPasswordField();
    private final DefaultComboBoxModel<SettingsComboEntry> geminiModelsComboModel = new DefaultComboBoxModel<>(new SettingsComboEntry[]{SettingsComboEntry.EMPTY_VALUE});
    private final ComboBox<SettingsComboEntry> geminiModelField = new ComboBox<>(geminiModelsComboModel);

    private final JBCheckBox ollamaEnabled = new JBCheckBox("Use this model");
    private final JBTextField ollamaUrlField = new JBTextField();
    private final DefaultComboBoxModel<SettingsComboEntry> ollamaModelsComboModel = new DefaultComboBoxModel<>(new SettingsComboEntry[]{SettingsComboEntry.EMPTY_VALUE});
    private final ComboBox<SettingsComboEntry> ollamaModelField = new ComboBox<>(ollamaModelsComboModel);

    public SettingsView() {
        Arrays.stream(OpenAIServiceVersion.values())
                .map(OpenAIServiceVersion::getVersion)
                .sorted(Comparator.reverseOrder())
                .forEach(azureOpenAiApiVersionComboModel::addElement);

        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(new TitledSeparator("Azure OpenAI Configuration"))
                .addComponent(azureOpenAiEnabled)
                .addLabeledComponent(new JBLabel("API key: "), azureOpenAiApiKeyField, 1, false)
                .addLabeledComponent(new JBLabel("URL: "), azureOpenAiUrlField, 1, false)
                .addLabeledComponent(new JBLabel("Deployment: "), azureOpenAiDeploymentField, 1, false)
                .addLabeledComponent(new JBLabel("API version: "), azureOpenAiApiVersionField, 1, false)

                .addVerticalGap(10)
                .addComponent(geminiEnabled)
                .addComponent(new TitledSeparator("Gemini Configuration"))
                .addLabeledComponent(new JBLabel("API key: "), geminiApiKeyField, 1, false)
                .addLabeledComponent(new JBLabel("Model: "), geminiModelField, 1, false)

                .addVerticalGap(10)
                .addComponent(ollamaEnabled)
                .addComponent(new TitledSeparator("Ollama Configuration"))
                .addLabeledComponent(new JBLabel("URL: "), ollamaUrlField, 1, false)
                .addLabeledComponent(new JBLabel("Model: "), ollamaModelField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        azureOpenAiEnabled.addActionListener(e -> {
            var value = azureOpenAiEnabled.isSelected();
            azureOpenAiApiKeyField.setEnabled(value);
            azureOpenAiUrlField.setEnabled(value);
            azureOpenAiDeploymentField.setEnabled(value);
            azureOpenAiApiVersionField.setEnabled(value);
        });

        geminiEnabled.addActionListener(e -> {
            var value = geminiEnabled.isSelected();
            geminiApiKeyField.setEnabled(value);
            geminiModelField.setEnabled(value);
        });

        ollamaEnabled.addActionListener(e -> {
            var value = ollamaEnabled.isSelected();
            ollamaUrlField.setEnabled(value);
            ollamaModelField.setEnabled(value);
        });

        new ComponentValidator(this).installOn(azureOpenAiApiKeyField);
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

    /*
     * ================
     * = Azure OpenAI =
     * ================
     */

    public boolean isUseAzureOpenAiModel() {
        return azureOpenAiEnabled.isSelected();
    }

    public void setUseAzureOpenAiModel(boolean value) {
        azureOpenAiEnabled.setSelected(value);
    }

    public String getAzureOpenAiApiKey() {
        return new String(azureOpenAiApiKeyField.getPassword());
    }

    public void setAzureOpenAiApiKey(String text) {
        azureOpenAiApiKeyField.setText(text);
    }

    public String getAzureOpenAiUrl() {
        return azureOpenAiUrlField.getText();
    }

    public void setAzureOpenAiError(String message) {
        var validator = ComponentValidator.getInstance(azureOpenAiApiKeyField).orElseThrow();
        var info = StringUtils.isBlank(message)
                ? null
                : new ValidationInfo(message, azureOpenAiApiKeyField);
        validator.updateInfo(info);
    }

    public void setAzureOpenAiUrl(String text) {
        azureOpenAiUrlField.setText(text);
    }

    public String getAzureOpenAiDeployment() {
        return azureOpenAiDeploymentField.getText();
    }

    public void setAzureOpenAiDeployment(String text) {
        azureOpenAiDeploymentField.setText(text);
    }

    public String getAzureOpenAiApiVersion() {
        var selectedItem = (String) azureOpenAiApiVersionField.getSelectedItem();
        if (selectedItem != null) {
            selectedItem = azureOpenAiApiVersionComboModel.getElementAt(0);
        }
        return selectedItem;
    }

    public void setAzureOpenAiApiVersion(String value) {
        azureOpenAiApiVersionField.setSelectedItem(value);
        if (azureOpenAiApiVersionField.getSelectedItem() == null) {
            azureOpenAiApiVersionField.setSelectedIndex(0);
        }
    }

    /*
     * ==========
     * = Gemini =
     * ==========
     */

    public boolean isUseGeminiModel() {
        return geminiEnabled.isSelected();
    }

    public void setUseGeminiModel(boolean value) {
        geminiEnabled.setSelected(value);
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
        var selectedItem = (SettingsComboEntry) geminiModelField.getSelectedItem();
        if (selectedItem != null) {
            if (!SettingsComboEntry.EMPTY_VALUE.equals(selectedItem)) {
                return selectedItem.getCode();
            }
        }
        return StringUtils.EMPTY;
    }

    public void setGeminiModel(String text) {
        if (StringUtils.isEmpty(text)) {
            geminiModelField.setSelectedIndex(0);
        } else {
            geminiModelField.setSelectedItem(new SettingsComboEntry(text, null));
        }
    }

    public void setGeminiModels(Collection<SettingsComboEntry> comboEntries) {
        geminiModelsComboModel.removeAllElements();
        comboEntries.forEach(geminiModelsComboModel::addElement);
    }

    /*
     * ==========
     * = Ollama =
     * ==========
     */

    public boolean isUseOllamaModel() {
        return ollamaEnabled.isSelected();
    }

    public void setUseOllamaModel(boolean value) {
        ollamaEnabled.setSelected(value);
    }

    public String getOllamaUrl() {
        return ollamaUrlField.getText();
    }

    public void setOllamaUrl(String text) {
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
        var selectedItem = (SettingsComboEntry) ollamaModelField.getSelectedItem();
        if (selectedItem != null) {
            if (!SettingsComboEntry.EMPTY_VALUE.equals(selectedItem)) {
                return selectedItem.getCode();
            }
        }
        return StringUtils.EMPTY;
    }

    public void setOllamaModel(String text) {
        if (StringUtils.isEmpty(text)) {
            ollamaModelField.setSelectedIndex(0);
        } else {
            ollamaModelField.setSelectedItem(new SettingsComboEntry(text, null));
        }
    }

    public void setOllamaModels(Collection<SettingsComboEntry> comboEntries) {
        ollamaModelsComboModel.removeAllElements();
        comboEntries.forEach(ollamaModelsComboModel::addElement);
    }


}
