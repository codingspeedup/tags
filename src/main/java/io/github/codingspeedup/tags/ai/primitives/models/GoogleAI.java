package io.github.codingspeedup.tags.ai.primitives.models;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.codingspeedup.tags.ai.boundary.EnvironmentSettingsProvider;

public class GoogleAI implements LLM {

    private final GoogleAiGeminiChatModel geminiChatModel;

    public GoogleAI(EnvironmentSettingsProvider settings) {
        this.geminiChatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(settings.getGeminiApiKey())
                .maxRetries(0)
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        return geminiChatModel.chat(chatRequest);
    }

}
