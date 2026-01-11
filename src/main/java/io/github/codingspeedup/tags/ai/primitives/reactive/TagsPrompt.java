package io.github.codingspeedup.tags.ai.primitives.reactive;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import io.github.codingspeedup.tags.ai.boundary.PromptLibraryProvider;
import io.github.codingspeedup.tags.ai.primitives.models.Model;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class TagsPrompt {

    private final ChatRequestParameters llmParameters;
    private final String toolsApiDesc;
    private final List<ChatMessage> chatMessages;
    private final Model llm;

    public static TagsPromptBuilder builder(PromptLibraryProvider plp) {
        return new TagsPromptBuilder(plp);
    }

}
