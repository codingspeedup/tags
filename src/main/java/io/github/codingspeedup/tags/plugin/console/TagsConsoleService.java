package io.github.codingspeedup.tags.plugin.console;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class TagsConsoleService extends ConsoleServiceBase {

    public static TagsConsoleService getInstance(Project project) {
        return project.getService(TagsConsoleService.class);
    }

}
