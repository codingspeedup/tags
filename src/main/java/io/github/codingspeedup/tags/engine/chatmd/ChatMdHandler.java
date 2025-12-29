package io.github.codingspeedup.tags.engine.chatmd;

import com.intellij.openapi.progress.ProgressIndicator;
import io.github.codingspeedup.tags.engine.core.PromptHandler;

import java.util.Optional;

public class ChatMdHandler implements PromptHandler {

    private final String mdContent;
    private final int mdLine;

    public ChatMdHandler(String mdContent, int mdLine) {
        this.mdContent = mdContent;
        this.mdLine = mdLine;
    }

    public Optional<String> process(ProgressIndicator indicator) {
        return Optional.empty();
    }

}
