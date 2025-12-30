package io.github.codingspeedup.tags.engine.chatmd;

import com.intellij.openapi.progress.ProgressIndicator;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.output.FinishReason;
import io.github.codingspeedup.tags.engine.core.GenerationResponse;
import io.github.codingspeedup.tags.engine.core.GenerationSink;
import io.github.codingspeedup.tags.engine.core.PromptHandler;
import io.github.codingspeedup.tags.engine.core.PromptUtl;
import io.github.codingspeedup.tags.integration.LLM;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.codingspeedup.tags.engine.chatmd.ChatMdUtl.CHAT_MD_EXTENSION;
import static io.github.codingspeedup.tags.engine.chatmd.ChatMdUtl.PARAMETERS_BLOCK_INFO;
import static io.github.codingspeedup.tags.engine.core.PromptUtl.*;

public class ChatMdHandler implements PromptHandler {

    private final String mdContent;
    private final int mdOffset;

    public ChatMdHandler(String mdContent, int mdOffset) {
        this.mdContent = mdContent;
        this.mdOffset = mdOffset;
    }

    public Optional<GenerationResponse> process(ProgressIndicator indicator) {
        var options = new MutableDataSet();
        var parser = Parser.builder(options).build();
        var document = parser.parse(mdContent);

        List<FencedCodeBlock> parametersBlocks = new ArrayList<>();
        List<FencedCodeBlock> systemBlocks = new ArrayList<>();
        List<FencedCodeBlock> userBlocks = new ArrayList<>();

        for (var node : document.getChildren()) {
            if (node instanceof FencedCodeBlock codeBlock) {
                var info = codeBlock.getInfo().toString();
                switch (info) {
                    case PARAMETERS_BLOCK_INFO -> parametersBlocks.add(codeBlock);
                    case SYSTEM_BLOCK_INFO -> systemBlocks.add(codeBlock);
                    case USER_BLOCK_INFO -> userBlocks.add(codeBlock);
                }
            }
            if (indicator.isCanceled()) {
                return Optional.empty();
            }
        }

        userBlocks = userBlocks.stream()
                .filter(block -> StringUtils.isNotBlank(getContent(block)))
                .toList();
        var userBlockIndex = findUserBlock(userBlocks, mdOffset);
        if (userBlockIndex < 0) {
            return Optional.empty();
        }
        var userBlock = userBlocks.get(userBlockIndex);
        var userMessage = UserMessage.from(CHAT_MD_EXTENSION, getContent(userBlock));

        var system = systemBlocks.stream()
                .map(this::getContent)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(SystemMessage::from);

        var llmParameters = parametersBlocks.stream()
                .map(this::getContent)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(this::collectParameters)
                .orElse(ChatRequestParameters.builder().build());

        var llmResponse = system.map(systemMessage -> LLM.chat(llmParameters, systemMessage, userMessage))
                .orElse(LLM.chat(llmParameters, userMessage));

        if (llmResponse.metadata().finishReason() == FinishReason.OTHER) {
            return Optional.empty();
        }

        var assistantMessage = PromptUtl.getAssistantBlock(StringUtils.trimToEmpty(llmResponse.aiMessage().text()));
        var insertionOffset = userBlock.getEndOffset() + 1;

        var hasFooter = false;
        var contentLen = mdContent.length();
        for (var i = userBlock.getEndOffset(); i < contentLen; i++) {
            if (!Character.isWhitespace(mdContent.charAt(i))) {
                hasFooter = true;
                break;
            }
        }
        var additionalUserBlock = hasFooter ? StringUtils.EMPTY : PromptUtl.getUserBlock(null);

        @SuppressWarnings("all")
        var newContent = new StringBuilder(contentLen + assistantMessage.length() + additionalUserBlock.length());
        newContent.append(mdContent, 0, insertionOffset);
        newContent.append(assistantMessage);
        newContent.append(mdContent, insertionOffset, contentLen);
        newContent.append(additionalUserBlock);

        var gr = new GenerationResponse();
        gr.setOutputChannel(GenerationSink.REPLACE_CONTENT);
        gr.setGeneratedContent(newContent.toString());
        gr.setStartOffset(insertionOffset);
        gr.setEndOffset(insertionOffset);
        return Optional.of(gr);
    }

    private String getContent(FencedCodeBlock fcb) {
        var content = fcb.getContentLines().stream()
                .map(line -> StringUtils.trimToEmpty(line.toString()))
                .collect(Collectors.joining("\n"));
        return StringUtils.trimToEmpty(content);
    }

    private int findUserBlock(List<FencedCodeBlock> blocks, int offset) {
        if (CollectionUtils.isEmpty(blocks)) {
            return -1;
        }
        if (blocks.size() == 1) {
            return 0;
        }
        if (offset < blocks.get(0).getStartOffset()) {
            return blocks.size() - 1;
        }
        for (var i = 0; i < blocks.size(); i++) {
            var block = blocks.get(i);
            if (offset < block.getStartOffset()) {
                return i - 1;
            }
            if (offset <= block.getEndOffset()) {
                return i;
            }
        }
        return blocks.size() - 1;
    }


    @SneakyThrows
    private ChatRequestParameters collectParameters(String data) {
        var properties = new Properties();
        try (var reader = new StringReader(data)) {
            properties.load(reader);
        }
        var llmParameters = ChatRequestParameters.builder();
        for (var parameterName : LLM_PARAMETERS_NAMES) {
            var propertyValue = properties.getProperty(parameterName);
            if (StringUtils.isNotBlank(propertyValue)) {
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
        }
        return llmParameters.build();
    }

    private Object convert(String propertyValue, Class<?> parameterType) {
        if (parameterType == String.class) {
            return propertyValue;
        }

        try {
            propertyValue = StringUtils.trimToEmpty(propertyValue);

            if (Number.class.isAssignableFrom(parameterType)) {
                var numericValue = new BigDecimal(propertyValue);
                if (parameterType == Integer.class) {
                    return numericValue.intValue();
                }
                if (parameterType == Double.class) {
                    return numericValue.doubleValue();
                }
            }

            if (List.class.isAssignableFrom(parameterType)) {
                return Arrays.stream(propertyValue.split(","))
                        .map(StringUtils::trimToEmpty)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
            }

            propertyValue = propertyValue.toUpperCase(Locale.ROOT);

            if (parameterType == ToolChoice.class) {
                return ToolChoice.valueOf(propertyValue);
            }

            if (parameterType == ResponseFormat.class) {
                if ("JSON".equals(propertyValue)) {
                    return ResponseFormat.JSON;
                }
                return ResponseFormat.TEXT;
            }

        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

}
