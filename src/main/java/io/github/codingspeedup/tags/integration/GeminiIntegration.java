package io.github.codingspeedup.tags.integration;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.codingspeedup.tags.settings.PluginSettingsState;
import org.apache.commons.lang.StringUtils;

public class GeminiIntegration implements LLM {

    private final GoogleAiGeminiChatModel geminiChatModel;

    public GeminiIntegration() {
        this.geminiChatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(PluginSettingsState.getInstance().getGeminiApiKey())
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        if (StringUtils.isBlank(chatRequest.modelName())) {
            chatRequest = ChatRequest.builder()
                    .messages(chatRequest.messages())
                    .parameters(chatRequest.parameters().overrideWith(
                            ChatRequestParameters.builder().modelName("gemini-flash-latest").build()))
                    .build();
        }
        return geminiChatModel.chat(chatRequest);
    }

}
