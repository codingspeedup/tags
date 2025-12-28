package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.codingspeedup.tags.engine.chatmd.ChatMd;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ExecutePromptAction extends AnAction {

    public static boolean isPromptable(VirtualFile file) {
        if (file == null) {
            return false;
        }
        var fileName = file.getName().toLowerCase(Locale.ROOT);
        return fileName.endsWith(ChatMd.CHAT_MD_EXTENSION);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var file = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE);
        e.getPresentation().setEnabledAndVisible(isPromptable(file));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }
    }

}
