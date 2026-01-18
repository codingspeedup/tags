package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import io.github.codingspeedup.tags.ai.deployment.orchestration.ChatMdUtl;
import io.github.codingspeedup.tags.minions.PluginUtl;
import io.github.codingspeedup.tags.minions.ProjectPromptLibraryProvider;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static io.github.codingspeedup.tags.ai.deployment.orchestration.ChatMdUtl.*;
import static io.github.codingspeedup.tags.minions.PluginUtl.reportError;

public class NewChatMdAction extends TagsActionBase {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        extractFolderActionContext(e).ifPresent(ac -> {
            var pLib = new ProjectPromptLibraryProvider(ac.project()).load().orElseThrow();

            @SuppressWarnings("all")
            var chatMdContent = new StringBuilder();
            chatMdContent.append(renderParametersBlock(pLib.getParameters()));
            chatMdContent.append(renderSystemBlock(pLib.getSystemTemplate().template()));
            chatMdContent.append(renderUserBlock(StringUtils.EMPTY));

            new Task.Backgroundable(ac.project(), "Opening new chat buffer") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    ApplicationManager.getApplication().invokeLater(() ->
                            WriteCommandAction.runWriteCommandAction(ac.project(), () -> {
                                try {
                                    var chatMdFile = PluginUtl.nextFile(ac.folder(), ChatMdUtl::buildChatMdFileName);
                                    PluginUtl.writeText(ac.project(), chatMdFile, chatMdContent.toString());
                                    FileEditorManager.getInstance(ac.project()).openFile(chatMdFile, true);
                                } catch (IOException e) {
                                    reportError(ac.project(), "Error opening new chat buffer", e);
                                }
                            }), ac.project().getDisposed());
                }
            }.queue();
        });
    }

}
