package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import io.github.codingspeedup.tags.engine.core.FileTypeModel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public abstract class EditTagsActionBase extends AnAction {

    public EditTagsActionBase(String text) {
        super(text);
    }

    public static boolean isAvailable(@NotNull AnActionEvent e) {
        var isAvailable = e.getProject() != null;
        if (isAvailable) {
            var file = e.getData(CommonDataKeys.VIRTUAL_FILE);
            isAvailable = file != null;
            if (isAvailable) {
                isAvailable = FileTypeModel.of(file.getName()).isPresent();
            }
        }
        return isAvailable;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(isAvailable(e));
    }

}
