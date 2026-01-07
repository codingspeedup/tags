package io.github.codingspeedup.tags.integration.llms;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.github.codingspeedup.tags.plugin.settings.TagsSettings;

import java.time.Duration;

public class OllamaAI implements LLM {

    private final OllamaChatModel ollamaChatModel;

    public OllamaAI() {
        this.ollamaChatModel = OllamaChatModel.builder()
                .baseUrl(TagsSettings.getInstance().getOllamaUrl())
                .timeout(Duration.ofMinutes(5))
                .maxRetries(0)
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        return ollamaChatModel.chat(chatRequest);
    }

}
