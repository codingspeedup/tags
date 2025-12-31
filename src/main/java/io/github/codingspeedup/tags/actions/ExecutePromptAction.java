package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.engine.tags.ChatMdPromptHandler;
import io.github.codingspeedup.tags.engine.core.ChatMdUtl;
import io.github.codingspeedup.tags.engine.core.TagsResult;
import io.github.codingspeedup.tags.engine.tags.SelectionPromptHandler;
import io.github.codingspeedup.tags.engine.tags.TagsPromptHandler;
import io.github.codingspeedup.tags.plugin.TagsUtl;
import org.jetbrains.annotations.NotNull;

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
                    isAvailable = editor.getSelectionModel().hasSelection()
                            || ChatMdUtl.isChatMd(file.getName())
                            || InsertTagsActionBase.isAvailable(e);
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

        var editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            logger.error("No editor selected");
            return;
        }

        var editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (editorFile == null) {
            logger.error("No virtual file selected");
            return;
        }

        var editorFileName = editorFile.getName();
        var editorCaret = editor.getCaretModel().getPrimaryCaret();
        var editorSelection = editor.getSelectionModel().getSelectedText();

        var document = editor.getDocument();
        var documentText = document.getText();
        var documentOffset = editor.getCaretModel().getOffset();

        new Task.Backgroundable(project, "Processing " + editorFileName) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    Optional<TagsResult> tagsResult;

                    if (editorCaret.hasSelection()) {
                        tagsResult = new SelectionPromptHandler(editorFileName, editorSelection).process(indicator);
                    } else {
                        if (ChatMdUtl.isChatMd(editorFileName)) {
                            tagsResult = new ChatMdPromptHandler(documentText, documentOffset).process(indicator);
                        } else {
                            tagsResult = new TagsPromptHandler(editorFileName, documentText, documentOffset).process(indicator);
                        }
                    }

                    tagsResult.ifPresentOrElse(
                            gr -> ApplicationManager.getApplication().invokeLater(() -> {
                                switch (gr.getGateway()) {
                                    case CLIPBOARD -> TagsUtl.sendToClipboard(project, gr);
                                    case BUFFER -> TagsUtl.openReadOnlyBuffer(project, gr);
                                    case CONTENT -> TagsUtl.updateEditorDocument(project, editor, document, gr);
                                }
                            }, ModalityState.defaultModalityState()),
                            () -> logger.warn("Prompt execution produced no result")
                    );
                } catch (Exception e) {
                    logger.error("Error processing file", e);
                }
            }
        }.queue();
    }


}
