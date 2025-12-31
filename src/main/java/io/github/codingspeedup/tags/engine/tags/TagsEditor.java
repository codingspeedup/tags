package io.github.codingspeedup.tags.engine.tags;

import io.github.codingspeedup.tags.engine.core.FileTypeModel;
import io.github.codingspeedup.tags.engine.core.TagsResult;
import io.github.codingspeedup.tags.engine.core.ActionResultGateway;
import io.github.codingspeedup.tags.engine.core.TagsBlock;
import lombok.AllArgsConstructor;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class TagsEditor {

    private final FileTypeModel ftModel;
    private final String fileContent;

    public TagsResult insertNewTemplate(int offset) {
        int tOffset = ftModel.getSections(fileContent).values().stream()
                .filter(section -> section.contains(offset))
                .findFirst()
                .map(TagsBlock::getFromOffset)
                .orElse(FileTypeModel.indexOfBol(fileContent, offset));

        @SuppressWarnings("all")
        var newContent = new StringBuilder(fileContent.length() + 1024);
        newContent.append(fileContent, 0, tOffset);
        newContent.append(ftModel.getTPrefix()).append("Explain {{concept}}").append("\n");
        newContent.append(ftModel.getAPrefix()).append("concept=abstraction").append("\n");
        newContent.append(ftModel.getGPrefix()).append(ActionResultGateway.BUFFER.name().toLowerCase(Locale.ROOT)).append("\n");
        newContent.append(fileContent, tOffset, fileContent.length());

        var tagsResult = new TagsResult();
        tagsResult.setGateway(ActionResultGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(tOffset);
        tagsResult.setEndOffset(tOffset);
        return tagsResult;
    }

    public TagsResult insertNewSection(int fromOffset, int toOffset) {
        var sPrefix = ftModel.getSPrefix();
        var existingSections = ftModel.getSections(fileContent).keySet();

        var sectionIndex = 2;
        var newSectionName = "section-1";
        while (existingSections.contains(newSectionName)) {
            newSectionName = "section-" + sectionIndex++;
        }

        fromOffset = FileTypeModel.indexOfBol(fileContent, fromOffset);
        toOffset = FileTypeModel.indexOfEol(fileContent, toOffset);

        var newContent = new StringBuilder(fileContent.length() + 64);
        newContent.append(fileContent, 0, fromOffset);
        newContent.append(sPrefix).append("<").append(newSectionName).append(">\n");
        var startOffset = newContent.length();
        newContent.append(fileContent, fromOffset, toOffset);
        var endOffset = newContent.length();
        newContent.append("\n").append(sPrefix).append("</").append(newSectionName).append(">\n");
        newContent.append(fileContent,  toOffset, fileContent.length());

        var tagsResult = new TagsResult();
        tagsResult.setGateway(ActionResultGateway.CONTENT);
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

        var tagsResult = new TagsResult();
        tagsResult.setGateway(ActionResultGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(offset);
        tagsResult.setEndOffset(offset);
        return Optional.of(tagsResult);
    }

}
