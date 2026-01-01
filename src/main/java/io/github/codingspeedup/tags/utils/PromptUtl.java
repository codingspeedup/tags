package io.github.codingspeedup.tags.utils;

import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final Pattern LLM_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

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
        if (!writer.isEmpty()) {
            writer.setLength(writer.length() - 1);
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

    @SneakyThrows
    public static ChatRequestParameters buildChatRequestParameters(Properties properties) {
        var llmParameters = ChatRequestParameters.builder();
        for (var parameterName : LLM_PARAMETERS_NAMES) {
            var propertyValue = properties.get(parameterName);
            if (propertyValue == null) {
                continue;
            }
            for (var method : llmParameters.getClass().getMethods()) {
                if (StringUtils.equals(method.getName(), parameterName) && method.getParameterCount() == 1) {
                    var parameter = method.getParameters()[0];
                    var parameterValue = convert(propertyValue, parameter.getType());
                    if (parameterValue != null) {
                        method.invoke(llmParameters, parameterValue);
                    }
                }
            }
        }
        return llmParameters.build();
    }

    private static Object convert(@NotNull Object propertyValue, Class<?> parameterType) {
        if (parameterType == String.class) {
            return StringUtils.trimToNull(String.valueOf(propertyValue));
        }
        try {
            if (Number.class.isAssignableFrom(parameterType)) {
                var numericValue = (propertyValue instanceof Number n) ? n : new BigDecimal(String.valueOf(propertyValue));
                if (parameterType == Integer.class) {
                    return numericValue.intValue();
                }
                if (parameterType == Double.class) {
                    return numericValue.doubleValue();
                }
                return null;
            }

            if (List.class.isAssignableFrom(parameterType)) {
                if (propertyValue instanceof List<?> listValue) {
                    return listValue.stream().map(String::valueOf).toList();
                }
                return Arrays.stream(String.valueOf(propertyValue).split(","))
                        .map(StringUtils::trimToEmpty)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
            }

            if (parameterType == ToolChoice.class) {
                if (propertyValue instanceof ToolChoice toolChoiceValue) {
                    return toolChoiceValue;
                }
                return ToolChoice.valueOf(String.valueOf(propertyValue).toUpperCase(Locale.ROOT));
            }

            if (parameterType == ResponseFormat.class) {
                var stringValue = String.valueOf(propertyValue).toUpperCase(Locale.ROOT);
                if ("JSON".equals(stringValue)) {
                    return ResponseFormat.JSON;
                }
                return ResponseFormat.TEXT;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }


    public static Set<String> findVariables(PromptTemplate promptTemplate) {
        var userVariables = new LinkedHashSet<String>();
        var matcher = LLM_TEMPLATE_VARIABLE_PATTERN.matcher(promptTemplate.template());
        while (matcher.find()) {
            userVariables.add(matcher.group(1).trim());
        }
        return userVariables;
    }

    public static StringBuilder startChatMd(PromptLibrary promptLibrary) {
        var chatMd = new StringBuilder();
        chatMd.append(PromptUtl.renderParametersBlock(promptLibrary.getParameters()));
        chatMd.append(PromptUtl.renderSystemBlock(promptLibrary.getSystem().template()));
        return chatMd;
    }
}
