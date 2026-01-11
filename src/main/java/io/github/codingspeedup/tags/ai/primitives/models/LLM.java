package io.github.codingspeedup.tags.ai.primitives.models;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;


public interface LLM {

    @SuppressWarnings("unused")
    ChatResponse chat(ChatRequest chatRequest);

    static ChatResponse doChat(ChatRequest chatRequest, String apiSpec) {
        return doChat(chatRequest.parameters(), apiSpec, chatRequest.messages().toArray(ChatMessage[]::new));
    }

    static ChatResponse doChat(ChatRequestParameters llmParameters, String apiSpec, ChatMessage... chatMessages) {
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

        if (apiSpec != null) {
            var metaSystem = """
                    # ROLE
                    You are a Groovy Scripting Engine.
                    Your task is to write a script that calls the PROVIDED_API to fulfill ORIGINAL_USER_REQUEST.
                    
                    # PROVIDED_API:
                    > [!NOTE]
                    > All calls must use the `ClassName.methodName` syntax.
                    %s
                    
                    ---
                    
                    # TARGET OBJECTIVE:
                    Fulfill the following request by writing a Groovy script using the PROVIDED_API.
                    
                    ## ORIGINAL_SYSTEM_INSTRUCTIONS:
                    %s
                    
                    ## ORIGINAL_USER_REQUEST:
                    """.formatted(apiSpec, systemText);

            systemText.setLength(0);
            systemText.append(metaSystem);
        }

        if (!systemText.isEmpty()) {
            var systemMessage = model.isSystemRoleSupported()
                    ? SystemMessage.from(systemText.toString())
                    : UserMessage.userMessage("system", systemText.toString());
            llmMessages.add(0, systemMessage);
        }

        if (apiSpec != null) {
            llmMessages.add(UserMessage.from("""
                    # CONSTRAINTS
                    - Return ONLY a valid Groovy script block.
                    - Do not include conversational filler or markdown explanations outside the code block.
                    - Ensure the script is self-contained and orchestrates the PROVIDED_API to reach the objective.
                    - **NAMED ARGUMENTS**: You may use Groovy's `key: value` syntax for optional parameters.
                    - **OMISSION**: If a parameter is marked with '?', OMIT it entirely rather than passing null or empty values.
                    """));
        }

        var chatRequest = ChatRequest.builder()
                .parameters(llmParameters)
                .messages(llmMessages)
                .build();

        return model.provider().chat(chatRequest);
    }

}
