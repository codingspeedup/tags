package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.utils.*;
import io.github.codingspeedup.tags.engine.ChatMdPromptHandler;
import io.github.codingspeedup.tags.engine.SelectionPromptHandler;
import io.github.codingspeedup.tags.engine.TagsPromptHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ExecutePromptAction extends AnAction {

    private final PromptRef promptRef;

    @SuppressWarnings("unused")
    public ExecutePromptAction() {
        this.promptRef = new PromptRef(TagsUtl.PLUGIN_PROMPT_LIBRARY_REF + ".");
    }

    public ExecutePromptAction(String promptRef, String text) {
        this.promptRef = new PromptRef(promptRef);
        getTemplatePresentation().setText(text);
    }

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
                            || TagsGroup.isChatMd(file.getName())
                            || FileTypeModel.of(file.getName()).isPresent();
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

                    if (editorSelection != null) {
                        tagsResult = new SelectionPromptHandler(editorFileName, editorSelection, promptRef).process(project, indicator);
                    } else {
                        if (TagsGroup.isChatMd(editorFileName)) {
                            tagsResult = new ChatMdPromptHandler(documentText, documentOffset).process(project, indicator);
                        } else {
                            tagsResult = new TagsPromptHandler(editorFileName, documentText, documentOffset).process(project, indicator);
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
