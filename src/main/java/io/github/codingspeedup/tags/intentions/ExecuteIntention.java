package io.github.codingspeedup.tags.intentions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class ExecuteIntention extends BaseTagsIntention {

    @Override
    public @NotNull String getText() {
        return "Execute as prompt";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        var actionManager = ActionManager.getInstance();
        var action = actionManager.getAction("SendToLLM");
        if (action != null) {
            var context = ((com.intellij.openapi.editor.ex.EditorEx) editor).getDataContext();
            var event = AnActionEvent.createFromDataContext(getClass().getName(), null, context);
            action.actionPerformed(event);
        }
    }

}
