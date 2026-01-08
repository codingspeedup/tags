package io.github.codingspeedup.tags.engine;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import io.github.codingspeedup.tags.integration.llms.LLM;
import io.github.codingspeedup.tags.prompting.plib.PromptRef;
import io.github.codingspeedup.tags.prompting.tags.TemplateModel;
import io.github.codingspeedup.tags.prompting.chat.PromptUtl;
import io.github.codingspeedup.tags.prompting.tools.PromptApiSpecBuilder;
import io.github.codingspeedup.tags.prompting.tags.SectionBlock;
import io.github.codingspeedup.tags.prompting.tags.TemplateBlock;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.github.codingspeedup.tags.prompting.chat.ChatMdUtl.*;
import static io.github.codingspeedup.tags.prompting.tags.TemplateModel.SECTION_REF_MARKER;
import static io.github.codingspeedup.tags.prompting.chat.PromptUtl.fillArguments;
import static io.github.codingspeedup.tags.prompting.chat.PromptUtl.toProperties;

public class TagsActionHandler implements ActionHandler {

    private record Buffer(String content, int offset) {
    }

    private final String fileName;
    private final String fileContent;
    private final int fileOffset;
    private final TemplateModel ftModel;
    private Map<String, SectionBlock> contentSections;

    public TagsActionHandler(String fileName, String fileContent, int fileOffset) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.fileOffset = fileOffset;
        this.ftModel = TemplateModel.of(fileName).orElseThrow();
    }

    private Map<String, SectionBlock> getContentSections() {
        if (contentSections == null) {
            contentSections = ftModel.getSections(fileContent);
        }
        return contentSections;
    }

    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var blocks = ftModel.identifyTemplates(fileContent);
        if (blocks.isEmpty()) {
            return Optional.empty();
        }

        var templateBlock = blocks.stream()
                .filter(t -> t.contains(fileOffset))
                .findFirst()
                .orElse(blocks.get(0));
        ftModel.fillTemplate(templateBlock, fileContent);

        var chatRequest = compileLlmRequest(project, templateBlock).orElseThrow();
        var apiSpec = PromptApiSpecBuilder.of(project, templateBlock.getPlus().lines().toList());
        var chatResponse = LLM.doChat(chatRequest, apiSpec.orElse(null));

        var tagsResult = new TagsResult(resolveGateway(templateBlock.getGateway()));
        switch (tagsResult.getGateway()) {

            case CLIPBOARD: {
                tagsResult.setContent(chatResponse.aiMessage().text());
                break;
            }

            case CONTENT: {
                var bc = buildNewContent(parseSectionName(templateBlock.getGateway()), chatResponse);
                tagsResult.setContent(bc.content());
                tagsResult.setStartOffset(bc.offset());
                tagsResult.setEndOffset(bc.offset());
                break;
            }

            case CHAT:
            default: {
                var bc = buildChatBuffer(chatRequest, chatResponse);
                tagsResult.setBufferName(nextChatMdBufferName(project, fileName));
                tagsResult.setContent(bc.content());
                tagsResult.setStartOffset(bc.offset());
                tagsResult.setEndOffset(bc.offset());
            }

        }
        return Optional.of(tagsResult);
    }

    private Optional<ChatRequest> compileLlmRequest(Project project, TemplateBlock templateBlock) {
        var templateText = templateBlock.getTemplate();
        if (StringUtils.isBlank(templateText)) {
            return Optional.empty();
        }

        var templateArgs = new HashMap<String, Object>();
        templateBlock.getArguments().stringPropertyNames()
                .forEach(key ->
                        templateArgs.put(key, resolveArgument(templateBlock.getArguments().getProperty(key))));

        var chatRequestBuilder = ChatRequest.builder();

        var chatMessages = new ArrayList<ChatMessage>();

        if (templateText.startsWith(TemplateModel.TEMPLATE_PREFIX)) {

            var pDesc = new PromptRef(templateText);
            var pLib = pDesc.getLibrary(project);

            var promptVariables = pLib.getVariables(pDesc.getId());
            fillArguments(templateArgs, promptVariables);

            var systemTemplate = pLib.getSystemTemplate();
            if (StringUtils.isNotBlank(systemTemplate.template())) {
                chatMessages.add(systemTemplate.apply(templateArgs).toSystemMessage());
            }

            var userTemplate = pLib.getPromptTemplate(pDesc.getId());
            chatMessages.add(userTemplate.apply(templateArgs).toUserMessage());

        } else {

            var userTemplate = PromptTemplate.from(templateBlock.getTemplate());

            var promptVariables = PromptUtl.findVariables(userTemplate);
            fillArguments(templateArgs, promptVariables);

            chatMessages.add(userTemplate.apply(templateArgs).toUserMessage());

        }

        return Optional.of(chatRequestBuilder.messages(chatMessages).build());
    }

    private ActionResultGateway resolveGateway(String gateway) {
        gateway = StringUtils.trimToEmpty(gateway);
        if (gateway.startsWith(SECTION_REF_MARKER)) {
            return ActionResultGateway.CONTENT;
        }
        if (StringUtils.equalsIgnoreCase(ActionResultGateway.CLIPBOARD.name(), gateway)) {
            return ActionResultGateway.CLIPBOARD;
        }
        return ActionResultGateway.CHAT;
    }

    private String resolveArgument(String value) {
        value = value.trim();
        if (value.startsWith(SECTION_REF_MARKER)) {
            value = parseSectionName(value);
            var sectionBlock = getContentSections().get(value);
            if (sectionBlock != null) {
                value = sectionBlock.getContent(fileContent);
            }
        }
        return value;
    }

    private static String parseSectionName(String value) {
        return value.substring(1).trim();
    }

    private Buffer buildChatBuffer(ChatRequest chatRequest, ChatResponse chatResponse) {
        var mdContent = new StringBuilder();
        mdContent.append(renderParametersBlock(toProperties(chatRequest.parameters(), false)));
        chatRequest.messages().forEach(message -> {
            switch (message.type()) {
                case SYSTEM -> mdContent.append(renderSystemBlock((SystemMessage) message));
                case USER -> mdContent.append(renderUserBlock((UserMessage) message));
            }
        });
        var bufferOffset = mdContent.length() + 1;
        mdContent.append(renderAiBlock(chatResponse.aiMessage().text()));
        mdContent.append(renderUserBlock(StringUtils.EMPTY));
        return new Buffer(mdContent.toString(), bufferOffset);
    }

    private Buffer buildNewContent(String sectionName, ChatResponse chatResponse) {
        var sectionBlock = getContentSections().get(sectionName);
        var sectionStart = TemplateModel.indexOfEol(fileContent, sectionBlock.getFromOffset());
        var sectionEnd = TemplateModel.indexOfBol(fileContent, sectionBlock.getToOffset());

        @SuppressWarnings("all")
        var bufferContent = new StringBuilder();
        bufferContent.append(fileContent, 0, sectionStart);
        bufferContent.append("\n").append(chatResponse.aiMessage().text()).append("\n");
        bufferContent.append(fileContent, sectionEnd, fileContent.length());

        return new Buffer(bufferContent.toString(), sectionStart + 1);
    }

}
