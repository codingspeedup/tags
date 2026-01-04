package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.MyMessageBundle;
import io.github.codingspeedup.tags.engine.ChatMdExecuteHandler;
import io.github.codingspeedup.tags.engine.SelectionPromptHandler;
import io.github.codingspeedup.tags.engine.TagsPromptHandler;
import io.github.codingspeedup.tags.utils.FileTypeModel;
import io.github.codingspeedup.tags.utils.PromptDesc;
import io.github.codingspeedup.tags.utils.TagsResult;
import io.github.codingspeedup.tags.utils.TagsUtl;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ExecuteAction extends AnAction {

    private final PromptDesc promptDesc;

    @SuppressWarnings("unused")
    public ExecuteAction() {
        this.promptDesc = new PromptDesc(TagsUtl.PLUGIN_PROMPT_LIBRARY_REF + ".");
    }

    public ExecuteAction(String promptRef, String text) {
        this.promptDesc = new PromptDesc(promptRef);
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

        e.getPresentation().setEnabledAndVisible(isAvailable(e));
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
            logger.error(String.format("%s: No editor selected",
                    MyMessageBundle.message("plugin.label")));
            return;
        }

        var editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (editorFile == null) {
            logger.error(String.format("%s: No virtual file selected",
                    MyMessageBundle.message("plugin.label")));
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
                        tagsResult = new SelectionPromptHandler(editorFileName, editorSelection, promptDesc).process(project, indicator);
                    } else {
                        if (TagsGroup.isChatMd(editorFileName)) {
                            tagsResult = new ChatMdExecuteHandler(documentText, documentOffset).process(project, indicator);
                        } else {
                            tagsResult = new TagsPromptHandler(editorFileName, documentText, documentOffset).process(project, indicator);
                        }
                    }

                    tagsResult.ifPresentOrElse(
                            tr -> ApplicationManager.getApplication().invokeLater(() -> {
                                switch (tr.getGateway()) {
                                    case CHAT -> TagsUtl.openChatBuffer(project, tr);
                                    case CLIPBOARD -> TagsUtl.sendToClipboard(project, tr);
                                    case CONTENT -> TagsUtl.updateEditorDocument(project, editor, document, tr);
                                }
                            }, ModalityState.defaultModalityState()),
                            () -> logger.warn(String.format("%s: Prompt execution produced no result",
                                    MyMessageBundle.message("plugin.label")))
                    );
                } catch (Exception e) {
                    logger.error(String.format("%s: Error processing file",
                            MyMessageBundle.message("plugin.label")), e);
                }
            }
        }.queue();
    }


}
