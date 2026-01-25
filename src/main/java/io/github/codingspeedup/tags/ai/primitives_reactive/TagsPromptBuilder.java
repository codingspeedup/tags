package io.github.codingspeedup.tags.ai.primitives_reactive;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.input.PromptTemplate;
import io.github.codingspeedup.tags.ai.boundary.BufferProvider;
import io.github.codingspeedup.tags.ai.boundary.EnvironmentSettingsProvider;
import io.github.codingspeedup.tags.ai.boundary.PromptLibraryProvider;
import io.github.codingspeedup.tags.ai.boundary.ToolboxSupport;
import io.github.codingspeedup.tags.ai.composition_orchestration.core.BufferModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.core.SectionModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.core.TagPlusModel;
import io.github.codingspeedup.tags.ai.composition_reactive.ToolboxApiSpecBuilder;
import io.github.codingspeedup.tags.ai.primitives_models.Model;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.github.codingspeedup.tags.ai.composition_orchestration.core.BufferModel.*;
import static io.github.codingspeedup.tags.ai.primitives_reactive.PromptUtl.LLM_PARAM_MODEL_NAME;

@RequiredArgsConstructor
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class TagsPromptBuilder {

    private final EnvironmentSettingsProvider settings;
    private final BufferProvider bufferProvider;
    private final PromptLibraryProvider promptLibraryProvider;
    private final ToolboxSupport toolboxSupport;

    private final Properties llmParameters = new Properties();
    private final List<Object> systemMessages = new ArrayList<>();
    private final List<Object> otherMessages = new ArrayList<>();
    private final Map<String, Object> contextArgs = new HashMap<>();
    private final List<String> toolbox = new ArrayList<>();
    private volatile Map<String, SectionModel> sections = Map.of();

    public TagsPromptBuilder tagPlus(TagPlusModel tagPlus) {
        var t = StringUtils.trimToEmpty(tagPlus.getTemplate());
        if (StringUtils.isNotEmpty(t)) {
            if (t.startsWith(BufferModel.PROMPT_REF_PREFIX)) {
                var pRef = new PromptRef(t);
                var pLib = promptLibraryProvider.load(pRef).orElseThrow();
                llmParameters(pLib.getParameters());
                systemTemplate(pLib.getSystemTemplate());
                userTemplate(pLib.getPromptTemplate(pRef.getId()).orElseThrow(
                        () -> new UnsupportedOperationException("Unavailable prompt template `" + pRef.getId() + "'")));
                contextArgs(pLib.getDefaults());
            } else {
                userTemplate(t);
            }
        }

        if (tagPlus.getArguments() != null) {
            tagPlus.getArguments().forEach((key, value) -> contextArgs((String) key, value));
        }

        var plus = StringUtils.trimToEmpty(tagPlus.getPlus());
        plus.lines().filter(StringUtils::isNotEmpty).forEach(this::toolbox);

        return this;
    }

    public TagsPromptBuilder llmParameters(Properties llmParameters) {
        this.llmParameters.putAll(llmParameters);
        return this;
    }

    public TagsPromptBuilder llmParameters(String llmParameters) {
        llmParameters = StringUtils.trimToNull(llmParameters);
        llmParameters(PromptUtl.parseProperties(llmParameters));
        return this;
    }

    public TagsPromptBuilder llmParameter(String key, Object value) {
        this.llmParameters.put(key, value);
        return this;
    }

    public TagsPromptBuilder systemTemplate(PromptTemplate template) {
        if (template != null) {
            this.systemMessages.add(template);
        }
        return this;
    }

    public TagsPromptBuilder systemTemplate(String template) {
        template = StringUtils.trimToEmpty(template);
        if (StringUtils.isNotEmpty(template)) {
            return this.systemTemplate(PromptTemplate.from(template));
        }
        return this;
    }

    public TagsPromptBuilder systemMessage(SystemMessage message) {
        if (message != null) {
            this.systemMessages.add(message);
        }
        return this;
    }

    public TagsPromptBuilder systemMessage(String message) {
        message = StringUtils.trimToEmpty(message);
        if (StringUtils.isNotEmpty(message)) {
            return this.systemMessage(SystemMessage.from(message));
        }
        return this;
    }

    public TagsPromptBuilder userTemplate(PromptTemplate template) {
        if (template != null) {
            this.otherMessages.add(template);
        }
        return this;
    }

    public TagsPromptBuilder userTemplate(String template) {
        template = StringUtils.trimToEmpty(template);
        if (StringUtils.isNotEmpty(template)) {
            return this.userTemplate(PromptTemplate.from(template));
        }
        return this;
    }

    public TagsPromptBuilder userMessage(UserMessage message) {
        if (message != null) {
            this.otherMessages.add(message);
        }
        return this;
    }

    public TagsPromptBuilder userMessage(String message) {
        message = StringUtils.trimToEmpty(message);
        if (StringUtils.isNotEmpty(message)) {
            return this.userMessage(UserMessage.from(message));
        }
        return this;
    }

    public TagsPromptBuilder contextArgs(Map<String, Object> contextArgs) {
        this.contextArgs.putAll(contextArgs);
        return this;
    }

    public TagsPromptBuilder contextArgs(Properties contextArgs) {
        contextArgs.putAll(this.contextArgs);
        return this;
    }

    public TagsPromptBuilder contextArgs(String key, Object value) {
        this.contextArgs.put(key, value);
        return this;
    }

    public TagsPromptBuilder toolbox(Class<?>... tools) {
        if (!ArrayUtils.isEmpty(tools)) {
            Arrays.stream(tools).forEach(this::toolbox);
        }
        return this;
    }

    public TagsPromptBuilder toolbox(String... tools) {
        toolbox.addAll(Arrays.asList(tools));
        return this;
    }

    public TagsPrompt build() {
        BufferModel.of(bufferProvider.getName()).ifPresent(bufferModel
                -> sections = bufferModel.getSections(bufferProvider.getContent()));

        var resolvedArgs = new HashMap<String, Object>();
        contextArgs.forEach((key, value) -> resolvedArgs.put(key, resolveArgument(value)));

        for (int i = 0; i < systemMessages.size(); i++) {
            var systemQuery = systemMessages.get(i);
            if (systemQuery instanceof PromptTemplate systemTemplate) {
                fillArguments(resolvedArgs, PromptUtl.findVariables(systemTemplate));
                systemMessages.set(i, systemTemplate.apply(resolvedArgs).toSystemMessage());
            }
        }

        var unifiedSystemMessage = systemMessages.stream()
                .map(msg -> (SystemMessage) msg)
                .map(SystemMessage::text)
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining("\n\n"));

        var toolsApiDescription = ToolboxApiSpecBuilder.of(toolboxSupport, toolbox).orElse(null);
        if (toolsApiDescription != null) {
            unifiedSystemMessage = """
                    # ROLE
                    You are a Groovy Scripting Engine.
                    Your task is to write a script that calls the PROVIDED_API to fulfill ORIGINAL_USER_REQUEST.
                    
                    # PROVIDED_API:
                    > [!NOTE]
                    > All calls must use the `ClassName.methodName` syntax.
                    %s
                    
                    ---
                    
                    # TARGET OBJECTIVE:
                    Fulfill the following request by writing a Groovy script using the PROVIDED_API.
                    
                    ## ORIGINAL_SYSTEM_INSTRUCTIONS:
                    %s
                    
                    ## ORIGINAL_USER_REQUEST:
                    """.formatted(toolsApiDescription, unifiedSystemMessage);
        }

        for (int i = 0; i < otherMessages.size(); i++) {
            var chatQuery = otherMessages.get(i);
            if (chatQuery instanceof PromptTemplate userTemplate) {
                fillArguments(resolvedArgs, PromptUtl.findVariables(userTemplate));
                otherMessages.set(i, userTemplate.apply(resolvedArgs).toUserMessage());
            }
        }

        var modelName = llmParameters.getProperty(LLM_PARAM_MODEL_NAME);
        var model = Model.of(settings, modelName).orElseThrow();
        llmParameters.setProperty(LLM_PARAM_MODEL_NAME, model.name());

        var chatMessages = new ArrayList<ChatMessage>();
        if (model.isSystemRoleSupported()) {
            chatMessages.add(SystemMessage.from(unifiedSystemMessage));
        } else {
            chatMessages.add(UserMessage.from("system", unifiedSystemMessage));
        }
        otherMessages.stream().map(msg -> (ChatMessage) msg).forEach(chatMessages::add);

        var chatRequest = ChatRequest.builder()
                .parameters(PromptUtl.toChatRequestParameters(llmParameters))
                .messages(chatMessages)
                .build();

        return new TagsPrompt(model.provider(), chatRequest);
    }

    private static void fillArguments(Map<String, Object> arguments, Set<String> requiredVariables) {
        requiredVariables.forEach(key -> {
            if (!arguments.containsKey(key)) {
                arguments.put(key, ARG_PLACEHOLDER);
            }
        });
    }

    private Object resolveArgument(Object rawValue) {
        if (rawValue instanceof String value) {
            value = value.trim();
            if (value.startsWith(SECTION_REF_MARKER)) {

                value = parseSectionName(value);
                var sectionBlock = Optional.ofNullable(sections.get(value)).orElseThrow();
                rawValue = sectionBlock.getContent(bufferProvider.getContent());

            } else if (value.startsWith(FILE_REF_MARKER)) {

                value = value.substring(FILE_REF_MARKER.length());

                var sectionName = StringUtils.EMPTY;
                var sectionIndex = value.indexOf(SECTION_REF_MARKER);
                if (sectionIndex >= 0) {
                    sectionName = parseSectionName(value.substring(sectionIndex));
                    value = value.substring(0, sectionIndex);
                }

                var linesSelection = StringUtils.EMPTY;
                var linesSelectionIndex = value.indexOf(LINES_REF_MARKER);
                if (linesSelectionIndex >= 0) {
                    linesSelection = value.substring(linesSelectionIndex + LINES_REF_MARKER.length());
                    value = value.substring(0, linesSelectionIndex);
                }

                rawValue = collectFileSelection(value.trim(), linesSelection, sectionName);
            }
        }
        return rawValue;
    }

    private String collectFileSelection(String fileRef, String linesSelection, String sectionName) {
        fileRef = fileRef.replace('\\', '/').replaceAll("[ \t]*/[ \t/]*", "/");
        var refBuffer = bufferProvider.resolve(fileRef).orElseThrow();
        var thatFileContent = refBuffer.getContent();

        if (StringUtils.isBlank(linesSelection) && StringUtils.isBlank(sectionName)) {
            return thatFileContent;
        }

        var value = new StringBuilder();

        if (StringUtils.isNotBlank(linesSelection)) {
            var allLines = thatFileContent.lines().toList();

            Arrays.stream(linesSelection.split(LINES_REF_GROUP_SEPARATOR))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .flatMap(spec -> {
                        if (spec.contains(LINES_REF_INTERVAL)) {
                            var boundaries = spec.split(LINES_REF_INTERVAL, 2);
                            var start = Integer.parseInt(boundaries[0].trim());
                            var end = Integer.parseInt(boundaries[1].trim());
                            return IntStream.rangeClosed(start, end).boxed();
                        } else {
                            return Stream.of(Integer.parseInt(spec));
                        }
                    })
                    .forEach(idx -> {
                        var zeroIdx = idx - 1;
                        if (zeroIdx >= 0 && zeroIdx < allLines.size()) {
                            value.append(allLines.get(zeroIdx)).append("\n");
                        }
                    });
        }

        if (StringUtils.isNotBlank(sectionName)) {
            var thatFtModel = BufferModel.of(refBuffer.getName()).orElseThrow();
            var thatFileSectionBlock = thatFtModel.getSections(thatFileContent).get(sectionName);
            Optional.ofNullable(thatFileSectionBlock).orElseThrow();
            value.append(thatFileSectionBlock.getContent(thatFileContent));
        }

        return value.toString().trim();
    }

}
