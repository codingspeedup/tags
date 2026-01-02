package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import io.github.codingspeedup.tags.MyMessageBundle;
import io.github.codingspeedup.tags.utils.TagsUtl;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TagsInitializer implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        var logger = TagsUtl.getLogger(project);
        logger.info("Initializing " + MyMessageBundle.message("plugin.label") + " plugin for project `" + project.getName() + "' ...");

        try {
            TagsUtl.resolvePromptLibrary(project).orElseThrow();
            TagsUtl.saveAllDocuments();
            logger.info(MyMessageBundle.message("plugin.label") + " plugin initialized.");
        } catch (Exception e) {
            logger.error(MyMessageBundle.message("plugin.label") + " plugin failed to initialize!", e);
        }

        return Unit.INSTANCE;
    }

}
