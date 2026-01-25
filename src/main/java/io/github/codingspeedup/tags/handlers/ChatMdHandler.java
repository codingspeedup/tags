package io.github.codingspeedup.tags.handlers;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import dev.langchain4j.model.output.FinishReason;
import io.github.codingspeedup.tags.ai.composition_orchestration.buffers.ChatMdModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.core.TagPlusModel;
import io.github.codingspeedup.tags.ai.composition_reactive.GroovyScriptExecutor;
import io.github.codingspeedup.tags.ai.deployment_orchestration.ResponseGateway;
import io.github.codingspeedup.tags.ai.primitives_reactive.TagsPrompt;
import io.github.codingspeedup.tags.minions.*;
import io.github.codingspeedup.tags.plugin.settings.SettingsState;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChatMdHandler extends ChatMdModel implements ActionHandler {

    private final VirtualFile contentFile;
    private final String contentName;
    private final String content;
    private final int contentOffset;

    private final List<FencedCodeBlock> parametersBlocks = new ArrayList<>();
    private final List<FencedCodeBlock> systemBlocks = new ArrayList<>();
    private final List<FencedCodeBlock> userBlocks;
    private final List<FencedCodeBlock> groovyBlocks;
    private final List<TagPlusModel> tagPlusModels;

    public ChatMdHandler(VirtualFile contentFile, String content, int contentOffset) {
        this.contentFile = contentFile;
        this.contentName = contentFile.getName();
        this.content = content;
        this.contentOffset = contentOffset;

        var mdParserOptions = new MutableDataSet();
        var mdParser = Parser.builder(mdParserOptions).build();
        var mdDocument = mdParser.parse(content);

        List<FencedCodeBlock> userBlocks = new ArrayList<>();
        List<FencedCodeBlock> groovyBlocks = new ArrayList<>();
        for (var node : mdDocument.getChildren()) {
            if (node instanceof FencedCodeBlock codeBlock) {
                var info = StringUtils.trimToEmpty(codeBlock.getInfo().toString());
                switch (info) {
                    case PARAMETERS_BLOCK_INFO -> parametersBlocks.add(codeBlock);
                    case SYSTEM_BLOCK_INFO -> systemBlocks.add(codeBlock);
                    case USER_BLOCK_INFO -> userBlocks.add(codeBlock);
                    default -> {
                        if (info.startsWith("groovy")) {
                            groovyBlocks.add(codeBlock);
                        }
                    }
                }
            }
        }
        this.userBlocks = userBlocks.stream()
                .filter(block -> StringUtils.isNotBlank(getContent(block)))
                .toList();
        this.groovyBlocks = groovyBlocks.stream()
                .filter(block -> StringUtils.isNotBlank(getContent(block)))
                .toList();
        this.tagPlusModels = this.userBlocks.stream().map(ChatMdModel::toTagPlusRange).toList();
    }

    @Override
    public List<TagPlusModel> locateTagPlusRanges(String content) {
        return tagPlusModels;
    }

    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var result = executeGroovy(project);
        if (result.isEmpty()) {
            result = executeUserPrompt(project);
        }
        return result;
    }

    @SuppressWarnings("ExtractMethodRecommender")
    private @NonNull Optional<TagsResult> executeGroovy(Project project) {
        var groovyBlock = groovyBlocks.stream()
                .filter(block -> block.getStartOffset() <= contentOffset && contentOffset <= block.getEndOffset())
                .findFirst();
        if (groovyBlock.isEmpty()) {
            return Optional.empty();
        }

        var groovyScript = String.join(StringUtils.EMPTY, groovyBlock.get().getContentLines());

        var outContent = new ByteArrayOutputStream();
        var errContent = new ByteArrayOutputStream();

        var toolboxManager = ToolboxManagerService.getInstance(project);
        toolboxManager.reloadIfChanged();

        var executor = new GroovyScriptExecutor(new PrintStream(outContent), new PrintStream(errContent), toolboxManager.getActiveLoader());
        var scriptResult = executor.execute(groovyScript, contentName);

        var shellMessage = new StringBuilder("```\n");
        shellMessage.append(Minions.sanitizeLineEndings(outContent.toString()));
        if (!Minions.endsWith(shellMessage, "\n")) {
            shellMessage.append("\n");
        }
        shellMessage.append("```\n");
        var capturedErr = StringUtils.trimToEmpty(errContent.toString());
        if (StringUtils.isNotBlank(capturedErr)) {
            shellMessage.append("<pre style=\"color: Salmon\">\n");
            shellMessage.append(Minions.sanitizeLineEndings(capturedErr));
            if (!Minions.endsWith(shellMessage, "\n")) {
                shellMessage.append("\n");
            }
            shellMessage.append("</pre>\n");
        }
        if (scriptResult != null) {
            if (scriptResult instanceof Throwable t) {
                shellMessage.append("<pre style=\"color: Salmon; font-weight: bold\">\n");
                shellMessage.append(scriptResult.getClass().getName()).append(":\n");
                shellMessage.append(Minions.sanitizeLineEndings(t.getMessage()));
            } else {
                shellMessage.append("<pre style=\"color: MediumSeaGreen; font-weight: bold\">\n");
                shellMessage.append(Minions.sanitizeLineEndings(String.valueOf(scriptResult)));
            }
            if (!Minions.endsWith(shellMessage, "\n")) {
                shellMessage.append("\n");
            }
            shellMessage.append("</pre>\n");
        }

        var responseMessage = renderResponseBlock("Groovy", shellMessage.toString());

        var insertionOffset = groovyBlock.get().getEndOffset() + 1;
        var contentLen = content.length();

        @SuppressWarnings("all")
        var newContent = new StringBuilder(contentLen + responseMessage.length() + 8);
        newContent.append(content, 0, insertionOffset);
        newContent.append(responseMessage);
        newContent.append(content, insertionOffset, contentLen);

        var tagsResult = new TagsResult(ResponseGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(insertionOffset);
        tagsResult.setEndOffset(insertionOffset);
        return Optional.of(tagsResult);
    }

    private @NonNull Optional<TagsResult> executeUserPrompt(Project project) {
        var blockIndex = findUserBlock(userBlocks, contentOffset);
        if (blockIndex < 0) {
            return Optional.empty();
        }

        var promptBuilder = TagsPrompt.builder(
                SettingsState.getInstance(),
                new ProjectBufferProvider(project, contentFile, content),
                new ProjectPromptLibraryProvider(project),
                new ProjectToolboxSupport(project));

        var tagPlusModel = tagPlusModels.get(blockIndex);
        fillTagPlusModel(tagPlusModel, content);
        promptBuilder.tagPlus(tagPlusModel);

        parametersBlocks.stream()
                .map(this::getContent)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .ifPresent(promptBuilder::llmParameters);

        extractMessage(systemBlocks, contentOffset).ifPresent(promptBuilder::systemMessage);

        var llmResponse = promptBuilder.build().execute();

        if (llmResponse.metadata().finishReason() == FinishReason.OTHER) {
            return Optional.empty();
        }

        var userBlock = userBlocks.get(blockIndex);
        var insertionOffset = userBlock.getEndOffset() + 1;
        var responseMessage = renderAiBlock(StringUtils.trimToEmpty(llmResponse.aiMessage().text()));

        var hasFooter = false;
        var contentLen = content.length();
        for (var i = userBlock.getEndOffset(); i < contentLen; i++) {
            if (!Character.isWhitespace(content.charAt(i))) {
                hasFooter = true;
                break;
            }
        }
        var additionalUserBlock = hasFooter ? StringUtils.EMPTY : renderUserBlock(StringUtils.EMPTY);

        @SuppressWarnings("all")
        var newContent = new StringBuilder(contentLen + responseMessage.length() + additionalUserBlock.length());
        newContent.append(content, 0, insertionOffset);
        newContent.append(responseMessage);
        newContent.append(content, insertionOffset, contentLen);
        newContent.append(additionalUserBlock);

        var tagsResult = new TagsResult(ResponseGateway.CONTENT);
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
