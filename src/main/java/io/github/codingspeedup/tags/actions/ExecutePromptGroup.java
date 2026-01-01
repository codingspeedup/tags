package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.*;
import io.github.codingspeedup.tags.utils.PromptLibrary;
import io.github.codingspeedup.tags.utils.TagsUtl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExecutePromptGroup extends DefaultActionGroup {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var isAvailable = e.getProject() != null;
        if (isAvailable) {
            var file = e.getData(CommonDataKeys.VIRTUAL_FILE);
            isAvailable = file != null;
            if (isAvailable) {
                var editor = e.getData(CommonDataKeys.EDITOR);
                isAvailable = editor != null;
                if (isAvailable) {
                    isAvailable = editor.getSelectionModel().hasSelection();
                }
            }
        }
        e.getPresentation().setEnabled(isAvailable);
    }

    @Override
    @NotNull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return AnAction.EMPTY_ARRAY;
        }
        var builtinLib = PromptLibrary.of(e.getProject());
        return builtinLib.getPrompts().keySet().stream()
                .filter(promptId -> builtinLib.getVariables(promptId).size() == 1)
                .map(promptId ->
                        new ExecutePromptAction(TagsUtl.PLUGIN_PROMPT_LIBRARY_REF + "." + promptId, promptId))
                .toArray(AnAction[]::new);
    }

}
