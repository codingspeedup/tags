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
import io.github.codingspeedup.tags.utils.PromptLibrary;
import io.github.codingspeedup.tags.utils.TagsUtl;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static io.github.codingspeedup.tags.utils.ChatUtl.*;
import static io.github.codingspeedup.tags.utils.TagsUtl.reportError;

public class NewChatMdAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        var isFolder = file != null && file.isDirectory();
        e.getPresentation().setEnabledAndVisible(isFolder);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }

        var chatFolder = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (chatFolder == null || !chatFolder.isDirectory()) {
            reportError(project, "Chat folder does not exist");
            return;
        }

        var pLib = PromptLibrary.of(project, TagsUtl.resolvePromptLibrary(project).orElseThrow());
        @SuppressWarnings("all")
        var bufferContent = new StringBuilder();
        bufferContent.append(renderParametersBlock(pLib.getParameters()));
        bufferContent.append(renderSystemBlock(pLib.getSystemTemplate().template()));
        bufferContent.append(renderUserBlock(StringUtils.EMPTY));

        new Task.Backgroundable(project, "Opening new chat buffer") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                ApplicationManager.getApplication().invokeLater(() ->
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                var bufferFile = nextBufferName(chatFolder);
                                TagsUtl.writeText(project, bufferFile, bufferContent.toString());
                                FileEditorManager.getInstance(project).openFile(bufferFile, true);
                            } catch (IOException e) {
                                reportError(project, "Error opening new chat buffer", e);
                            }
                        }), project.getDisposed());
            }
        }.queue();
    }

}
