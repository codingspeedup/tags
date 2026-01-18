package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;

public abstract class TagsActionBase extends AnAction {

    protected record FolderActionContext(
            @NotNull Project project,
            @NotNull VirtualFile folder
    ) {
    }

    protected static Optional<FolderActionContext> extractFolderActionContext(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return Optional.empty();
        }

        var location = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (location != null && !location.isDirectory()) {
            location = location.getParent();
        }
        if (location == null) {
            return Optional.empty();
        }

        return Optional.of(new FolderActionContext(project, location));
    }

    protected record DocumentActionContext(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull VirtualFile file,
            @NotNull String fileName,
            @NotNull Document document,
            @NotNull String documentText
    ) {
    }

    protected static Optional<DocumentActionContext> extractDocumentActionContext(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return Optional.empty();
        }

        var editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            reportError(project, "No editor selected");
            return Optional.empty();
        }

        var editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (editorFile == null) {
            reportError(project, "No virtual file selected");
            return Optional.empty();
        }

        var fileName = editorFile.getName();
        var document = editor.getDocument();

        return Optional.of(new DocumentActionContext(
                project, editor, editorFile, fileName, document, document.getText()
        ));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
