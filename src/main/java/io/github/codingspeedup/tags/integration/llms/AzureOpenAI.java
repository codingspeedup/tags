package io.github.codingspeedup.tags.integration.llms;

import com.azure.ai.openai.OpenAIServiceVersion;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.codingspeedup.tags.plugin.settings.TagsSettings;

import java.util.Arrays;
import java.util.Optional;

public class AzureOpenAI implements LLM {

    public static Optional<OpenAIServiceVersion> identifyVersion(String version) {
        return Arrays.stream(OpenAIServiceVersion.values())
                .filter(v -> v.getVersion().equals(version))
                .findFirst();
    }

    private final AzureOpenAiChatModel.Builder modelBuilder;

    public AzureOpenAI() {
        var settings = TagsSettings.getInstance();
        modelBuilder = AzureOpenAiChatModel.builder()
                .apiKey(settings.getAzureOpenAiApiKey())
                .endpoint(settings.getAzureOpenAiUrl())
                .deploymentName(settings.getAzureOpenAiDeployment())
                .serviceVersion(settings.getAzureOpenAiApiVersion())
        ;
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        var model = modelBuilder.defaultRequestParameters(chatRequest.parameters()).build();
        return model.chat(chatRequest);
    }

}
