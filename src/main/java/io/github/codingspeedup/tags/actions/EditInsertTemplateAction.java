package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.handlers.TagsEditHandler;
import io.github.codingspeedup.tags.minions.PluginUtl;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptLibUtl;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptRef;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import org.jetbrains.annotations.NotNull;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;

public class EditInsertTemplateAction extends EditActionBase {

    private final PromptRef promptRef;

    @SuppressWarnings("unused")
    public EditInsertTemplateAction() {
        this.promptRef = new PromptRef(String.format("%s.Explain", PromptLibUtl.PLUGIN_PROMPT_LIBRARY_NAME));
    }

    public EditInsertTemplateAction(String text, PromptRef promptRef) {
        this.promptRef = promptRef;
        getTemplatePresentation().setText(text);
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

        var ftModel = BufferModel.of(editorFileName).orElse(null);
        if (ftModel == null) {
            reportError(project, String.format("Unrecognized file model for `%s'", editorFileName));
            return;
        }

        var document = editor.getDocument();
        var documentText = document.getText();
        var documentOffset = editor.getCaretModel().getOffset();

        new Task.Backgroundable(project, "Marking new section in " + editorFileName) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    var tagsEditor = new TagsEditHandler(ftModel, documentText);
                    var gr = tagsEditor.insertNewTemplate(project, documentOffset, promptRef);
                    PluginUtl.updateEditorDocument(project, editor, document, gr);
                } catch (Exception e) {
                    reportError(project, "Error processing file", e);
                }
            }
        }.queue();
    }

}
