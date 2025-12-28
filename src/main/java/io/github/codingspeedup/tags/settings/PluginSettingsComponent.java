package io.github.codingspeedup.tags.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

public class PluginSettingsComponent {

    private final JPanel mainPanel;

    private final JBPasswordField geminiApiKeyField = new JBPasswordField();
    private final JBTextField geminiModelField = new JBTextField();

    public PluginSettingsComponent() {
        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Gemini API key: "), geminiApiKeyField, 1, false)
                .addLabeledComponent(new JBLabel("Gemini model: "), geminiModelField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public String getGeminiApiKey() {
        return new String(geminiApiKeyField.getPassword());
    }

    public void setGeminiApiKey(String text) {
        geminiApiKeyField.setText(text);
    }

    public String getGeminiModel() {
        return geminiModelField.getText();
    }

    public void setGeminiModel(String text) {
        geminiModelField.setText(text);
    }

}
