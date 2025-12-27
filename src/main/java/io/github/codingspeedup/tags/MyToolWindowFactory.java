package io.github.codingspeedup.tags;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Random;

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
        private final JBPanel<JBPanel<?>> content;
        private final JBLabel label;
        private final Random random = new Random();

        public MyToolWindow() {
            this.content = new JBPanel<>();
            this.label = new JBLabel("The random number is: ?");

            JButton shuffleButton = new JButton("Shuffle");
            shuffleButton.addActionListener(e -> {
                int nextInt = random.nextInt(1000);
                label.setText("The random number is: " + nextInt);
            });

            this.content.add(label);
            this.content.add(shuffleButton);
        }

        public JBPanel<JBPanel<?>> getContent() {
            return content;
        }
    }
}