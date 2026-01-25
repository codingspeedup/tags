package io.github.codingspeedup.tags.ai.primitives_models;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

public interface LLM {

    @SuppressWarnings("unused")
    ChatResponse chat(ChatRequest chatRequest);

}
