package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.*;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptLibUtl;
import io.github.codingspeedup.tags.minions.ProjectPromptLibraryProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProcessAsGroup extends DefaultActionGroup {

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
        e.getPresentation().setEnabledAndVisible(isAvailable);
    }

    @Override
    @NotNull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return AnAction.EMPTY_ARRAY;
        }
        var builtinLib = new ProjectPromptLibraryProvider(e.getProject()).load().orElseThrow();
        return builtinLib.getPrompts().keySet().stream()
                .filter(promptId -> builtinLib.getVariables(promptId).size() == 1)
                .map(promptId ->
                        new ProcessAction(PromptLibUtl.PLUGIN_PROMPT_LIBRARY_NAME + "." + promptId, promptId))
                .toArray(AnAction[]::new);
    }

}
