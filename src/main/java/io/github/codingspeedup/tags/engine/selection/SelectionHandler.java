package io.github.codingspeedup.tags.engine.selection;

import com.intellij.openapi.progress.ProgressIndicator;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.engine.core.GenerationSink;
import io.github.codingspeedup.tags.engine.core.GenerationResponse;
import io.github.codingspeedup.tags.engine.core.PromptHandler;
import io.github.codingspeedup.tags.engine.core.PromptUtl;
import io.github.codingspeedup.tags.integration.LLM;

import java.util.Optional;

public class SelectionHandler implements PromptHandler {

    private final String fileName;
    private final String selection;

    public SelectionHandler(String fileName, String selection) {
        this.fileName = fileName;
        this.selection = selection;
    }

    public Optional<GenerationResponse> process(ProgressIndicator indicator) {
        var systemMessage = SystemMessage.from(PromptUtl.getDefaultSystemMessage());
        var userMessage = UserMessage.from(selection);
        if (indicator.isCanceled()) {
            return Optional.empty();
        }
        var response = LLM.chat(systemMessage, userMessage);
        var mdContent = PromptUtl.getUserBlock(selection);
        var mdOffset = mdContent.length();

        mdContent += PromptUtl.getAssistantBlock(response.aiMessage().text())
                + "\n\n---\n"
                + PromptUtl.getSystemBlock(PromptUtl.getDefaultSystemMessage());

        var gr = new GenerationResponse();
        gr.setOutputChannel(GenerationSink.MD_BUFFER);
        gr.setBufferName(fileName + ".result.md");
        gr.setGeneratedContent(mdContent);
        gr.setContentOffset(mdOffset);
        return Optional.of(gr);
    }

}
