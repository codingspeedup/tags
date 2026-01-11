package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.engine.TagsEditHandler;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import io.github.codingspeedup.tags.plugin.core.TagsUtl;
import org.jetbrains.annotations.NotNull;

import static io.github.codingspeedup.tags.plugin.core.TagsUtl.reportError;

public class EditInsertSectionAction extends EditActionBase {

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
        var editorCaret = editor.getCaretModel().getPrimaryCaret();

        var ftModel = BufferModel.of(editorFileName).orElse(null);
        if (ftModel == null) {
            reportError(project, String.format("Unrecognized file model for `%s'", editorFileName));
            return;
        }

        var document = editor.getDocument();
        var documentText = document.getText();

        var fromOffset = editorCaret.hasSelection() ? editorCaret.getSelectionStart() : editor.getCaretModel().getOffset();
        var toOffset = editorCaret.hasSelection() ? editorCaret.getSelectionEnd() : fromOffset;

        new Task.Backgroundable(project, "Marking new section in " + editorFileName) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    var tagsEditor = new TagsEditHandler(ftModel, documentText);
                    var tagsResult = tagsEditor.insertNewSection(fromOffset, toOffset);
                    TagsUtl.updateEditorDocument(project, editor, document, tagsResult);
                } catch (Exception e) {
                    reportError(project, "Error processing file", e);
                }
            }
        }.queue();
    }

}
