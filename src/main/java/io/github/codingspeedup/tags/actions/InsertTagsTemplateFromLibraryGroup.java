package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsertTagsTemplateFromLibraryGroup extends DefaultActionGroup {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(EditTagsActionBase.isAvailable(e));
    }

    @Override
    @NotNull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return AnAction.EMPTY_ARRAY;
        }

        new InsertTagsTemplateFromLibrarySubGroup("Lorem");

        var actions = new AnAction[3];
        for (int i = 0; i < 3; i++) {
            var actionId = "DynamicAction_" + i;
            actions[i] = new InsertTagsTemplateFromLibraryAction(actionId, "Acțiune Dinamică " + (i + 1));
        }

        return actions;
    }

}
