package io.github.codingspeedup.tags.engine.selection;

import com.intellij.openapi.progress.ProgressIndicator;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.engine.core.PromptHandler;
import io.github.codingspeedup.tags.engine.core.PromptUtl;
import io.github.codingspeedup.tags.integration.LLM;

import java.util.Optional;

public class SelectionHandler implements PromptHandler {

    private final String selection;

    public SelectionHandler(String selection) {
        this.selection = selection;
    }

    public Optional<String> process(ProgressIndicator indicator) {
        var systemMessage = SystemMessage.from(PromptUtl.getDefaultSystemMessage());
        var userMessage = UserMessage.from(selection);
        if (indicator.isCanceled()) {
            return Optional.empty();
        }
        var response = LLM.chat(systemMessage, userMessage);
        var mdContent = PromptUtl.getUserBlock(selection)
                + PromptUtl.getAssistantBlock(response.aiMessage().text())
                + "\n\n---\n"
                + PromptUtl.getSystemBlock(PromptUtl.getDefaultSystemMessage());
        return Optional.of(mdContent);
    }

}
