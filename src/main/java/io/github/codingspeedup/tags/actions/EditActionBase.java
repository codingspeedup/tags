package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;

@NoArgsConstructor
public abstract class EditActionBase extends TagsActionBase {

    public static boolean isAvailable(@NotNull AnActionEvent e) {
        var isAvailable = e.getProject() != null;
        if (isAvailable) {
            var file = e.getData(CommonDataKeys.VIRTUAL_FILE);
            isAvailable = file != null;
            if (isAvailable) {
                isAvailable = BufferModel.of(file.getName()).isPresent();
            }
        }
        return isAvailable;
    }

    protected record EditActionContext(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull VirtualFile file,
            @NotNull String fileName,
            @NotNull BufferModel model,
            @NotNull Document document,
            @NotNull String documentText
    ) {
    }

    protected static Optional<EditActionContext> extractEditActionContext(@NotNull AnActionEvent e) {
        var optAc = extractDocumentActionContext(e);
        if (optAc.isPresent()) {
            var ac = optAc.get();
            var ftModel = BufferModel.of(ac.fileName()).orElse(null);
            if (ftModel == null) {
                reportError(ac.project(), String.format("Unrecognized file model for `%s'", ac.fileName()));
            } else {
                return Optional.of(new EditActionContext(
                        ac.project(), ac.editor(), ac.file(), ac.fileName(), ftModel, ac.document(), ac.documentText()
                ));
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isAvailable(e));
    }

}
