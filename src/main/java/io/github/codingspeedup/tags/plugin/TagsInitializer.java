package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import io.github.codingspeedup.tags.engine.chatmd.ChatMdUtl;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TagsInitializer implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        var logger = project.getService(TagsConsoleService.class);
        logger.info("Initializing T.A.G.S.+ for project `" + project.getName() + "' ...");

        try {
            var chatMdFolder = TagsUtl.getChatFolder(project).orElseThrow();
            ChatMdUtl.ensureDefaultChat(chatMdFolder);

            logger.info("T.A.G.S.+ initialized.");
        } catch (Exception e) {
            logger.error("T.A.G.S.+ failed to initialize.", e);
        }

        return Unit.INSTANCE;
    }

}
