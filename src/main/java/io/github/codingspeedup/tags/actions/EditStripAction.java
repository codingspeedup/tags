package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.handlers.TagsEditHandler;
import io.github.codingspeedup.tags.minions.PluginUtl;
import org.jetbrains.annotations.NotNull;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;

public class EditStripAction extends EditActionBase {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        extractEditActionContext(e).ifPresent(ac -> {
            var documentOffset = ac.editor().getCaretModel().getOffset();

            new Task.Backgroundable(ac.project(), "Marking new section in " + ac.fileName()) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    try {
                        var tagsEditor = new TagsEditHandler(ac.model(), ac.documentText());
                        var tagsResult = tagsEditor.stripTags(documentOffset);
                        tagsResult.ifPresent(result ->
                                PluginUtl.updateEditorDocument(ac.project(), ac.editor(), ac.document(), result));
                    } catch (Exception e) {
                        reportError(ac.project(), "Error processing file", e);
                    }
                }
            }.queue();
        });
    }

}
