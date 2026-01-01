package io.github.codingspeedup.tags.engine.tags;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.output.FinishReason;
import io.github.codingspeedup.tags.engine.core.ActionResultGateway;
import io.github.codingspeedup.tags.engine.core.PromptHandler;
import io.github.codingspeedup.tags.engine.core.PromptUtl;
import io.github.codingspeedup.tags.engine.core.TagsResult;
import io.github.codingspeedup.tags.integration.LLM;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.codingspeedup.tags.engine.core.ChatMdUtl.CHAT_MD_EXTENSION;
import static io.github.codingspeedup.tags.engine.core.PromptUtl.*;

public class ChatMdPromptHandler implements PromptHandler {

    private final String content;
    private final int contentOffset;

    public ChatMdPromptHandler(String content, int contentOffset) {
        this.content = content;
        this.contentOffset = contentOffset;
    }

    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var options = new MutableDataSet();
        var parser = Parser.builder(options).build();
        var document = parser.parse(content);

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
        var userBlockIndex = findUserBlock(userBlocks, contentOffset);
        if (userBlockIndex < 0) {
            return Optional.empty();
        }
        var userBlock = userBlocks.get(userBlockIndex);
        var userMessage = UserMessage.from(CHAT_MD_EXTENSION, getContent(userBlock));

        var system = extractMessage(systemBlocks, contentOffset)
                .map(SystemMessage::from);

        var llmParameters = parametersBlocks.stream()
                .map(this::getContent)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(this::collectParameters)
                .orElse(ChatRequestParameters.builder().build());

        var llmResponse = system.map(systemMessage -> LLM.doChat(llmParameters, systemMessage, userMessage))
                .orElse(LLM.doChat(llmParameters, userMessage));

        if (llmResponse.metadata().finishReason() == FinishReason.OTHER) {
            return Optional.empty();
        }

        var assistantMessage = PromptUtl.renderAiBlock(StringUtils.trimToEmpty(llmResponse.aiMessage().text()));
        var insertionOffset = userBlock.getEndOffset() + 1;

        var hasFooter = false;
        var contentLen = content.length();
        for (var i = userBlock.getEndOffset(); i < contentLen; i++) {
            if (!Character.isWhitespace(content.charAt(i))) {
                hasFooter = true;
                break;
            }
        }
        var additionalUserBlock = hasFooter ? StringUtils.EMPTY : PromptUtl.renderUserBlock(null);

        @SuppressWarnings("all")
        var newContent = new StringBuilder(contentLen + assistantMessage.length() + additionalUserBlock.length());
        newContent.append(content, 0, insertionOffset);
        newContent.append(assistantMessage);
        newContent.append(content, insertionOffset, contentLen);
        newContent.append(additionalUserBlock);

        var tagsResult = new TagsResult();
        tagsResult.setGateway(ActionResultGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(insertionOffset);
        tagsResult.setEndOffset(insertionOffset);
        return Optional.of(tagsResult);
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

    private Optional<String> extractMessage(List<FencedCodeBlock> blocks, int offset) {
        if (CollectionUtils.isEmpty(blocks)) {
            return Optional.empty();
        }
        var content = StringUtils.EMPTY;
        if (blocks.size() == 1) {
            content = getContent(blocks.get(0));
        } else {
            // find the block nearest to the offset (regardless if the offset is inside or outside the block)
            // if the distance is equal prefer the block at the highest offset
            FencedCodeBlock nearestBlock = null;
            int minDistance = Integer.MAX_VALUE;

            for (FencedCodeBlock block : blocks) {
                int start = block.getStartOffset();
                int end = block.getEndOffset();
                int distance;

                if (offset >= start && offset <= end) {
                    distance = 0;
                } else if (offset < start) {
                    distance = start - offset;
                } else {
                    distance = offset - end;
                }

                if (distance < minDistance) {
                    minDistance = distance;
                    nearestBlock = block;
                } else if (distance == minDistance) {
                    if (nearestBlock != null && block.getStartOffset() > nearestBlock.getStartOffset()) {
                        nearestBlock = block;
                    } else if (nearestBlock == null) {
                        nearestBlock = block;
                    }
                }
            }
            if (nearestBlock != null) {
                content = getContent(nearestBlock);
            }
        }
        return StringUtils.isBlank(content) ? Optional.empty() : Optional.of(content);
    }


    @SneakyThrows
    private ChatRequestParameters collectParameters(String data) {
        var properties = PromptUtl.parseProperties(data);
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
