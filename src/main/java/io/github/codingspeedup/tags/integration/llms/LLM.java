package io.github.codingspeedup.tags.integration.llms;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static io.github.codingspeedup.tags.integration.llms.Model.nullifyToolSpecification;

public interface LLM {

    @SuppressWarnings("unused")
    ChatResponse chat(ChatRequest chatRequest);

    static ChatResponse doChat(ChatRequest chatRequest) {
        return doChat(chatRequest.parameters(), chatRequest.messages().toArray(ChatMessage[]::new));
    }

    static ChatResponse doChat(ChatRequestParameters llmParameters, ChatMessage... chatMessages) {
        var model = Model.of(llmParameters.modelName()).orElseThrow();

        llmParameters = llmParameters.defaultedBy(ChatRequestParameters.builder()
                .modelName(model.name())
                .build());

        var systemText = new StringBuilder();

        var llmMessages = Arrays.stream(chatMessages)
                .filter(m -> {
                    if (m instanceof SystemMessage s) {
                        systemText.append(s.text()).append("\n\n");
                        return false;
                    }
                    return true;
                }).collect(Collectors.toCollection(ArrayList::new));

        var toolsProvided = CollectionUtils.isNotEmpty(llmParameters.toolSpecifications());
        if (toolsProvided) {
            var toolSpec = Json.toJson(llmParameters.toolSpecifications());
            nullifyToolSpecification(llmParameters);

            var metaSystem = """
                    # ROLE
                    You are a Groovy Scripting Engine.
                    Your task is to write a script that calls the PROVIDED_API to fulfill ORIGINAL_USER_REQUEST.
                    
                    # PROVIDED_API:
                    %s
                    
                    # TARGET OBJECTIVE:
                    Fulfill the following request by writing a Groovy script using the PROVIDED_API.
                    
                    ## ORIGINAL_SYSTEM_INSTRUCTIONS:
                    %s
                    
                    ## ORIGINAL_USER_REQUEST:
                    """.formatted(toolSpec, systemText);

            systemText.setLength(0);
            systemText.append(metaSystem);
        }

        if (!systemText.isEmpty()) {
            var systemMessage = model.isSystemRoleSupported()
                    ? SystemMessage.from(systemText.toString())
                    : UserMessage.userMessage("system", systemText.toString());
            llmMessages.add(0, systemMessage);
        }

        if (toolsProvided) {
            llmMessages.add(UserMessage.from("""
                    # CONSTRAINTS
                    - Return ONLY a valid Groovy script block.
                    - Use 'var' for all local variable declarations.
                    - Do not include conversational filler or markdown explanations outside the code block.
                    - Ensure the script is self-contained and orchestrates the PROVIDED_API to reach the objective.
                    """));
        }

        var chatRequest = ChatRequest.builder()
                .parameters(llmParameters)
                .messages(llmMessages)
                .build();

        return model.provider().chat(chatRequest);
    }

}
