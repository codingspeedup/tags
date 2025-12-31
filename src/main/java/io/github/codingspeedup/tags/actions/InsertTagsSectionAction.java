package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.engine.core.FileTypeModel;
import io.github.codingspeedup.tags.engine.tags.TagsSectionEditor;
import io.github.codingspeedup.tags.plugin.TagsUtl;
import org.jetbrains.annotations.NotNull;

public class InsertTagsSectionAction extends InsertTagsActionBase {

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

        var ftModel = FileTypeModel.of(editorFileName).orElse(null);
        if (ftModel == null) {
            logger.error("Unrecognized file model for `" + editorFileName + "'");
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
                    var sectionEditor = new TagsSectionEditor(ftModel, documentText);
                    var gr = sectionEditor.insertNewSection(fromOffset, toOffset);
                    TagsUtl.updateEditorDocument(project, editor, document, gr);
                } catch (Exception e) {
                    logger.error("Error processing file", e);
                }
            }
        }.queue();
    }

}
