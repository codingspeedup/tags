package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VfsUtilCore;
import io.github.codingspeedup.tags.ai.composition.reactive.ToolboxUtl;
import io.github.codingspeedup.tags.minions.PluginUtl;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;

public class NewToolboxAction extends TagsActionBase {

    @Override
    public void update(@NotNull AnActionEvent e) {
        var project = e.getProject();
        var isAvailable = project != null;
        if (isAvailable) {
            var location = e.getData(CommonDataKeys.VIRTUAL_FILE);
            isAvailable = location != null;
            if (isAvailable) {
                var pLibRoot = PluginUtl.resolveToolboxFolder(project).orElseThrow();
                isAvailable = VfsUtilCore.isAncestor(pLibRoot, location, false);
            }
        }
        e.getPresentation().setEnabledAndVisible(isAvailable);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        extractFolderActionContext(e).ifPresent(ac ->
                new Task.Backgroundable(ac.project(), "Creating new toolbox") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        ApplicationManager.getApplication().invokeLater(() ->
                                WriteCommandAction.runWriteCommandAction(ac.project(), () -> {
                                    try {
                                        var toolboxFile = PluginUtl.nextFile(ac.folder(), ToolboxUtl::buildToolboxFileName);
                                        var packageName = VfsUtilCore.getRelativePath(ac.folder(),
                                                PluginUtl.resolveToolboxFolder(ac.project()).orElseThrow().getParent(), '.');
                                        var toolboxName = FilenameUtils.getBaseName(toolboxFile.getName());
                                        var toolboxSource = ToolboxUtl.buildSampleToolbox(packageName, toolboxName);
                                        PluginUtl.writeText(ac.project(), toolboxFile, toolboxSource);
                                        FileEditorManager.getInstance(ac.project()).openFile(toolboxFile, true);
                                    } catch (IOException e) {
                                        reportError(ac.project(), "Error creating new toolbox", e);
                                    }
                                }), ac.project().getDisposed());
                    }
                }.queue());
    }

}
