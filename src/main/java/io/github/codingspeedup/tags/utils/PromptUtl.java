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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromptUtl {

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

    private static final Pattern LLM_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

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

    public static Properties toProperties(ChatRequestParameters parameters, boolean withModelName) {
        var properties = new Properties();

        if (withModelName) {
            properties.put("modelName", parameters.modelName());
        }

        properties.put("temperature", parameters.temperature());
        properties.put("topP", parameters.topP());
        properties.put("topK", parameters.topK());
        properties.put("frequencyPenalty", parameters.frequencyPenalty());
        properties.put("presencePenalty", parameters.presencePenalty());
        properties.put("maxOutputTokens", parameters.maxOutputTokens());

        var stopSequences = parameters.stopSequences();
        if (stopSequences != null) {
            properties.put("stopSequences", String.join(",", stopSequences));
        }

        var toolChoice = parameters.toolChoice();
        if (toolChoice != null) {
            properties.put("responseFormat", toolChoice.name());
        }

        var responseFormat = parameters.responseFormat();
        if (responseFormat != null) {
            properties.put("responseFormat", responseFormat.type().name());
        }

        return properties;
    }

    public static Set<String> findVariables(PromptTemplate promptTemplate) {
        var userVariables = new LinkedHashSet<String>();
        var matcher = LLM_TEMPLATE_VARIABLE_PATTERN.matcher(promptTemplate.template());
        while (matcher.find()) {
            userVariables.add(matcher.group(1).trim());
        }
        return userVariables;
    }

}
