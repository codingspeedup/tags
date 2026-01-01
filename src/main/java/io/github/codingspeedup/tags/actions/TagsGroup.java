package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import io.github.codingspeedup.tags.engine.ChatMdPromptHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class TagsGroup extends DefaultActionGroup {

    public static boolean isChatMd(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(ChatMdPromptHandler.CHAT_MD_EXTENSION);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(ExecutePromptAction.isAvailable(e));
    }

}
