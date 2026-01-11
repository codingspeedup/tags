package io.github.codingspeedup.tags.ai.boundary;

public interface EnvironmentSettingsProvider {

    boolean isUseAzureOpenAiModel();

    String getAzureOpenAiApiKey();

    String getAzureOpenAiUrl();

    String getAzureOpenAiDeployment();

    String getAzureOpenAiApiVersion();

    String getGeminiApiKey();

    String getGeminiModel();

    String getOllamaUrl();

    String getOllamaModel();

}
