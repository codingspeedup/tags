package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VfsUtilCore;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptLibUtl;
import io.github.codingspeedup.tags.minions.PluginUtl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;

public class NewPromptLibraryAction extends TagsActionBase {

    @Override
    public void update(@NotNull AnActionEvent e) {
        var project = e.getProject();
        var isAvailable = project != null;
        if (isAvailable) {
            var location = e.getData(CommonDataKeys.VIRTUAL_FILE);
            isAvailable = location != null;
            if (isAvailable) {
                var pLibRoot = PluginUtl.resolvePromptLibrary(project).orElseThrow().getParent();
                isAvailable = VfsUtilCore.isAncestor(pLibRoot, location, false);
            }
        }
        e.getPresentation().setEnabledAndVisible(isAvailable);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        extractFolderActionContext(e).ifPresent(ac ->
                new Task.Backgroundable(ac.project(), "Creating new prompt library") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        ApplicationManager.getApplication().invokeLater(() ->
                                WriteCommandAction.runWriteCommandAction(ac.project(), () -> {
                                    try {
                                        var pLibFile = PluginUtl.nextFile(ac.folder(), PromptLibUtl::buildPromptLibraryFileName);
                                        PluginUtl.writeText(ac.project(), pLibFile, PromptLibUtl.SAMPLE_LIBRARY_CONTENT);
                                        FileEditorManager.getInstance(ac.project()).openFile(pLibFile, true);
                                    } catch (IOException e) {
                                        reportError(ac.project(), "Error creating new prompt library", e);
                                    }
                                }), ac.project().getDisposed());
                    }
                }.queue());
    }

}
