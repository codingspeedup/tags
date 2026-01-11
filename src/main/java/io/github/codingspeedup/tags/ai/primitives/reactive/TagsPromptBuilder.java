package io.github.codingspeedup.tags.ai.primitives.reactive;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import io.github.codingspeedup.tags.ai.boundary.PromptLibraryProvider;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.TagPlusModel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

@RequiredArgsConstructor
@SuppressWarnings({"UnusedReturnValue"})
public class TagsPromptBuilder {

    private final PromptLibraryProvider promptLibraryProvider;

    private final Properties llmParameters = new Properties();
    private final List<Object> systemMessages = new ArrayList<>();
    private final List<Object> chatMessages = new ArrayList<>();
    private final Map<String, Object> contextArgs = new HashMap<>();
    private final List<String> toolbox = new ArrayList<>();

    public TagsPromptBuilder tagPlus(TagPlusModel tagPlus) {
        var t = StringUtils.trimToEmpty(tagPlus.getTemplate());
        if (StringUtils.isNotEmpty(t)) {
            if (t.startsWith(BufferModel.PROMPT_REF_PREFIX)) {
                var pRef = new PromptRef(t);
                var pLib = promptLibraryProvider.load(pRef).orElseThrow();
                llmParameters(pLib.getParameters());
                systemTemplate(pLib.getSystemTemplate());
                userTemplate(pLib.getPromptTemplate(pRef.getId()));
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
            this.chatMessages.add(template);
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
            this.chatMessages.add(message);
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
//        var toolsApiDescription = ToolboxApiSpecBuilder.of(project, toolbox).orElse(null);
//
//        var resolvedArgs = new HashMap<String, Object>();
//        contextArgs.forEach((key, value) -> resolvedArgs.put(key, resolveArgument(value)));
//
//        var modelName = llmParameters.getProperty(LLM_PARAM_MODEL_NAME);
//        var model = Model.of(modelName).orElseThrow();
//        llmParameters.setProperty(LLM_PARAM_MODEL_NAME, model.name());

        return null;
    }

    private Object resolveArgument(Object rawValue) {
//        if (rawValue instanceof String value) {
//            value = value.trim();
//            if (value.startsWith(SECTION_REF_MARKER)) {
//                value = parseSectionName(value);
//                var sectionBlock = getContentSections().get(value);
//                Optional.ofNullable(sectionBlock).orElseThrow();
//                value = sectionBlock.getContent(fileContent);
//            } else if (value.startsWith(FILE_REF_MARKER)) {
//                value = value.substring(FILE_REF_MARKER.length());
//
//                var sectionName = StringUtils.EMPTY;
//                var sectionIndex = value.indexOf(SECTION_REF_MARKER);
//                if (sectionIndex >= 0) {
//                    sectionName = parseSectionName(value.substring(sectionIndex));
//                    value = value.substring(0, sectionIndex);
//                }
//
//                var linesSelection = StringUtils.EMPTY;
//                var linesSelectionIndex = value.indexOf(LINES_REF_MARKER);
//                if (linesSelectionIndex >= 0) {
//                    linesSelection = value.substring(linesSelectionIndex + LINES_REF_MARKER.length());
//                    value = value.substring(0, linesSelectionIndex);
//                }
//
//                value = collectFileSelection(project, value.trim(), linesSelection, sectionName);
//            }
//        }
        return rawValue;
    }


}
