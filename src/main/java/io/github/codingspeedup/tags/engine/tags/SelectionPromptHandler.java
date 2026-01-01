package io.github.codingspeedup.tags.engine.tags;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.engine.core.ActionResultGateway;
import io.github.codingspeedup.tags.engine.core.TagsResult;
import io.github.codingspeedup.tags.engine.core.PromptHandler;
import io.github.codingspeedup.tags.engine.core.PromptUtl;
import io.github.codingspeedup.tags.integration.LLM;

import java.util.Optional;

public class SelectionPromptHandler implements PromptHandler {

    private final String fileName;
    private final String selection;

    public SelectionPromptHandler(String fileName, String selection) {
        this.fileName = fileName;
        this.selection = selection;
    }

    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var promptContext = PromptUtl.getDefaultPromptContext(project);

        var systemMessage = SystemMessage.from(promptContext.getRight());
        var userMessage = UserMessage.from(selection);
        if (indicator.isCanceled()) {
            return Optional.empty();
        }
        var response = LLM.doChat(systemMessage, userMessage);
        var mdContent = PromptUtl.renderUserBlock(selection);
        var mdOffset = mdContent.length();

        mdContent += PromptUtl.renderAiBlock(response.aiMessage().text())
                + "\n\n---\n"
                + PromptUtl.renderSystemBlock(promptContext.getRight());

        var tagsResult = new TagsResult();
        tagsResult.setGateway(ActionResultGateway.BUFFER);
        tagsResult.setBufferName(fileName + ".result.md");
        tagsResult.setContent(mdContent);
        tagsResult.setStartOffset(mdOffset);
        tagsResult.setEndOffset(mdOffset);
        return Optional.of(tagsResult);
    }

}
