package io.github.codingspeedup.tags.ai.primitives.reactive;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Properties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromptLibUtl {

    public static final String PLUGIN_PROMPT_LIBRARY_NAME = "~";

    public static final String PLUGIN_PROMPT_LIBRARY_EXTENSION = ".yaml";

    public static final String SAMPLE_LIBRARY_CONTENT = """
            parameters:
              temperature: null
              topP: null
              topK: null
              frequencyPenalty: null
              presencePenalty: null
              maxOutputTokens: null
              stopSequences: null
            
            defaults:
              some-var: default value
            
            system: |
              You are a helpful and precise AI assistant.
              Provide clear, accurate, and direct responses to the user's instructions.
            
            prompts:
              - id: "LoremIpsum"
                template: |
                  Lorem ipsum {{some-var}} ...
                gateway: #tags+
                plus: null
            
            """;

    public static String buildPromptLibraryFileName(int version) {
        return String.format("plib%d%s", version, PLUGIN_PROMPT_LIBRARY_EXTENSION);
    }

    public static void mergeToProperties(Properties target, Map<String, Object> source) {
        if (source == null) {
            return;
        }
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                target.put(key, value.toString());
            }
        });
    }

}
