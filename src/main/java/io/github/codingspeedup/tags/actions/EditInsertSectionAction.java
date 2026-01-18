package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.handlers.TagsEditHandler;
import io.github.codingspeedup.tags.minions.PluginUtl;
import org.jetbrains.annotations.NotNull;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;

public class EditInsertSectionAction extends EditActionBase {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        extractEditActionContext(e).ifPresent(ac -> {
            var editorCaret = ac.editor().getCaretModel().getPrimaryCaret();
            var fromOffset = editorCaret.hasSelection() ? editorCaret.getSelectionStart() : ac.editor().getCaretModel().getOffset();
            var toOffset = editorCaret.hasSelection() ? editorCaret.getSelectionEnd() : fromOffset;

            new Task.Backgroundable(ac.project(), "Marking new section in " + ac.fileName()) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    try {
                        var tagsEditor = new TagsEditHandler(ac.model(), ac.documentText());
                        var tagsResult = tagsEditor.insertNewSection(fromOffset, toOffset);
                        PluginUtl.updateEditorDocument(ac.project(), ac.editor(), ac.document(), tagsResult);
                    } catch (Exception e) {
                        reportError(ac.project(), "Error processing file", e);
                    }
                }
            }.queue();
        });
    }

}
