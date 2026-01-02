package io.github.codingspeedup.tags.engine;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.output.FinishReason;
import io.github.codingspeedup.tags.integration.LLM;
import io.github.codingspeedup.tags.utils.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.codingspeedup.tags.utils.PromptUtl.toChatRequestParameters;

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
                    case ChatUtl.PARAMETERS_BLOCK_INFO -> parametersBlocks.add(codeBlock);
                    case ChatUtl.SYSTEM_BLOCK_INFO -> systemBlocks.add(codeBlock);
                    case ChatUtl.USER_BLOCK_INFO -> userBlocks.add(codeBlock);
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
        var userMessage = UserMessage.from(ChatUtl.CHAT_MD_EXTENSION, getContent(userBlock));

        var system = extractMessage(systemBlocks, contentOffset)
                .map(SystemMessage::from);

        var llmParameters = parametersBlocks.stream()
                .map(this::getContent)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(data -> toChatRequestParameters(PromptUtl.parseProperties(data)))
                .orElse(ChatRequestParameters.builder().build());

        var llmResponse = system.map(systemMessage -> LLM.doChat(llmParameters, systemMessage, userMessage))
                .orElse(LLM.doChat(llmParameters, userMessage));

        if (llmResponse.metadata().finishReason() == FinishReason.OTHER) {
            return Optional.empty();
        }

        var assistantMessage = ChatUtl.renderAiBlock(StringUtils.trimToEmpty(llmResponse.aiMessage().text()));
        var insertionOffset = userBlock.getEndOffset() + 1;

        var hasFooter = false;
        var contentLen = content.length();
        for (var i = userBlock.getEndOffset(); i < contentLen; i++) {
            if (!Character.isWhitespace(content.charAt(i))) {
                hasFooter = true;
                break;
            }
        }
        var additionalUserBlock = hasFooter ? StringUtils.EMPTY : ChatUtl.renderUserBlock(StringUtils.EMPTY);

        @SuppressWarnings("all")
        var newContent = new StringBuilder(contentLen + assistantMessage.length() + additionalUserBlock.length());
        newContent.append(content, 0, insertionOffset);
        newContent.append(assistantMessage);
        newContent.append(content, insertionOffset, contentLen);
        newContent.append(additionalUserBlock);

        var tagsResult = new TagsResult(ActionResultGateway.CONTENT);
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

}
