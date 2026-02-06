package io.github.codingspeedup.tags.ai.primitives_models;

import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.codingspeedup.tags.ai.boundary.EnvironmentSettingsProvider;
import org.apache.commons.lang.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.util.Locale;


public class AmazonBedrockAI implements LLM {

    private final BedrockChatModel model;

    public AmazonBedrockAI(EnvironmentSettingsProvider settings) {
        // System.setProperty("aws.accessKeyId", settings.getAwsAccessKeyId());

        var region = resolveRegion(settings.getAwsRegion());

        BedrockRuntimeClient client;

        if (StringUtils.isBlank(settings.getAmazonBedrockModelToken())) {
            var credentials = StringUtils.isBlank(settings.getAwsSessionToken())
                    ? AwsBasicCredentials.create(settings.getAwsAccessKeyId(), settings.getAwsSecretAccessKey())
                    : AwsSessionCredentials.create(settings.getAwsAccessKeyId(), settings.getAwsSecretAccessKey(), settings.getAwsSessionToken());

            client = BedrockRuntimeClient.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        } else {
            client = null;
        }

        model = BedrockChatModel.builder()
                .client(client)
                .modelId(settings.getAmazonBedrockModelId())
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        return model.chat(chatRequest);
    }

    private static Region resolveRegion(String awsRegion) {
        awsRegion = StringUtils.trimToEmpty(awsRegion);
        if (StringUtils.isEmpty(awsRegion)) {
            return Region.US_EAST_1;
        }
        for (var region : Region.regions()) {
            if (StringUtils.equalsIgnoreCase(region.id(), awsRegion)) {
                return region;
            }
        }
        return Region.of(awsRegion.toLowerCase(Locale.ROOT));
    }


}
