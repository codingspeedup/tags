package io.github.codingspeedup.tags.plugin.console;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class GroovyConsoleService extends ConsoleServiceBase {

    public static GroovyConsoleService getInstance(Project project) {
        return project.getService(GroovyConsoleService.class);
    }

}
