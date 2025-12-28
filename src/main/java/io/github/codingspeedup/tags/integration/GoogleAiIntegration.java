package io.github.codingspeedup.tags.integration;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.codingspeedup.tags.settings.PluginSettingsState;

public class GoogleAiIntegration implements LLM {

    private final GoogleAiGeminiChatModel geminiChatModel;

    public GoogleAiIntegration() {
        this.geminiChatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(PluginSettingsState.getInstance().getGeminiApiKey())
                .maxRetries(1)
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        return geminiChatModel.chat(chatRequest);
    }

}
