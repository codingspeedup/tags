package io.github.codingspeedup.tags.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiFile;
import io.github.codingspeedup.tags.plugin.management.TagsMessageBundle;
import io.github.codingspeedup.tags.actions.TagsGroup;
import io.github.codingspeedup.tags.integration.groovy.ToolboxManagerService;
import io.github.codingspeedup.tags.utils.FileTypeModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TagsPromptIntention implements IntentionAction, HighPriorityAction, Iconable {

    private static final Icon MY_ICON = IconLoader.getIcon("/META-INF/processIcon.svg", TagsPromptIntention.class);

    @Override
    public @NotNull String getFamilyName() {
        return TagsMessageBundle.message("plugin.label");
    }

    @Override
    public @NotNull String getText() {
        return TagsMessageBundle.message("action.Execute.text");
    }

    @Override
    public Icon getIcon(int flags) {
        return MY_ICON;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        var description = TagsMessageBundle.message("action.Execute.description");
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
                        || ToolboxManagerService.isGroovy(file.getName())
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