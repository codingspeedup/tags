package io.github.codingspeedup.tags;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import io.github.codingspeedup.tags.integration.LLM;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MyToolWindowFactory implements ToolWindowFactory {

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        MyToolWindow myToolWindow = new MyToolWindow();
        Content content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false);
        toolWindow.getContentManager().addContent(content);
    }

    public static class MyToolWindow {

        @Getter
        private final JBPanel<JBPanel<?>> content;
        private final JBLabel label;

        public MyToolWindow() {
            this.content = new JBPanel<>();
            this.label = new JBLabel("The random number is: ?");

            JButton shuffleButton = new JButton("Shuffle");
            shuffleButton.addActionListener(e -> {
                var chatResponse = LLM.chat("Generate a random number between 0 and 1000");
                label.setText("The random number is: " + chatResponse.aiMessage().text());
            });

            this.content.add(label);
            this.content.add(shuffleButton);
        }

    }

}