package io.github.codingspeedup.tags.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiFile;
import io.github.codingspeedup.tags.MyMessageBundle;
import io.github.codingspeedup.tags.actions.TagsGroup;
import io.github.codingspeedup.tags.utils.FileTypeModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TagsGroupIntention extends BaseTagsIntention {

    @Override
    public @NotNull String getText() {
        return MyMessageBundle.message("plugin.label");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        var isAvailable = file != null && editor != null;
        if (isAvailable) {
            isAvailable = editor.getSelectionModel().hasSelection()
                    || TagsGroup.isChatMd(file.getName())
                    || FileTypeModel.of(file.getName()).isPresent();
        }
        return isAvailable;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        var actions = List.of(
                new ExecuteIntention(),
                new ExplainIntention(),
                new ReviewIntention(),
                new DocumentIntention()
        );

        actions = actions.stream().filter(action -> action.isAvailable(project, editor, file)).toList();

        if (actions.isEmpty()) {
            return;
        }

        if (actions.size() == 1) {
            actions.get(0).invoke(project, editor, file);
            return;
        }

        var step = new BaseListPopupStep<IntentionAction>("Select " + MyMessageBundle.message("plugin.label") + " Action", actions) {

            @Override
            public @NotNull String getTextFor(IntentionAction value) {
                return value.getText();
            }

            @Override
            public com.intellij.openapi.ui.popup.PopupStep<?> onChosen(IntentionAction selected, boolean finalChoice) {
                selected.invoke(project, editor, file);
                return FINAL_CHOICE;
            }

        };

        JBPopupFactory.getInstance()
                .createListPopup(step)
                .showInBestPositionFor(editor);
    }


}