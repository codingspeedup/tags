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
import io.github.codingspeedup.tags.ai.deployment.orchestration.ChatMdUtl;
import io.github.codingspeedup.tags.minions.ProjectPromptLibraryProvider;
import io.github.codingspeedup.tags.minions.PluginUtl;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;
import static io.github.codingspeedup.tags.ai.deployment.orchestration.ChatMdUtl.*;

public class NewChatMdAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
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
        var chatMdFolder = location;

        var pLib = new ProjectPromptLibraryProvider(project).load().orElseThrow();

        @SuppressWarnings("all")
        var chatMdContent = new StringBuilder();
        chatMdContent.append(renderParametersBlock(pLib.getParameters()));
        chatMdContent.append(renderSystemBlock(pLib.getSystemTemplate().template()));
        chatMdContent.append(renderUserBlock(StringUtils.EMPTY));

        new Task.Backgroundable(project, "Opening new chat buffer") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                ApplicationManager.getApplication().invokeLater(() ->
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                var chatMdFile = PluginUtl.nextFile(chatMdFolder, ChatMdUtl::buildChatMdFileName);
                                PluginUtl.writeText(project, chatMdFile, chatMdContent.toString());
                                FileEditorManager.getInstance(project).openFile(chatMdFile, true);
                            } catch (IOException e) {
                                reportError(project, "Error opening new chat buffer", e);
                            }
                        }), project.getDisposed());
            }
        }.queue();
    }

}
