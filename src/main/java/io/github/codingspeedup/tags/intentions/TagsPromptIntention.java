package io.github.codingspeedup.tags.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import io.github.codingspeedup.tags.MyMessageBundle;
import io.github.codingspeedup.tags.actions.TagsGroup;
import io.github.codingspeedup.tags.utils.FileTypeModel;
import org.jetbrains.annotations.NotNull;

public class TagsPromptIntention implements IntentionAction, HighPriorityAction {

    @Override
    public @NotNull String getFamilyName() {
        return MyMessageBundle.message("plugin.label");
    }

    @Override
    public @NotNull String getText() {
        return MyMessageBundle.message("action.Execute.text");
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        var description = MyMessageBundle.message("action.Execute.description");
        return new IntentionPreviewInfo.Html(description);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        var isAvailable = file != null && editor != null;
        if (isAvailable) {
            isAvailable = !editor.getSelectionModel().hasSelection();
            if (isAvailable) {
                isAvailable = TagsGroup.isChatMd(file.getName())
                        || TagsGroup.isGroovy(file.getName())
                        || FileTypeModel.of(file.getName()).isPresent();
            }
        }
        return isAvailable;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        var actionManager = ActionManager.getInstance();
        var action = actionManager.getAction("Execute");
        if (action != null) {
            var context = ((com.intellij.openapi.editor.ex.EditorEx) editor).getDataContext();
            var event = AnActionEvent.createFromDataContext(getClass().getName(), null, context);
            action.actionPerformed(event);
        }
    }

}