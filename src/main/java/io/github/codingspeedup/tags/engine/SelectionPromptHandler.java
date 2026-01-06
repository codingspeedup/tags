package io.github.codingspeedup.tags.engine;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.integration.llms.LLM;
import io.github.codingspeedup.tags.utils.ActionResultGateway;
import io.github.codingspeedup.tags.utils.PromptHandler;
import io.github.codingspeedup.tags.utils.PromptDesc;
import io.github.codingspeedup.tags.utils.TagsResult;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Optional;

import static io.github.codingspeedup.tags.utils.ChatUtl.*;
import static io.github.codingspeedup.tags.utils.PromptUtl.*;

public class SelectionPromptHandler implements PromptHandler {

    private final String fileName;
    private final String selection;
    private final PromptDesc promptDesc;

    public SelectionPromptHandler(String fileName, String selection, PromptDesc promptDesc) {
        this.fileName = fileName;
        this.selection = selection;
        this.promptDesc = promptDesc;
    }

    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var promptLib = promptDesc.getLibrary(project);

        var chatRequestParameters = toChatRequestParameters(promptLib.getParameters());
        var systemMessage = SystemMessage.from(promptLib.getSystemTemplate().template());
        UserMessage userMessage;
        if (promptDesc.getId().isEmpty()) {
            userMessage = UserMessage.from(selection);
        } else {
            var promptTemplate = promptLib.getPromptTemplate(promptDesc.getId());
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

        var tagsResult = new TagsResult(ActionResultGateway.CHAT);
        tagsResult.setBufferName(nextBufferName(project, fileName));
        tagsResult.setContent(mdContent.toString());
        tagsResult.setStartOffset(mdOffset);
        tagsResult.setEndOffset(mdOffset);
        return Optional.of(tagsResult);
    }


}
