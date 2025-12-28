package io.github.codingspeedup.tags.integration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;

public interface LLM {

    @SuppressWarnings("unused")
    ChatResponse chat(ChatRequest chatRequest);

    static ChatResponse chat(String message) {
        var chatMessage = UserMessage.from(message);
        var chatRequest = ChatRequest.builder()
                .messages(chatMessage)
                .build();
        try {
            return new GeminiIntegration().chat(chatRequest);
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

}
