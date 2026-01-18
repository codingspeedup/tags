package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptLibUtl;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptRef;
import io.github.codingspeedup.tags.handlers.*;
import io.github.codingspeedup.tags.minions.PluginUtl;
import io.github.codingspeedup.tags.minions.ToolboxManagerService;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;
import static io.github.codingspeedup.tags.minions.PluginUtl.reportInfo;

public class ProcessAction extends TagsActionBase {

    private final PromptRef promptRef;

    @SuppressWarnings("unused")
    public ProcessAction() {
        this.promptRef = new PromptRef(PromptLibUtl.PLUGIN_PROMPT_LIBRARY_NAME + ".");
    }

    public ProcessAction(String promptRef, String text) {
        this.promptRef = new PromptRef(promptRef);
        getTemplatePresentation().setText(text);
    }

    public static boolean isProcessable(@NotNull AnActionEvent e) {
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
                            || BufferModel.of(file.getName()).isPresent();
                }
            }
        }
        return isAvailable;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isProcessable(e));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        extractDocumentActionContext(e).ifPresent(ac -> {
            var editorSelection = ac.editor().getSelectionModel().getSelectedText();
            var documentOffset = ac.editor().getCaretModel().getOffset();

            new Task.Backgroundable(ac.project(), "Processing " + ac.fileName()) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    try {
                        Optional<TagsResult> tagsResult;

                        if (editorSelection != null) {
                            tagsResult = new SelectionHandler(ac.fileName(), editorSelection, promptRef).process(ac.project(), indicator);
                        } else {
                            if (TagsGroup.isChatMd(ac.fileName())) {
                                tagsResult = new ChatMdHandler(ac.file(), ac.documentText(), documentOffset).process(ac.project(), indicator);
                            } else if (ToolboxManagerService.isGroovy(ac.fileName())) {
                                tagsResult = new GroovyScriptHandler(ac.fileName(), ac.documentText()).process(ac.project(), indicator);
                            } else {
                                tagsResult = new TagsActionHandler(ac.file(), ac.documentText(), documentOffset).process(ac.project(), indicator);
                            }
                        }

                        tagsResult.ifPresentOrElse(
                                tr -> ApplicationManager.getApplication().invokeLater(() -> {
                                    switch (tr.getGateway()) {
                                        case CHAT -> PluginUtl.openChatBuffer(ac.project(), tr);
                                        case CLIPBOARD -> PluginUtl.sendToClipboard(ac.project(), tr);
                                        case CONTENT ->
                                                PluginUtl.updateEditorDocument(ac.project(), ac.editor(), ac.document(), tr);
                                        case INFO -> PluginUtl.reportInfo(ac.project(), tr.getContent());
                                        case WARN -> PluginUtl.reportWarning(ac.project(), tr.getContent());
                                        case ERROR -> PluginUtl.reportError(ac.project(), tr.getContent());
                                    }
                                }, ModalityState.defaultModalityState()),
                                () -> reportInfo(ac.project(), "Prompt execution produced no result")
                        );

                    } catch (Exception e) {
                        reportError(ac.project(), "Processing error", e);
                    }
                }
            }.queue();
        });

    }

}
