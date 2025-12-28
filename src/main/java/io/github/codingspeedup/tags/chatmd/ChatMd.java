package io.github.codingspeedup.tags.chatmd;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;

public class ChatMd {

    public static final String CHAT_MD_EXTENSION = ".chat.md";
    private static final String DEFAULT_CHAT_MD = "default" + CHAT_MD_EXTENSION;

    public static void ensureDefaultChat(VirtualFile chatMdRoot) {
        if (chatMdRoot.findChild(DEFAULT_CHAT_MD) == null) {
            ApplicationManager.getApplication().invokeAndWait(() -> WriteAction.run(() -> {
                try {
                    var newFile = chatMdRoot.createChildData(ChatMd.class, DEFAULT_CHAT_MD);
                    VfsUtil.saveText(newFile, "# Default Chat\n\nStart your conversation here...");
                } catch (IOException e) {
                    throw new RuntimeException("Could not create default chat file", e);
                }
            }));
        }
    }

}
