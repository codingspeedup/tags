package io.github.codingspeedup.tags.engine.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMdUtl {

    public static final String CHAT_MD_EXTENSION = ".chat.md";


    private static final String DEFAULT_CHAT_MD = "default" + CHAT_MD_EXTENSION;

    public static boolean isChatMd(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(ChatMdUtl.CHAT_MD_EXTENSION);
    }

    public static void ensureDefaultChat(Project project, VirtualFile chatMdRoot) {
        if (chatMdRoot.findChild(DEFAULT_CHAT_MD) == null) {
            ApplicationManager.getApplication().invokeAndWait(() -> WriteAction.run(() -> {
                try {
                    var promptContext = PromptUtl.getDefaultPromptContext(project);
                    var newFile = chatMdRoot.createChildData(ChatMdUtl.class, DEFAULT_CHAT_MD);
                    VfsUtil.saveText(newFile, PromptUtl.renderParametersBlock(promptContext.getLeft())
                            + PromptUtl.renderSystemBlock(promptContext.getRight())
                            + PromptUtl.renderUserBlock(null));
                } catch (IOException e) {
                    throw new RuntimeException("Could not create default chat file", e);
                }
            }));
        }
    }

}
