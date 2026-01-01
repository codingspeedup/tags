package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.utils.FileTypeModel;
import io.github.codingspeedup.tags.engine.TagsEditor;
import io.github.codingspeedup.tags.utils.TagsUtl;
import org.jetbrains.annotations.NotNull;

public class InsertTagsTemplateAction extends EditTagsActionBase {

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

        var ftModel = FileTypeModel.of(editorFileName).orElse(null);
        if (ftModel == null) {
            logger.error("Unrecognized file model for `" + editorFileName + "'");
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
                    var tagsEditor = new TagsEditor(ftModel, documentText);
                    var gr = tagsEditor.insertNewTemplate(documentOffset);
                    TagsUtl.updateEditorDocument(project, editor, document, gr);
                } catch (Exception e) {
                    logger.error("Error processing file", e);
                }
            }
        }.queue();
    }

}
