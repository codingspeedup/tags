package io.github.codingspeedup.tags.engine;

import com.intellij.openapi.project.Project;
import io.github.codingspeedup.tags.prompting.plib.PromptRef;
import io.github.codingspeedup.tags.prompting.tags.PromptBlock;
import io.github.codingspeedup.tags.prompting.tags.TemplateModel;
import lombok.AllArgsConstructor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class TagsEditHandler {

    private final TemplateModel ftModel;
    private final String fileContent;

    public TagsResult insertNewTemplate(Project project, int offset, PromptRef promptRef) {
        int tOffset = ftModel.getSections(fileContent).values().stream()
                .filter(section -> section.contains(offset))
                .findFirst()
                .map(PromptBlock::getFromOffset)
                .orElse(TemplateModel.indexOfBol(fileContent, offset));

        var pLib = promptRef.getLibrary(project);

        var newContent = new StringBuilder(fileContent.length() + 1024);
        newContent.append(fileContent, 0, tOffset);
        newContent.append(ftModel.getTPrefix()).append(promptRef.templateRef()).append("\n");
        for (var varName : pLib.getVariables(promptRef.getId())) {
            var varValue = TemplateModel.VAR_PLACEHOLDER;
            if (pLib.getDefaults().containsKey(varName)) {
                varValue = String.valueOf(pLib.getDefaults().get(varName));
            }
            newContent.append(ftModel.getAPrefix()).append(varName).append(TemplateModel.VAR_SEPARATOR).append(varValue).append("\n");
        }
        // newContent.append(ftModel.getGPrefix()).append(ActionResultGateway.CHAT.name().toLowerCase(Locale.ROOT)).append("\n");
        newContent.append(ftModel.getPlusPrefix()).append(TemplateModel.VAR_PLACEHOLDER).append("\n");
        newContent.append(fileContent, tOffset, fileContent.length());

        var tagsResult = new TagsResult(ActionResultGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(tOffset);
        tagsResult.setEndOffset(tOffset);
        return tagsResult;
    }

    public TagsResult insertNewSection(int fromOffset, int toOffset) {
        var sPrefix = ftModel.getSPrefix();
        var existingSections = ftModel.getSections(fileContent).keySet();

        var sectionIndex = 1;
        var newSectionName = TemplateModel.SECTION_ROOT_ID + sectionIndex;
        while (existingSections.contains(newSectionName)) {
            newSectionName = TemplateModel.SECTION_ROOT_ID + (++sectionIndex);
        }

        fromOffset = TemplateModel.indexOfBol(fileContent, fromOffset);
        toOffset = TemplateModel.indexOfEol(fileContent, toOffset);

        var newContent = new StringBuilder(fileContent.length() + 64);
        newContent.append(fileContent, 0, fromOffset);
        newContent.append(sPrefix).append(TemplateModel.SECTION_NAME_START).append(newSectionName).append(TemplateModel.SECTION_NAME_END).append("\n");
        var startOffset = newContent.length();
        newContent.append(fileContent, fromOffset, toOffset);
        var endOffset = newContent.length();
        newContent.append("\n").append(sPrefix).append(TemplateModel.SECTION_NAME_START).append(TemplateModel.SECTION_CLOSE).append(newSectionName).append(TemplateModel.SECTION_NAME_END).append("\n");
        newContent.append(fileContent, toOffset, fileContent.length());

        var tagsResult = new TagsResult(ActionResultGateway.CONTENT);
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

        var tagsResult = new TagsResult(ActionResultGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(offset);
        tagsResult.setEndOffset(offset);
        return Optional.of(tagsResult);
    }

}
