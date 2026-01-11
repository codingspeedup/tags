package io.github.codingspeedup.tags.minions;

import com.intellij.openapi.project.Project;
import io.github.codingspeedup.tags.ai.boundary.ToolboxSupport;
import io.github.codingspeedup.tags.plugin.console.TagsConsoleService;

public class ProjectToolboxSupport implements ToolboxSupport {

    private final ToolboxManagerService  toolboxManagerService;
    private final TagsConsoleService  tagsConsoleService;

    public ProjectToolboxSupport(Project project) {
        this.toolboxManagerService = ToolboxManagerService.getInstance(project);
        this.tagsConsoleService = TagsConsoleService.getInstance(project);
    }

    @Override
    public ClassLoader getToolboxClassLoader() {
        toolboxManagerService.reloadIfChanged();
        return toolboxManagerService.getActiveLoader();
    }

    @Override
    public void warn(String message) {
        tagsConsoleService.warn(message);
    }

}
