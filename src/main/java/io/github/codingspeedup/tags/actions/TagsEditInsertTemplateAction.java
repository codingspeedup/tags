package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.MyMessageBundle;
import io.github.codingspeedup.tags.utils.FileTypeModel;
import io.github.codingspeedup.tags.engine.TagsEditHandler;
import io.github.codingspeedup.tags.utils.PromptDesc;
import io.github.codingspeedup.tags.utils.TagsUtl;
import org.jetbrains.annotations.NotNull;

public class TagsEditInsertTemplateAction extends TagsEditActionBase {

    private final PromptDesc promptDesc;

    @SuppressWarnings("unused")
    public TagsEditInsertTemplateAction() {
        this.promptDesc = new PromptDesc(String.format("%s.Explain", TagsUtl.PLUGIN_PROMPT_LIBRARY_REF));
    }

    public TagsEditInsertTemplateAction(String text, PromptDesc promptDesc) {
        this.promptDesc = promptDesc;
        getTemplatePresentation().setText(text);
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

        var ftModel = FileTypeModel.of(editorFileName).orElse(null);
        if (ftModel == null) {
            logger.error(String.format("%s: Unrecognized file model for `%s'",
                    MyMessageBundle.message("plugin.label"),
                    editorFileName));
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
                    var gr = tagsEditor.insertNewTemplate(project, documentOffset, promptDesc);
                    TagsUtl.updateEditorDocument(project, editor, document, gr);
                } catch (Exception e) {
                    logger.error(String.format("%s: Error processing file",
                            MyMessageBundle.message("plugin.label")), e);
                }
            }
        }.queue();
    }

}
