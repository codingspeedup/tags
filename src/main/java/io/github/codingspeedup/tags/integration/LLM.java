package io.github.codingspeedup.tags.integration;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import io.github.codingspeedup.tags.plugin.TagsSettings;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

public interface LLM {

    @SuppressWarnings("unused")
    ChatResponse chat(ChatRequest chatRequest);

    static ChatResponse chat(ChatMessage... chatMessages) {
        return chat(ChatRequestParameters.builder().build(), chatMessages);
    }

    static ChatResponse chat(ChatRequestParameters llmParameters, ChatMessage... chatMessages) {
        var modelName = llmParameters.modelName();
        if (StringUtils.isBlank(modelName)) {
            modelName = TagsSettings.getInstance().getGeminiModel();
            llmParameters = llmParameters.overrideWith(ChatRequestParameters.builder().modelName(modelName).build());
        }

        var processedMessages = new ArrayList<ChatMessage>();
        var pendingSystemText = new StringBuilder();
        var systemRoleSupported = isSystemRoleSupported(modelName);
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

        var chatRequest = ChatRequest.builder()
                .parameters(llmParameters)
                .messages(processedMessages)
                .build();

        try {
            return new GoogleAI().chat(chatRequest);
        } catch (Exception e) {
            var aiMessage = AiMessage.builder()
                    .text(e.getClass().getName() + ": " + e.getMessage())
                    .build();
            return ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .modelName(chatRequest.modelName())
                    .finishReason(FinishReason.OTHER)
                    .build();
        }
    }

    static boolean isSystemRoleSupported(String modelName) {
        return StringUtils.trimToEmpty(modelName).toLowerCase().contains("gemini");
    }

}
