package io.github.codingspeedup.tags.plugin.console;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import io.github.codingspeedup.tags.plugin.core.TagsMessageBundle;

@Service(Service.Level.PROJECT)
public final class TagsConsoleService extends ConsoleServiceBase {

    public static TagsConsoleService getInstance(Project project) {
        return project.getService(TagsConsoleService.class);
    }

    private final String prefix = TagsMessageBundle.message("plugin.label") + ": ";

    public void info(String message) {
        super.info(prefix + message);
    }

    public void warn(String message) {
        super.warn(prefix + message);
    }

    public void error(String message) {
        super.error(prefix + message);
    }

}
