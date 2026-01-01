package io.github.codingspeedup.tags.engine.core;

import com.intellij.openapi.project.Project;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromptUtl {

    public static final String PARAMETERS_BLOCK_INFO = "llm-parameters";
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

    public static Pair<Properties, String> getDefaultPromptContext(Project project) {
        var pLib = PromptLibrary.of(project, TagsUtl.resolvePromptLibrary(project).orElseThrow());
        return Pair.of(pLib.getParameters(), pLib.getSystem().template());
    }

    public static String getCurrentTimeIso() {
        return LocalDateTime.now().format(FORMATTER);
    }


    public static String renderParametersBlock(Properties parameters) {
        var writer = new StringBuilder();
        if (parameters != null) {
            parameters.forEach((key, value) ->
                    writer.append(key).append("=").append(value).append("\n")
            );
        }
        return String.format("""
                #### 🛠️ parameters
                ```%s
                %s
                ```
                """, PARAMETERS_BLOCK_INFO, writer);
    }

    public static String renderSystemBlock(String message) {
        return String.format("""
                #### ⚙️ Intention
                ```%s
                %s
                ```
                """, SYSTEM_BLOCK_INFO, StringUtils.trimToEmpty(message));
    }

    public static String renderUserBlock(String message) {
        return String.format("""
                ### 👤 User
                ```%s
                %s
                ```
                """, USER_BLOCK_INFO, StringUtils.trimToEmpty(message));
    }

    public static String renderAiBlock(String message) {
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

    public static Set<String> findVariables(PromptTemplate promptTemplate) {
        var userVariables = new LinkedHashSet<String>();
        var matcher = LLM_TEMPLATE_VARIABLE_PATTERN.matcher(promptTemplate.template());
        while (matcher.find()) {
            userVariables.add(matcher.group(1).trim());
        }
        return userVariables;
    }

}
