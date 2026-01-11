package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VfsUtilCore;
import io.github.codingspeedup.tags.plugin.core.TagsUtl;
import io.github.codingspeedup.tags.ai.composition.reactive.ToolboxUtl;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static io.github.codingspeedup.tags.plugin.core.TagsUtl.reportError;

public class NewToolboxAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var project = e.getProject();
        var isAvailable = project != null;
        if (isAvailable) {
            var location = e.getData(CommonDataKeys.VIRTUAL_FILE);
            isAvailable = location != null;
            if (isAvailable) {
                var pLibRoot = TagsUtl.resolveToolboxFolder(project).orElseThrow();
                isAvailable = VfsUtilCore.isAncestor(pLibRoot, location, false);
            }
        }
        e.getPresentation().setEnabledAndVisible(isAvailable);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }
        var location = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (location != null && !location.isDirectory()) {
            location = location.getParent();
        }
        if (location == null) {
            return;
        }
        var toolboxFolder = location;

        new Task.Backgroundable(project, "Creating new toolbox") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                ApplicationManager.getApplication().invokeLater(() ->
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                var toolboxFile = ToolboxUtl.nextToolboxFile(toolboxFolder);
                                var packageName = VfsUtilCore.getRelativePath(toolboxFolder,
                                        TagsUtl.resolveToolboxFolder(project).orElseThrow().getParent(), '.');
                                var toolboxName = FilenameUtils.getBaseName(toolboxFile.getName());
                                var toolboxSource = ToolboxUtl.buildSampleToolbox(packageName, toolboxName);
                                TagsUtl.writeText(project, toolboxFile, toolboxSource);
                                FileEditorManager.getInstance(project).openFile(toolboxFile, true);
                            } catch (IOException e) {
                                reportError(project, "Error creating new toolbox", e);
                            }
                        }), project.getDisposed());
            }
        }.queue();
    }

}
