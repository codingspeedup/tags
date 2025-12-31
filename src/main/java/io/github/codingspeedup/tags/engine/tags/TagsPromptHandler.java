package io.github.codingspeedup.tags.engine.tags;

import com.intellij.openapi.progress.ProgressIndicator;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import io.github.codingspeedup.tags.engine.core.*;
import io.github.codingspeedup.tags.integration.LLM;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TagsPromptHandler implements PromptHandler {

    private record BufferContent(String content, int offset) {
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

    public Optional<TagsResult> process(ProgressIndicator indicator) {
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

        var tagsResult = new TagsResult();
        tagsResult.setGateway(resolveGateway(templateBlock.getGateway()));
        switch (tagsResult.getGateway()) {
            case CONTENT: {
                var bc = buildNewContent(parseSectionName(templateBlock.getGateway()), chatResponse);
                tagsResult.setContent(bc.content());
                tagsResult.setStartOffset(bc.offset());
                tagsResult.setEndOffset(bc.offset());
                break;
            }
            case BUFFER: {
                var bc = buildBuffer(chatRequest, chatResponse);
                tagsResult.setBufferName(fileName + ".md");
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


    private ChatRequest compileLlmRequest(TemplateBlock template) {
        var userTemplate = PromptTemplate.from(template.getTemplate());

        var arguments = new HashMap<String, Object>();
        template.getArguments().stringPropertyNames()
                .forEach(key -> arguments.put(key, resolveArgument(template.getArguments().getProperty(key))));

        var userVariables = PromptUtl.findVariables(userTemplate.template());
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
        if (StringUtils.endsWithIgnoreCase("buffer", gateway)) {
            return ActionResultGateway.BUFFER;
        }
        return ActionResultGateway.CLIPBOARD;
    }

    private String resolveArgument(String value) {
        value = value.trim();
        if (value.startsWith("#")) {
            value = parseSectionName(value);
            var sectionBlock = getContentSections().get(value);
            if (sectionBlock != null) {
                value = fileContent.substring(sectionBlock.getFromOffset(), sectionBlock.getToOffset());
            }
        }
        return value;
    }

    private static String parseSectionName(String value) {
        return value.substring(1).trim();
    }

    private BufferContent buildBuffer(ChatRequest chatRequest, ChatResponse chatResponse) {
        var bufferContent = new StringBuilder();
        chatRequest.messages().forEach(message -> {
            switch (message.type()) {
                case SYSTEM -> bufferContent.append(PromptUtl.getSystemBlock(message.toString()));
                case USER -> bufferContent.append(PromptUtl.getUserBlock(message.toString()));
            }
        });
        var bufferOffset = bufferContent.length() + 1;
        bufferContent.append(PromptUtl.getAiBlock(chatResponse.aiMessage().text()));
        return new BufferContent(bufferContent.toString(), bufferOffset);
    }

    private BufferContent buildNewContent(String sectionName, ChatResponse chatResponse) {
        var sectionBlock = getContentSections().get(sectionName);
        return new BufferContent(fileContent, fileOffset);
    }

}
