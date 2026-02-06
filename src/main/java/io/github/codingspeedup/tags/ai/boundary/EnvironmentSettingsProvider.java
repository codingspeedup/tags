package io.github.codingspeedup.tags.ai.boundary;

public interface EnvironmentSettingsProvider {

    boolean isUseAzureOpenAiModel();

    String getAzureOpenAiApiKey();

    String getAzureOpenAiUrl();

    String getAzureOpenAiDeployment();

    String getAzureOpenAiApiVersion();


    boolean isUseAmazonBedrockModel();

    String getAwsRegion();

    String getAwsAccessKeyId();

    String getAwsSecretAccessKey();

    String getAwsSessionToken();

    String getAmazonBedrockModelToken();

    String getAmazonBedrockModelId();


    boolean isUseGeminiModel();

    String getGeminiApiKey();

    String getGeminiModel();


    boolean isUseOllamaModel();

    String getOllamaUrl();

    String getOllamaModel();

}
