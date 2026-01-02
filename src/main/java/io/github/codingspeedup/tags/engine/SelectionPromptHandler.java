package io.github.codingspeedup.tags.engine;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.integration.LLM;
import io.github.codingspeedup.tags.utils.ActionResultGateway;
import io.github.codingspeedup.tags.utils.PromptHandler;
import io.github.codingspeedup.tags.utils.PromptRef;
import io.github.codingspeedup.tags.utils.TagsResult;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Optional;

import static io.github.codingspeedup.tags.utils.ChatUtl.*;
import static io.github.codingspeedup.tags.utils.PromptUtl.*;

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

        var chatRequestParameters = buildChatRequestParameters(promptLib.getParameters());
        var systemMessage = SystemMessage.from(promptLib.getSystem().template());
        UserMessage userMessage;
        if (promptRef.getPromptId().isEmpty()) {
            userMessage = UserMessage.from(selection);
        } else {
            var promptTemplate = promptLib.getPromptTemplate(promptRef.getPromptId());
            var promptVars = findVariables(promptTemplate);
            Map<String, Object> args = Map.of(promptVars.iterator().next(), selection);
            userMessage = promptTemplate.apply(args).toUserMessage();
        }

        if (indicator.isCanceled()) {
            return Optional.empty();
        }
        var response = LLM.doChat(chatRequestParameters, systemMessage, userMessage);

        var mdContent = new StringBuilder();
        mdContent.append(renderParametersBlock(toProperties(chatRequestParameters, false)));
        mdContent.append(renderSystemBlock(promptLib.getSystem().template()));
        mdContent.append(renderUserBlock(userMessage));
        var mdOffset = mdContent.length();
        mdContent.append(renderAiBlock(response.aiMessage().text()));
        mdContent.append(StringUtils.EMPTY);

        var tagsResult = new TagsResult(ActionResultGateway.CHAT);
        tagsResult.setBufferName(nextBufferName(project, fileName));
        tagsResult.setContent(mdContent.toString());
        tagsResult.setStartOffset(mdOffset);
        tagsResult.setEndOffset(mdOffset);
        return Optional.of(tagsResult);
    }


}
