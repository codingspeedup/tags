package io.github.codingspeedup.tags.engine;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.utils.*;
import io.github.codingspeedup.tags.integration.LLM;

import java.util.Optional;

public class SelectionPromptHandler implements PromptHandler {

    private final String fileName;
    private final String selection;
    private final PromptRef promptRef;

    public SelectionPromptHandler(String fileName, String selection, PromptRef promptRef) {
        this.fileName = fileName;
        this.selection = selection;
        this.promptRef = promptRef;
    }

    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var promptLib = promptRef.getLibrary(project);

        var chatMd = new StringBuilder();



        var chatRequestParameters = PromptUtl.buildChatRequestParameters(promptLib.getParameters());


        var systemMessage = SystemMessage.from(promptLib.getSystem().template());
        var userMessage = UserMessage.from(selection);
        if (indicator.isCanceled()) {
            return Optional.empty();
        }
        var response = LLM.doChat(systemMessage, userMessage);
        var mdContent = PromptUtl.renderUserBlock(selection);
        var mdOffset = mdContent.length();

        mdContent += PromptUtl.renderAiBlock(response.aiMessage().text())
                + "\n\n---\n"
                + PromptUtl.renderSystemBlock(promptLib.getSystem().template());

        var tagsResult = new TagsResult();
        tagsResult.setGateway(ActionResultGateway.BUFFER);
        tagsResult.setBufferName(fileName + ".result.md");
        tagsResult.setContent(mdContent);
        tagsResult.setStartOffset(mdOffset);
        tagsResult.setEndOffset(mdOffset);
        return Optional.of(tagsResult);
    }

}
