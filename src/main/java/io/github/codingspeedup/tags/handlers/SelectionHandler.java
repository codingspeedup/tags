package io.github.codingspeedup.tags.handlers;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.ai.deployment.orchestration.ResponseGateway;
import io.github.codingspeedup.tags.ai.primitives.models.LLM;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptRef;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Optional;

import static io.github.codingspeedup.tags.ai.deployment.orchestration.ChatMdUtl.*;
import static io.github.codingspeedup.tags.ai.primitives.reactive.PromptUtl.*;

public class SelectionHandler implements ActionHandler {

    private final String fileName;
    private final String selection;
    private final PromptRef promptRef;

    public SelectionHandler(String fileName, String selection, PromptRef promptRef) {
        this.fileName = fileName;
        this.selection = selection;
        this.promptRef = promptRef;
    }

    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var promptLib = promptRef.getLibrary(project);

        var chatRequestParameters = toChatRequestParameters(promptLib.getParameters());
        var systemMessage = SystemMessage.from(promptLib.getSystemTemplate().template());
        UserMessage userMessage;
        if (promptRef.getId().isEmpty()) {
            userMessage = UserMessage.from(selection);
        } else {
            var promptTemplate = promptLib.getPromptTemplate(promptRef.getId());
            var promptVars = findVariables(promptTemplate);
            Map<String, Object> args = Map.of(promptVars.iterator().next(), selection);
            userMessage = promptTemplate.apply(args).toUserMessage();
        }

        if (indicator.isCanceled()) {
            return Optional.empty();
        }
        var response = LLM.doChat(chatRequestParameters, null, systemMessage, userMessage);

        var mdContent = new StringBuilder();
        mdContent.append(renderParametersBlock(toProperties(chatRequestParameters, false)));
        mdContent.append(renderSystemBlock(promptLib.getSystemTemplate().template()));
        mdContent.append(renderUserBlock(userMessage));
        var mdOffset = mdContent.length();
        mdContent.append(renderAiBlock(response.aiMessage().text()));
        mdContent.append(StringUtils.EMPTY);

        var tagsResult = new TagsResult(ResponseGateway.CHAT);
        tagsResult.setBufferName(nextChatMdBufferName(project, fileName));
        tagsResult.setContent(mdContent.toString());
        tagsResult.setStartOffset(mdOffset);
        tagsResult.setEndOffset(mdOffset);
        return Optional.of(tagsResult);
    }


}
