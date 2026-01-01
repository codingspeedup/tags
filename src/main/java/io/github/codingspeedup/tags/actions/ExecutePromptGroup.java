package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExecutePromptGroup extends DefaultActionGroup {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(ExecutePromptAction.isAvailable(e));
    }

    @Override
    @NotNull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) return AnAction.EMPTY_ARRAY;

        var actions = new AnAction[3];
        for (int i = 0; i < 3; i++) {
            var actionId = ExecutePromptAction.CORE_ID + "-" + i;
            actions[i] = new ExecutePromptAction(actionId, "Prompt As " + (i + 1));
        }

        return actions;
    }

}
