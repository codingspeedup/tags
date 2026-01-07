package io.github.codingspeedup.tags.plugin.management;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import io.github.codingspeedup.tags.MyMessageBundle;
import io.github.codingspeedup.tags.utils.TagsUtl;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.github.codingspeedup.tags.utils.TagsUtl.reportError;

public class TagsInitializer implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        var logger = TagsUtl.getLogger(project);
        logger.info(String.format("%s: Initializing plugin for project `%s' ...",
                MyMessageBundle.message("plugin.label"),
                project.getName()));
        try {
            TagsUtl.resolvePromptLibrary(project).orElseThrow();
            TagsUtl.saveAllDocuments();
            logger.info(String.format("%s: Plugin initialized.",
                    MyMessageBundle.message("plugin.label")));
        } catch (Exception e) {
            reportError(project, "Plugin initialization failure", e);
        }
        return Unit.INSTANCE;
    }

}
