package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.engine.ChatMdHandler;
import io.github.codingspeedup.tags.engine.GroovyScriptHandler;
import io.github.codingspeedup.tags.engine.SelectionHandler;
import io.github.codingspeedup.tags.engine.TagsActionHandler;
import io.github.codingspeedup.tags.integration.groovy.ToolboxManagerService;
import io.github.codingspeedup.tags.prompting.plib.PromptLibUtl;
import io.github.codingspeedup.tags.prompting.tags.TemplateModel;
import io.github.codingspeedup.tags.prompting.plib.PromptRef;
import io.github.codingspeedup.tags.engine.TagsResult;
import io.github.codingspeedup.tags.plugin.core.TagsUtl;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.codingspeedup.tags.plugin.core.TagsUtl.*;

public class ProcessAction extends AnAction {

    private final PromptRef promptRef;

    @SuppressWarnings("unused")
    public ProcessAction() {
        this.promptRef = new PromptRef(PromptLibUtl.PLUGIN_PROMPT_LIBRARY_NAME + ".");
    }

    public ProcessAction(String promptRef, String text) {
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
                            || ToolboxManagerService.isGroovy(file.getName())
                            || TemplateModel.of(file.getName()).isPresent();
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

        var editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            reportError(project, "No editor selected");
            return;
        }

        var editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (editorFile == null) {
            reportError(project, "No virtual file selected");
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
                        tagsResult = new SelectionHandler(editorFileName, editorSelection, promptRef).process(project, indicator);
                    } else {
                        if (TagsGroup.isChatMd(editorFileName)) {
                            tagsResult = new ChatMdHandler(editorFileName, documentText, documentOffset).process(project, indicator);
                        } else  if (ToolboxManagerService.isGroovy(editorFileName)) {
                            tagsResult = new GroovyScriptHandler(editorFileName, documentText).process(project, indicator);
                        } else {
                            tagsResult = new TagsActionHandler(editorFile.getParent(), editorFileName, documentText, documentOffset).process(project, indicator);
                        }
                    }

                    tagsResult.ifPresentOrElse(
                            tr -> ApplicationManager.getApplication().invokeLater(() -> {
                                switch (tr.getGateway()) {
                                    case CHAT -> TagsUtl.openChatBuffer(project, tr);
                                    case CLIPBOARD -> TagsUtl.sendToClipboard(project, tr);
                                    case CONTENT -> TagsUtl.updateEditorDocument(project, editor, document, tr);
                                    case INFO -> TagsUtl.reportInfo(project, tr.getContent());
                                    case WARN -> TagsUtl.reportWarning(project, tr.getContent());
                                    case ERROR -> TagsUtl.reportError(project, tr.getContent());
                                }
                            }, ModalityState.defaultModalityState()),
                            () -> reportInfo(project, "Prompt execution produced no result")
                    );

                } catch (Exception e) {
                    reportError(project, "Processing error", e);
                }
            }
        }.queue();
    }

}
