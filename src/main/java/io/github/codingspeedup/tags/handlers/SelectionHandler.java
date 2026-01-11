package io.github.codingspeedup.tags.handlers;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.ai.deployment.orchestration.ResponseGateway;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptRef;
import io.github.codingspeedup.tags.ai.primitives.reactive.TagsPrompt;
import io.github.codingspeedup.tags.minions.ProjectBufferProvider;
import io.github.codingspeedup.tags.minions.ProjectPromptLibraryProvider;
import io.github.codingspeedup.tags.minions.ProjectToolboxSupport;
import io.github.codingspeedup.tags.plugin.settings.SettingsState;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

import static io.github.codingspeedup.tags.ai.deployment.orchestration.ChatMdUtl.*;
import static io.github.codingspeedup.tags.ai.primitives.reactive.PromptUtl.findVariables;
import static io.github.codingspeedup.tags.ai.primitives.reactive.PromptUtl.toProperties;

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
        var promptLib = new ProjectPromptLibraryProvider(project).load().orElseThrow();

        var promptBuilder = TagsPrompt.builder(
                SettingsState.getInstance(),
                new ProjectBufferProvider(project, null, selection),
                new ProjectPromptLibraryProvider(project),
                new ProjectToolboxSupport(project));

        promptBuilder.llmParameters(promptLib.getParameters());

        promptBuilder.systemTemplate(promptLib.getSystemTemplate());

        if (promptRef.getId().isEmpty()) {
            promptBuilder.userMessage(selection);
        } else {
            var promptTemplate = promptLib.getPromptTemplate(promptRef.getId());
            promptBuilder.userTemplate(promptTemplate);
            var promptVars = findVariables(promptTemplate);
            promptBuilder.contextArgs(promptVars.iterator().next(), selection);
        }

        if (indicator.isCanceled()) {
            return Optional.empty();
        }

        var prompt = promptBuilder.build();
        var chatResponse = prompt.execute();

        var mdContent = new StringBuilder();
        mdContent.append(renderParametersBlock(toProperties(prompt.chatRequest().parameters(), false)));
        mdContent.append(renderSystemBlock(promptLib.getSystemTemplate().template()));
        var chatMessages = prompt.chatRequest().messages();
        mdContent.append(renderUserBlock((UserMessage) chatMessages.get(chatMessages.size() - 1)));
        var mdOffset = mdContent.length();
        mdContent.append(renderAiBlock(chatResponse.aiMessage().text()));
        mdContent.append(StringUtils.EMPTY);

        var tagsResult = new TagsResult(ResponseGateway.CHAT);
        tagsResult.setBufferName(nextChatMdBufferName(project, fileName));
        tagsResult.setContent(mdContent.toString());
        tagsResult.setStartOffset(mdOffset);
        tagsResult.setEndOffset(mdOffset);
        return Optional.of(tagsResult);
    }


}
