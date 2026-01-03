package io.github.codingspeedup.tags.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.*;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.invocation.LangChain4jManaged;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.input.PromptTemplate;
import io.github.codingspeedup.tags.tools.ToolsPackageMaker;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.isNullOrBlank;
import static io.github.codingspeedup.tags.utils.PromptDesc.VAR_PLACEHOLDER;
import static java.util.Arrays.stream;

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
    private static final ObjectMapper SAFE_MAPPER = new ObjectMapper();

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

    public static Optional<List<ToolSpecification>> buildToolSpec(String toolkitName) {
        try {
            var toolkitClassFQN = ToolsPackageMaker.class.getPackageName() + "." + toolkitName;
            var classWithTools = Class.forName(toolkitClassFQN);
            var toolSpecifications = stream(classWithTools.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(Tool.class))
                    .map(PromptUtl::toolSpecificationFrom)
                    .toList();
            ToolSpecifications.validateSpecifications(toolSpecifications);
            return CollectionUtils.isEmpty(toolSpecifications) ? Optional.empty() : Optional.of(toolSpecifications);
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    public static ToolSpecification toolSpecificationFrom(Method method) {
        Tool tool = method.getAnnotation(Tool.class);
        return ToolSpecification.builder()
                .name(getName(tool, method))
                .description(getDescription(tool))
                .parameters(parametersFrom(method.getParameters()))
                .metadata(getMetadata(tool))
                .build();
    }

    private static String getName(Tool tool, Method method) {
        return isNullOrBlank(tool.name()) ? method.getName() : tool.name();
    }

    private static String getDescription(Tool tool) {
        String description = String.join("\n", tool.value());
        return description.isEmpty() ? null : description;
    }

    private static Map<String, Object> getMetadata(Tool annotation) {
        var metadataJson = annotation.metadata();
        if (isNullOrBlank(metadataJson)) {
            return Collections.emptyMap();
        }
        try {
            var typeRef = new TypeReference<Map<String, Object>>() {
            };
            return SAFE_MAPPER.readValue(metadataJson, typeRef);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static JsonObjectSchema parametersFrom(Parameter[] parameters) {

        Map<String, JsonSchemaElement> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        Map<Class<?>, JsonSchemaElementUtils.VisitedClassMetadata> visited = new LinkedHashMap<>();

        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(ToolMemoryId.class)
                    || InvocationParameters.class.isAssignableFrom(parameter.getType())
                    || LangChain4jManaged.class.isAssignableFrom(parameter.getType())
                    || parameter.getType() == InvocationContext.class) {
                continue;
            }

            boolean isRequired = Optional.ofNullable(parameter.getAnnotation(P.class))
                    .map(P::required)
                    .orElse(true);

            properties.put(parameter.getName(), jsonSchemaElementFrom(parameter, visited));
            if (isRequired) {
                required.add(parameter.getName());
            }
        }

        Map<String, JsonSchemaElement> definitions = new LinkedHashMap<>();
        visited.forEach((clazz, visitedClassMetadata) -> {
            if (visitedClassMetadata.recursionDetected) {
                definitions.put(visitedClassMetadata.reference, visitedClassMetadata.jsonSchemaElement);
            }
        });

        if (properties.isEmpty()) {
            return null;
        }

        return JsonObjectSchema.builder()
                .addProperties(properties)
                .required(required)
                .definitions(definitions.isEmpty() ? null : definitions)
                .build();
    }

    private static JsonSchemaElement jsonSchemaElementFrom(Parameter parameter,
                                                           Map<Class<?>, JsonSchemaElementUtils.VisitedClassMetadata> visited) {
        P annotation = parameter.getAnnotation(P.class);
        String description = annotation == null ? null : annotation.value();
        return JsonSchemaElementUtils.jsonSchemaElementFrom(
                parameter.getType(),
                parameter.getParameterizedType(),
                description,
                true,
                visited
        );
    }

}
