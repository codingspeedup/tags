package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import io.github.codingspeedup.tags.engine.chatmd.ChatMdUtl;
import io.github.codingspeedup.tags.engine.chatmd.ChatMdHandler;
import io.github.codingspeedup.tags.engine.selection.SelectionHandler;
import io.github.codingspeedup.tags.plugin.TagsConsoleService;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ExecutePromptAction extends AnAction {


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
                    isAvailable = editor.getSelectionModel().hasSelection() || ChatMdUtl.isChatMd(file.getName());
                }
            }
        }
        e.getPresentation().setEnabledAndVisible(isAvailable);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }
        var logger = project.getService(TagsConsoleService.class);

        var editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (editorFile == null) {
            logger.error("No virtual file selected");
            return;
        }

        var fileName = editorFile.getName();

        var editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            logger.error("No editor selected");
            return;
        }

        var document = editor.getDocument();
        var fileText = document.getText();
        var filePos = editor.getCaretModel().getPrimaryCaret().getLogicalPosition();
        var fileSel = editor.getSelectionModel().getSelectedText();

        new Task.Backgroundable(project, "Processing " + fileName) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                Optional<String> result = Optional.empty();

                try {
                    if (fileSel != null) {
                        result = new SelectionHandler(fileSel).process(indicator);
                    } else if (ChatMdUtl.isChatMd(fileName)) {
                        result = new ChatMdHandler(fileText, filePos.line).process(indicator);
                    }
                } catch (Exception e) {
                    logger.error("Error processing file", e);
                }

                result.ifPresent(newContent -> ApplicationManager.getApplication().invokeLater(() -> {
                    if (fileSel != null) {
                        openReadOnlyBuffer(project, fileName, newContent);
                    } else {
                        updateCurrentBuffer(newContent, project, document);
                    }
                }, ModalityState.defaultModalityState()));
            }
        }.queue();
    }

    private static void updateCurrentBuffer(String newContent, Project project, Document document) {
        WriteCommandAction.runWriteCommandAction(project, () -> document.setText(newContent));
    }

    private void openReadOnlyBuffer(Project project, String baseName, String content) {
        var lvf = new LightVirtualFile(
                baseName + ".result.md",
                FileTypeManager.getInstance().getFileTypeByExtension("md"),
                content
        );
        lvf.setWritable(false);
        FileEditorManager.getInstance(project).openFile(lvf, true);
    }

}
