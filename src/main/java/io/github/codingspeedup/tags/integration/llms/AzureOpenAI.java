package io.github.codingspeedup.tags.integration.llms;

import com.azure.ai.openai.OpenAIServiceVersion;

import java.util.Arrays;
import java.util.Optional;

public class AzureOpenAI {

    public static Optional<OpenAIServiceVersion> identifyVersion(String version) {
        return Arrays.stream(OpenAIServiceVersion.values())
                .filter(v -> v.getVersion().equals(version))
                .findFirst();
    }

}
