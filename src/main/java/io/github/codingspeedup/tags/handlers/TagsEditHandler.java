package io.github.codingspeedup.tags.handlers;

import com.intellij.openapi.project.Project;
import io.github.codingspeedup.tags.ai.deployment.orchestration.ResponseGateway;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptRef;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferRange;
import lombok.AllArgsConstructor;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.codingspeedup.tags.utils.Minions.endsWith;

@AllArgsConstructor
public class TagsEditHandler {

    private final BufferModel ftModel;
    private final String fileContent;

    public TagsResult insertNewTemplate(Project project, int offset, PromptRef promptRef) {
        int tOffset = ftModel.getSections(fileContent).values().stream()
                .filter(section -> section.contains(offset))
                .findFirst()
                .map(BufferRange::getFromOffset)
                .orElse(BufferModel.indexOfBol(fileContent, offset));

        var pLib = promptRef.getLibrary(project);

        var newContent = new StringBuilder(fileContent.length() + 1024);
        newContent.append(fileContent, 0, tOffset);
        newContent.append(ftModel.getTPrefix()).append(promptRef.templateRef()).append("\n");
        for (var varName : pLib.getVariables(promptRef.getId())) {
            var varValue = BufferModel.ARG_PLACEHOLDER;
            if (pLib.getDefaults().containsKey(varName)) {
                varValue = String.valueOf(pLib.getDefaults().get(varName));
            }
            newContent.append(ftModel.getAPrefix()).append(varName).append(BufferModel.ARG_SEPARATOR).append(varValue).append("\n");
        }
        newContent.append(ftModel.getGPrefix()).append(ResponseGateway.CHAT.name().toLowerCase(Locale.ROOT)).append("\n");
        newContent.append(ftModel.getPlusPrefix()).append(BufferModel.ARG_PLACEHOLDER).append("\n");
        newContent.append(fileContent, tOffset, fileContent.length());

        var tagsResult = new TagsResult(ResponseGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(tOffset);
        tagsResult.setEndOffset(tOffset);
        return tagsResult;
    }

    public TagsResult insertNewSection(int fromOffset, int toOffset) {
        var sPrefix = ftModel.getSPrefix();
        var existingSections = ftModel.getSections(fileContent).keySet();

        var sectionIndex = 1;
        var newSectionName = BufferModel.SECTION_ROOT_ID + sectionIndex;
        while (existingSections.contains(newSectionName)) {
            newSectionName = BufferModel.SECTION_ROOT_ID + (++sectionIndex);
        }

        fromOffset = BufferModel.indexOfBol(fileContent, fromOffset);
        toOffset = BufferModel.indexOfEol(fileContent, toOffset);

        var newContent = new StringBuilder(fileContent.length() + 64);
        newContent.append(fileContent, 0, fromOffset);
        newContent.append(sPrefix).append(BufferModel.SECTION_NAME_START).append(newSectionName).append(BufferModel.SECTION_NAME_END).append("\n");
        var startOffset = newContent.length();
        newContent.append(fileContent, fromOffset, toOffset);
        var endOffset = newContent.length();
        if (!endsWith(newContent, "\n")) {
            newContent.append("\n");
        }
        newContent.append(sPrefix)
                .append(BufferModel.SECTION_NAME_START)
                .append(BufferModel.SECTION_CLOSE)
                .append(newSectionName)
                .append(BufferModel.SECTION_NAME_END)
                .append("\n");
        newContent.append(fileContent, toOffset, fileContent.length());

        var tagsResult = new TagsResult(ResponseGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(startOffset);
        tagsResult.setEndOffset(endOffset);
        return tagsResult;
    }

    public Optional<TagsResult> stripTags(int offset) {
        var tPrefix = ftModel.getTPrefix();
        var aPrefix = ftModel.getAPrefix();
        var gPrefix = ftModel.getGPrefix();
        var sPrefix = ftModel.getSPrefix();
        var pPrefix = ftModel.getPlusPrefix();

        var contentChanged = new AtomicBoolean();
        var newContent = new StringBuilder();
        fileContent.lines().forEach(line -> {
            var foo = line.stripLeading();
            if (foo.startsWith(tPrefix) || foo.startsWith(aPrefix) || foo.startsWith(gPrefix) || foo.startsWith(sPrefix) || foo.startsWith(pPrefix)) {
                contentChanged.set(true);
            } else {
                newContent.append(line).append("\n");
            }
        });

        if (!contentChanged.get()) {
            return Optional.empty();
        }

        var tagsResult = new TagsResult(ResponseGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(offset);
        tagsResult.setEndOffset(offset);
        return Optional.of(tagsResult);
    }

}
