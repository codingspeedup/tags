package io.github.codingspeedup.tags.engine.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromptUtl {

    public static final String SYSTEM_BLOCK_INFO = "llm-system-message";
    public static final String USER_BLOCK_INFO = "llm-user-message";


    public static final List<String> LLM_PARAMETERS_NAMES = List.of(
            "modelName",
            "temperature",
            "topP",
            "topK",
            "frequencyPenalty",
            "presencePenalty",
            "maxOutputTokens",
            "stopSequences",
            "toolChoice",
            "responseFormat"
    );

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getDefaultSystemMessage() {
        return """
                Act as a senior engineer providing high-density, technically accurate info without fluff or polite filler.
                Prioritize immediate Markdown code blocks and use minimal prose only for non-obvious logic.
                """;
    }

    public static String getCurrentTimeIso() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static String getSystemBlock(String message) {
        return String.format("""
                #### ⚙️ Intention
                ```%s
                %s
                ```
                """, SYSTEM_BLOCK_INFO, StringUtils.trimToEmpty(message));
    }

    public static String getUserBlock(String message) {
        return String.format("""
                ### 👤 User
                ```%s
                %s
                ```
                """, USER_BLOCK_INFO, StringUtils.trimToEmpty(message));
    }

    public static String getAiBlock(String message) {
        return String.format("""
                
                #### 🤖 AI: %s
                
                ---
                %s
                """, getCurrentTimeIso(), message);
    }

    @SneakyThrows
    public static Properties parseProperties(String data) {
        var properties = new Properties();
        try (var reader = new StringReader(data)) {
            properties.load(reader);
        }
        return properties;
    }

    private static final Pattern LLM_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    public static Set<String> findVariables(String messageTemplate) {
        var userVariables = new HashSet<String>();
        var matcher = LLM_TEMPLATE_VARIABLE_PATTERN.matcher(messageTemplate);
        while (matcher.find()) {
            // .trim() handles spaces like {{ name }} vs {{name}}
            userVariables.add(matcher.group(1).trim());
        }
        return userVariables;
    }

}
