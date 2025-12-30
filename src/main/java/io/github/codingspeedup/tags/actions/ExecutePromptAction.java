package io.github.codingspeedup.tags.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import io.github.codingspeedup.tags.engine.chatmd.ChatMdHandler;
import io.github.codingspeedup.tags.engine.chatmd.ChatMdUtl;
import io.github.codingspeedup.tags.engine.core.GenerationResponse;
import io.github.codingspeedup.tags.engine.selection.SelectionHandler;
import io.github.codingspeedup.tags.plugin.TagsUtl;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.Optional;

public class ExecutePromptAction extends AnAction {

    public static boolean isAvailable(@NotNull AnActionEvent e) {
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

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }
        var logger = TagsUtl.getLogger(project);

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
        var fileSel = editor.getSelectionModel().getSelectedText();
        var fileText = fileSel == null ? document.getText() : null;
        var fileOffset = editor.getCaretModel().getOffset();

        new Task.Backgroundable(project, "Processing " + fileName) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                Optional<GenerationResponse> result = Optional.empty();
                try {
                    if (fileSel != null) {
                        result = new SelectionHandler(fileName, fileSel).process(indicator);
                    } else {
                        if (ChatMdUtl.isChatMd(fileName)) {
                            result = new ChatMdHandler(fileText, fileOffset).process(indicator);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing file", e);
                }

                result.ifPresentOrElse(
                        gr -> ApplicationManager.getApplication().invokeLater(() -> {
                            switch (gr.getOutputChannel()) {
                                case CLIPBOARD -> sendToClipboard(project, gr);
                                case MD_BUFFER -> openReadOnlyBuffer(project, gr);
                                case REPLACE_FILE -> updateCurrentBuffer(project, editor, document, gr);
                            }
                        }, ModalityState.defaultModalityState()),
                        () -> logger.warn("Prompt execution produced no result")
                );
            }
        }.queue();
    }

    private void sendToClipboard(Project project, GenerationResponse gr) {
        CopyPasteManager.getInstance().setContents(new StringSelection(gr.getGeneratedContent()));
        NotificationGroupManager.getInstance()
                .getNotificationGroup("GenerationGroup")
                .createNotification(
                        "Copied to Clipboard",
                        "Content successfully sent to system clipboard.",
                        NotificationType.INFORMATION
                )
                .notify(project);
    }

    private void openReadOnlyBuffer(Project project, GenerationResponse gr) {
        var lvf = new LightVirtualFile(
                gr.getBufferName(),
                FileTypeManager.getInstance().getFileTypeByExtension(FilenameUtils.getExtension(gr.getBufferName())),
                gr.getGeneratedContent()
        );
        lvf.setWritable(false);
        var fileEditors = FileEditorManager.getInstance(project).openFile(lvf, true);
        if (fileEditors.length > 0 && fileEditors[0] instanceof TextEditor textEditor) {
            var editor = textEditor.getEditor();
            editor.getCaretModel().moveToOffset(gr.getContentOffset());
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
    }

    private static void updateCurrentBuffer(Project project, Editor editor, Document document, GenerationResponse gr) {
        WriteCommandAction.runWriteCommandAction(project, () -> document.setText(gr.getGeneratedContent()));
        var caretModel = editor.getCaretModel();
        caretModel.moveToOffset(gr.getContentOffset());
        var scrollingModel = editor.getScrollingModel();
        scrollingModel.scrollToCaret(ScrollType.CENTER);
    }

}
