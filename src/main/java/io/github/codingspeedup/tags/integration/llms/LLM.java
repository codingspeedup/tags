package io.github.codingspeedup.tags.integration.llms;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.codingspeedup.tags.plugin.TagsSettings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public interface LLM {

    @SuppressWarnings("unused")
    ChatResponse chat(ChatRequest chatRequest);

    static ChatResponse doChat(ChatRequest chatRequest) {
        return doChat(chatRequest.parameters(), chatRequest.messages().toArray(ChatMessage[]::new));
    }

    static ChatResponse doChat(ChatRequestParameters llmParameters, ChatMessage... chatMessages) {
        llmParameters = llmParameters.defaultedBy(ChatRequestParameters.builder()
                .modelName(TagsSettings.getInstance().getGeminiModel())
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

        if (CollectionUtils.isNotEmpty(llmParameters.toolSpecifications())) {
            var toolSpec = Json.toJson(llmParameters.toolSpecifications());

            if (!systemText.isEmpty()) {
                systemText.append("---\n**Additional Engineering Rules:**\n");
            }

            systemText.append("## ROLE: Expert Groovy Script Generator\n");
            systemText.append("## TASK: Generate a Groovy script using this API specification:\n\n");
            systemText.append(toolSpec).append("\n\n");
            systemText.append("## RULES:\n");
            systemText.append("- Output ONLY the code block\n");
            systemText.append("- Code comments are optional\n");
            systemText.append("- Simulate the logic; do not call external APIs.\n");

            try {
                var field = llmParameters.getClass().getDeclaredField("toolSpecifications");
                field.setAccessible(true);
                field.set(llmParameters, null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to nullify tools via reflection", e);
            }
        }

        if (!systemText.isEmpty()) {
            var systemMessage = isSystemRoleSupported(llmParameters.modelName())
                    ? SystemMessage.from(systemText.toString())
                    : UserMessage.userMessage("system", systemText.toString());
            llmMessages.add(0, systemMessage);
        }

        var chatRequest = ChatRequest.builder()
                .parameters(llmParameters)
                .messages(llmMessages)
                .build();

        return new GoogleAI().chat(chatRequest);
    }

    static boolean isSystemRoleSupported(String modelName) {
        return StringUtils.trimToEmpty(modelName).toLowerCase().contains("gemini");
    }

}
