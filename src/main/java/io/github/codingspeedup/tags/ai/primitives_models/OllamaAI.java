package io.github.codingspeedup.tags.ai.primitives_models;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.github.codingspeedup.tags.ai.boundary.EnvironmentSettingsProvider;

import java.time.Duration;

public class OllamaAI implements LLM {

    private final OllamaChatModel ollamaChatModel;

    public OllamaAI(EnvironmentSettingsProvider settings) {
        this.ollamaChatModel = OllamaChatModel.builder()
                .baseUrl(settings.getOllamaUrl())
                .timeout(Duration.ofMinutes(5))
                .maxRetries(0)
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        return ollamaChatModel.chat(chatRequest);
    }

}
