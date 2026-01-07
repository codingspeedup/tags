package io.github.codingspeedup.tags.plugin.console;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ConsoleWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        var consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        TagsConsoleService.getInstance(project).setConsoleView(consoleView);
        var contentFactory = ContentFactory.getInstance();
        var content = contentFactory.createContent(consoleView.getComponent(), "Plugin", false);
        toolWindow.getContentManager().addContent(content);
    }

}