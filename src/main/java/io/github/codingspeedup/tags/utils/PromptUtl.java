package io.github.codingspeedup.tags.utils;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.codingspeedup.tags.utils.PromptDesc.VAR_PLACEHOLDER;

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
    public static ChatRequestParameters toChatRequestParameters(Properties properties) {
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
        if (parameters != null) {
            Object value = parameters.modelName();
            if (withModelName && value != null) {
                properties.put("modelName", value);
            }

            value = parameters.temperature();
            if (value != null) {
                properties.put("temperature", value);
            }

            value = parameters.topP();
            if (value != null) {
                properties.put("topP", value);
            }

            value = parameters.topK();
            if (value != null) {
                properties.put("topK", value);
            }

            value = parameters.frequencyPenalty();
            if (value != null) {
                properties.put("frequencyPenalty", value);
            }

            value = parameters.maxOutputTokens();
            if (value != null) {
                properties.put("maxOutputTokens", value);
            }

            var stopSequences = parameters.stopSequences();
            if (CollectionUtils.isNotEmpty(stopSequences)) {
                properties.put("stopSequences", String.join(",", stopSequences));
            }

            value = parameters.toolChoice();
            if (value != null) {
                properties.put("responseFormat", String.valueOf(value));
            }

            var responseFormat = parameters.responseFormat();
            if (responseFormat != null) {
                properties.put("responseFormat", responseFormat.type().name());
            }
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

    public static void fillArguments(HashMap<String, Object> arguments, Set<String> requiredVariables) {
        requiredVariables.forEach(key -> {
            if (!arguments.containsKey(key)) {
                arguments.put(key, VAR_PLACEHOLDER);
            }
        });
    }

    public static Optional<List<ToolSpecification>> buildToolSpec(String toolkitClassFQN) {
        toolkitClassFQN = StringUtils.trimToEmpty(toolkitClassFQN);
        if (StringUtils.isEmpty(toolkitClassFQN) || toolkitClassFQN.equals(VAR_PLACEHOLDER)) {
            return Optional.of(List.of());
        }
        try {
            var classWithTools = Class.forName(toolkitClassFQN);
            return buildToolSpec(classWithTools);
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    public static Optional<List<ToolSpecification>> buildToolSpec(Class<?> classWithTools) {
        var toolSpecifications = ToolSpecifications.toolSpecificationsFrom(classWithTools);
        return CollectionUtils.isEmpty(toolSpecifications) ? Optional.empty() : Optional.of(toolSpecifications);
    }

}
