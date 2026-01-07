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
        var contentFactory = ContentFactory.getInstance();
        var contentManager = toolWindow.getContentManager();

        // 1. Setup Plugin Console Tab
        var pluginConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        TagsConsoleService.getInstance(project).setConsoleView(pluginConsoleView);
        var pluginContent = contentFactory.createContent(pluginConsoleView.getComponent(), "Plugin", false);
        contentManager.addContent(pluginContent);

        // 2. Setup Groovy Console Tab
        var groovyConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        GroovyConsoleService.getInstance(project).setConsoleView(groovyConsoleView);
        var groovyContent = contentFactory.createContent(groovyConsoleView.getComponent(), "Groovy", false);
        contentManager.addContent(groovyContent);
    }

}