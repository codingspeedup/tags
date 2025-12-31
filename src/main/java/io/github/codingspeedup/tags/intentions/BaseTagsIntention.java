package io.github.codingspeedup.tags.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import io.github.codingspeedup.tags.MyMessageBundle;
import io.github.codingspeedup.tags.engine.core.ChatMdUtl;
import org.jetbrains.annotations.NotNull;

public abstract class BaseTagsIntention implements IntentionAction, HighPriorityAction {

    @Override
    public @NotNull String getFamilyName() {
        return MyMessageBundle.message("plugin.label");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        var isAvailable = file != null && editor != null;
        if (isAvailable) {
            isAvailable = editor.getSelectionModel().hasSelection() || ChatMdUtl.isChatMd(file.getName());
        }
        return isAvailable;
    }

    protected boolean hasSelection(Editor editor) {
        return editor != null && editor.getSelectionModel().hasSelection();
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

}
