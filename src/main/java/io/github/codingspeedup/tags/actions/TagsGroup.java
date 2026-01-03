package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import io.github.codingspeedup.tags.utils.ChatUtl;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class TagsGroup extends DefaultActionGroup {

    public static boolean isChatMd(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(ChatUtl.CHAT_MD_EXTENSION);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var place = e.getPlace();
        if (ActionPlaces.EDITOR_POPUP.equals(place)) {
            e.getPresentation().setEnabledAndVisible(ExecutePromptAction.isAvailable(e));
        }
    }

}
