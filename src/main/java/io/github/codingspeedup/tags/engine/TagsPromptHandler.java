package io.github.codingspeedup.tags.engine;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import io.github.codingspeedup.tags.integration.LLM;
import io.github.codingspeedup.tags.utils.*;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.github.codingspeedup.tags.utils.ChatUtl.*;
import static io.github.codingspeedup.tags.utils.PromptUtl.toProperties;

public class TagsPromptHandler implements PromptHandler {

    private record Buffer(String content, int offset) {
    }

    private final String fileName;
    private final String fileContent;
    private final int fileOffset;
    private final FileTypeModel ftModel;
    private Map<String, SectionBlock> contentSections;

    public TagsPromptHandler(String fileName, String fileContent, int fileOffset) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.fileOffset = fileOffset;
        this.ftModel = FileTypeModel.of(fileName).orElseThrow();
    }

    private Map<String, SectionBlock> getContentSections() {
        if (contentSections == null) {
            contentSections = ftModel.getSections(fileContent);
        }
        return contentSections;
    }

    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var block = ftModel.identifyTemplates(fileContent).stream()
                .filter(t -> t.contains(fileOffset))
                .findFirst();
        if (block.isEmpty()) {
            return Optional.empty();
        }

        var templateBlock = block.get();
        ftModel.fillTemplate(templateBlock, fileContent);

        var chatRequest = compileLlmRequest(templateBlock);
        var chatResponse = LLM.doChat(chatRequest);

        var tagsResult = new TagsResult(resolveGateway(templateBlock.getGateway()));
        switch (tagsResult.getGateway()) {
            case CHAT: {
                var bc = buildChatBuffer(chatRequest, chatResponse);
                tagsResult.setBufferName(nextBufferName(project, fileName));
                tagsResult.setContent(bc.content());
                tagsResult.setStartOffset(bc.offset());
                tagsResult.setEndOffset(bc.offset());
                break;
            }
            case CONTENT: {
                var bc = buildNewContent(parseSectionName(templateBlock.getGateway()), chatResponse);
                tagsResult.setContent(bc.content());
                tagsResult.setStartOffset(bc.offset());
                tagsResult.setEndOffset(bc.offset());
                break;
            }
            case CLIPBOARD:
            default: {
                tagsResult.setContent(chatResponse.aiMessage().text());
            }
        }
        return Optional.of(tagsResult);
    }


    private ChatRequest compileLlmRequest(TemplateBlock templateBlock) {
        var userTemplate = PromptTemplate.from(templateBlock.getTemplate());

        var arguments = new HashMap<String, Object>();
        templateBlock.getArguments().stringPropertyNames()
                .forEach(key -> arguments.put(key, resolveArgument(templateBlock.getArguments().getProperty(key))));

        var userVariables = PromptUtl.findVariables(userTemplate);
        userVariables.forEach(key -> {
            if (!arguments.containsKey(key)) {
                arguments.put(key, "∅");
            }
        });

        var userMessage = userTemplate.apply(arguments).toUserMessage();
        return ChatRequest.builder().messages(userMessage).build();
    }

    private ActionResultGateway resolveGateway(String gateway) {
        gateway = StringUtils.trimToEmpty(gateway);
        if (gateway.startsWith("#")) {
            return ActionResultGateway.CONTENT;
        }
        if (StringUtils.endsWithIgnoreCase(ActionResultGateway.CHAT.name(), gateway)) {
            return ActionResultGateway.CHAT;
        }
        return ActionResultGateway.CLIPBOARD;
    }

    private String resolveArgument(String value) {
        value = value.trim();
        if (value.startsWith("#")) {
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
        return new Buffer(mdContent.toString(), bufferOffset);
    }

    private Buffer buildNewContent(String sectionName, ChatResponse chatResponse) {
        var sectionBlock = getContentSections().get(sectionName);
        var sectionStart = FileTypeModel.indexOfEol(fileContent, sectionBlock.getFromOffset());
        var sectionEnd = FileTypeModel.indexOfBol(fileContent, sectionBlock.getToOffset());

        @SuppressWarnings("all")
        var bufferContent = new StringBuilder();
        bufferContent.append(fileContent, 0, sectionStart);
        bufferContent.append("\n").append(chatResponse.aiMessage().text()).append("\n");
        bufferContent.append(fileContent, sectionEnd, fileContent.length());

        return new Buffer(bufferContent.toString(), sectionStart + 1);
    }

}
