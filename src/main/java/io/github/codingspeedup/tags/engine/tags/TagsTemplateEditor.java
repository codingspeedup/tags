package io.github.codingspeedup.tags.engine.tags;

import io.github.codingspeedup.tags.engine.core.FileTypeModel;
import io.github.codingspeedup.tags.engine.core.TagsResult;
import io.github.codingspeedup.tags.engine.core.ActionResultGateway;
import io.github.codingspeedup.tags.engine.core.TagsBlock;
import lombok.AllArgsConstructor;

import java.util.Locale;

@AllArgsConstructor
public class TagsTemplateEditor {

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
        newContent.append(ftModel.getTPrefix()).append("Explain the concept of \"{{concept}}\"").append("\n");
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

}
