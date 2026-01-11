package io.github.codingspeedup.tags.plugin.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import io.github.codingspeedup.tags.minions.PluginUtl;
import io.github.codingspeedup.tags.plugin.console.TagsConsoleService;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;

public class TagsInitializer implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        var logger = TagsConsoleService.getInstance(project);
        logger.info(String.format("%s: Initializing plugin for project `%s' ...",
                TagsMessageBundle.message("plugin.label"),
                project.getName()));
        try {
            PluginUtl.resolveChatFolder(project).orElseThrow();
            PluginUtl.resolvePromptLibrary(project).orElseThrow();
            PluginUtl.resolveToolboxFolder(project).orElseThrow();
            PluginUtl.saveAllDocuments();
            logger.info(String.format("%s: Plugin initialized.",
                    TagsMessageBundle.message("plugin.label")));
        } catch (Exception e) {
            reportError(project, "Plugin initialization failure", e);
        }
        return Unit.INSTANCE;
    }

}
