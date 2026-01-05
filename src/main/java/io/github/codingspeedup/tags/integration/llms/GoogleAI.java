package io.github.codingspeedup.tags.integration.llms;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.codingspeedup.tags.plugin.TagsSettings;

public class GoogleAI implements LLM {

    private final GoogleAiGeminiChatModel geminiChatModel;

    public GoogleAI() {
        this.geminiChatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(TagsSettings.getInstance().getGeminiApiKey())
                .maxRetries(0)
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        return geminiChatModel.chat(chatRequest);
    }

}
