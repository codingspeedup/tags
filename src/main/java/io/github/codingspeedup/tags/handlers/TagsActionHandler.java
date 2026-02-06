package io.github.codingspeedup.tags.handlers;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.codingspeedup.tags.ai.composition_orchestration.buffers.ChatMdModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.core.BufferModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.core.SectionModel;
import io.github.codingspeedup.tags.ai.primitives_reactive.TagsPrompt;
import io.github.codingspeedup.tags.minions.ProjectBufferProvider;
import io.github.codingspeedup.tags.minions.ProjectPromptLibraryProvider;
import io.github.codingspeedup.tags.minions.ProjectToolboxSupport;
import io.github.codingspeedup.tags.plugin.settings.SettingsModel;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Optional;

import static io.github.codingspeedup.tags.ai.composition_orchestration.core.BufferModel.parseSectionName;
import static io.github.codingspeedup.tags.ai.deployment_orchestration.ResponseGateway.resolveGateway;
import static io.github.codingspeedup.tags.ai.primitives_reactive.PromptUtl.toProperties;

public class TagsActionHandler implements ActionHandler {

    private record Buffer(String content, int offset) {
    }

    private final VirtualFile virtualFile;
    private final String fileName;
    private final String fileContent;
    private final int fileOffset;
    private final BufferModel ftModel;
    private Map<String, SectionModel> contentSections;

    public TagsActionHandler(VirtualFile virtualFile, String fileContent, int fileOffset) {
        this.virtualFile = virtualFile;
        this.fileName = virtualFile.getName();
        this.fileContent = fileContent;
        this.fileOffset = fileOffset;
        this.ftModel = BufferModel.of(fileName).orElseThrow();
    }

    private Map<String, SectionModel> getContentSections() {
        if (contentSections == null) {
            contentSections = ftModel.getSections(fileContent);
        }
        return contentSections;
    }

    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var blocks = ftModel.locateTagPlusRanges(fileContent);
        if (blocks.isEmpty()) {
            return Optional.empty();
        }

        var templateBlock = blocks.stream()
                .filter(t -> t.contains(fileOffset))
                .findFirst()
                .orElse(blocks.get(0));
        ftModel.fillTagPlusModel(templateBlock, fileContent);

        var promptBuilder = TagsPrompt.builder(
                SettingsModel.getInstance(),
                new ProjectBufferProvider(project, virtualFile, fileContent),
                new ProjectPromptLibraryProvider(project),
                new ProjectToolboxSupport(project));

        promptBuilder.tagPlus(templateBlock);

        var prompt = promptBuilder.build();
        var chatResponse = prompt.execute();

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
                var bc = buildChatBuffer(prompt.chatRequest(), chatResponse);
                tagsResult.setBufferName(ChatMdModel.nextChatMdBufferName(project, fileName));
                tagsResult.setContent(bc.content());
                tagsResult.setStartOffset(bc.offset());
                tagsResult.setEndOffset(bc.offset());
            }

        }
        return Optional.of(tagsResult);
    }

    private Buffer buildChatBuffer(ChatRequest chatRequest, ChatResponse chatResponse) {
        var mdContent = new StringBuilder();
        mdContent.append(ChatMdModel.renderParametersBlock(toProperties(chatRequest.parameters(), false)));
        chatRequest.messages().forEach(message -> {
            switch (message.type()) {
                case SYSTEM -> mdContent.append(ChatMdModel.renderSystemBlock((SystemMessage) message));
                case USER -> mdContent.append(ChatMdModel.renderUserBlock((UserMessage) message));
            }
        });
        var bufferOffset = mdContent.length() + 1;
        mdContent.append(ChatMdModel.renderAiBlock(chatResponse.aiMessage().text()));
        mdContent.append(ChatMdModel.renderUserBlock(StringUtils.EMPTY));
        return new Buffer(mdContent.toString(), bufferOffset);
    }

    private Buffer buildNewContent(String sectionName, ChatResponse chatResponse) {
        var sectionBlock = getContentSections().get(sectionName);
        var sectionStart = BufferModel.indexOfEol(fileContent, sectionBlock.getFromOffset());
        var sectionEnd = BufferModel.indexOfBol(fileContent, sectionBlock.getToOffset());

        @SuppressWarnings("all")
        var bufferContent = new StringBuilder();
        bufferContent.append(fileContent, 0, sectionStart);
        bufferContent.append("\n").append(chatResponse.aiMessage().text()).append("\n");
        bufferContent.append(fileContent, sectionEnd, fileContent.length());

        return new Buffer(bufferContent.toString(), sectionStart + 1);
    }

}
