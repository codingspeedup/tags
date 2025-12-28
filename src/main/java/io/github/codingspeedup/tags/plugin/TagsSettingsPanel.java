package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.FormBuilder;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.util.Collection;

public class TagsSettingsPanel {


    private final JPanel mainPanel;

    private final JBPasswordField geminiApiKeyField = new JBPasswordField();

    private final DefaultComboBoxModel<ComboEntry> geminiModelsComboModel = new DefaultComboBoxModel<>(new ComboEntry[]{ComboEntry.EMPTY_VALUE});
    private final ComboBox<ComboEntry> geminiModelField = new ComboBox<>(geminiModelsComboModel);

    public TagsSettingsPanel() {
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

}
