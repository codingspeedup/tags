package io.github.codingspeedup.tags.ai.primitives_reactive;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.codingspeedup.tags.ai.boundary.BufferProvider;
import io.github.codingspeedup.tags.ai.boundary.EnvironmentSettingsProvider;
import io.github.codingspeedup.tags.ai.boundary.PromptLibraryProvider;
import io.github.codingspeedup.tags.ai.boundary.ToolboxSupport;
import io.github.codingspeedup.tags.ai.primitives_models.LLM;

public record TagsPrompt(LLM llm, ChatRequest chatRequest) {

    public static TagsPromptBuilder builder(
            EnvironmentSettingsProvider settings,
            BufferProvider bufferProvider,
            PromptLibraryProvider promptLibraryProvider,
            ToolboxSupport toolboxSupport
    ) {
        return new TagsPromptBuilder(settings, bufferProvider, promptLibraryProvider, toolboxSupport);
    }

    public ChatResponse execute() {
        return llm.chat(chatRequest);
    }

}
