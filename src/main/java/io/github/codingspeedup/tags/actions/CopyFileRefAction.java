package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.vfs.VfsUtil;
import io.github.codingspeedup.tags.minions.PluginUtl;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.stream.Collectors;

import static io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel.*;

public class CopyFileRefAction extends TagsActionBase {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        extractDocumentActionContext(e).ifPresent(ac -> {
            var relativePath = VfsUtil.getRelativePath(ac.file(), PluginUtl.resolveProjectRoot(ac.project()));
            if (relativePath != null) {
                var ref = new StringBuilder(FILE_REF_MARKER).append(relativePath).append(LINES_REF_MARKER);
                var selectionModel = ac.editor().getSelectionModel();
                if (selectionModel.hasSelection()) {
                    int selectionStart = selectionModel.getSelectionStart();
                    int selectionEnd = selectionModel.getSelectionEnd();
                    int firstLine = ac.document().getLineNumber(selectionStart);
                    int lastLine = ac.document().getLineNumber(selectionEnd);
                    if (firstLine == lastLine) {
                        ref.append(firstLine + 1);
                    } else {
                        if (firstLine > lastLine) {
                            firstLine = (lastLine + firstLine) - (lastLine = firstLine);
                        }
                        ref.append(firstLine + 1).append(LINES_REF_INTERVAL).append(lastLine + 1);
                    }
                } else {
                    var carets = ac.editor().getCaretModel().getAllCarets();
                    var selection = carets.stream()
                            .map(caret -> String.valueOf(caret.getLogicalPosition().line + 1))
                            .collect(Collectors.joining(LINES_REF_GROUP_SEPARATOR));
                    ref.append(selection);
                }
                CopyPasteManager.getInstance().setContents(new StringSelection(ref.toString()));
            }
        });
    }

}
