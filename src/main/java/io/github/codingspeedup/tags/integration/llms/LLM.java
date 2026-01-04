package io.github.codingspeedup.tags.integration.llms;

import dev.langchain4j.data.message.*;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.codingspeedup.tags.plugin.TagsSettings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

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

        var processedMessages = new ArrayList<ChatMessage>();
        var pendingSystemText = new StringBuilder();
        var systemRoleSupported = isSystemRoleSupported(llmParameters.modelName());
        for (ChatMessage m : chatMessages) {
            if (m instanceof SystemMessage sm && !systemRoleSupported) {
                pendingSystemText.append(sm.text()).append("\n\n");
            } else if (m instanceof UserMessage um && !pendingSystemText.isEmpty()) {
                var newContents = new ArrayList<Content>();
                newContents.add(TextContent.from(pendingSystemText.toString()));
                newContents.addAll(um.contents());

                processedMessages.add(UserMessage.from(um.name(), newContents));
                pendingSystemText.setLength(0);
            } else {
                processedMessages.add(m);
            }
        }

        if (CollectionUtils.isNotEmpty(llmParameters.toolSpecifications())) {
            var toolSpec = Json.toJson(llmParameters.toolSpecifications());
            try {
                var field = llmParameters.getClass().getDeclaredField("toolSpecifications");
                field.setAccessible(true);
                field.set(llmParameters, null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to nullify tools via reflection", e);
            }
        }

        var chatRequest = ChatRequest.builder()
                .parameters(llmParameters)
                .messages(processedMessages)
                .build();

        return new GoogleAI().chat(chatRequest);
    }

    static boolean isSystemRoleSupported(String modelName) {
        return StringUtils.trimToEmpty(modelName).toLowerCase().contains("gemini");
    }

}
