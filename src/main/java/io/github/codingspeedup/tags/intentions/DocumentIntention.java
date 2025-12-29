package io.github.codingspeedup.tags.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class DocumentIntention extends BaseTagsIntention {

    @Override
    public @NotNull String getText() {
        return "Document selected code";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return hasSelection(editor);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        System.out.println("Executing " + getClass().getName());
    }

}
